package com.xmlcalabash.steps

import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XMLCalabashRuntime, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, XPointer}
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}

import scala.collection.mutable

class XInclude() extends DefaultXmlStep with ProcessMatchingNodes {
  private val localAttrNS = "http://www.w3.org/2001/XInclude/local-attributes"
  private val xiNS = "http://www.w3.org/2001/XInclude"

  private val xi_include = new QName("xi", xiNS, "include")
  private val xi_fallback = new QName("xi", xiNS, "fallback")
  private val _fixup_xml_base = new QName("fixup-xml-base")
  private val _fixup_xml_lang = new QName("fixup-xml-lang")
  private val _set_xml_id = new QName("set-xml-id")
  private val _accept = new QName("accept")
  private val _accept_language = new QName("accept-language")
  private val cx_trim = new QName("cx", XProcConstants.ns_cx, "trim")
  private val cx_read_limit = new QName("cx", XProcConstants.ns_cx, "read-limit")
  private val _encoding = new QName("encoding")
  private val _href = new QName("href")
  private val _parse = new QName("parse")
  private val _fragid = new QName("fragid")
  private val _xpointer = new QName("xpointer")

  private var source: XdmNode = _
  private var smeta: XProcMetadata = _

  private var matcherStack = List.empty[ProcessMatch]
  private var inside = List.empty[String]
  private var setXmlId = List.empty[Option[String]]
  private var fixupBase = false
  private var fixupLang = false
  private var copyAttributes = false
  private var defaultTrimText = false
  private var trimText = false
  private var readLimit = 1024 * 1000 * 100
  private var mostRecentException = Option.empty[Exception]
  private var staticContext: StaticContext = _


  override def inputSpec: XmlPortSpecification = XmlPortSpecification.MARKUPSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    smeta = metadata
  }

  override def run(context: StaticContext): Unit = {
    staticContext = context
    super.run(context)

    fixupBase = booleanBinding(_fixup_xml_base).getOrElse(false)
    fixupLang = booleanBinding(_fixup_xml_lang).getOrElse(false)
    copyAttributes = true // XInclude 1.1

    defaultTrimText = booleanBinding(cx_trim).getOrElse(false)
    trimText = defaultTrimText

    val str = optionalStringBinding(cx_read_limit)
    if (str.isDefined) {
      readLimit = str.get.toInt
    }

    val result = expandXIncludes(source)
    consumer.get.receive("result", result, smeta)
  }

  private def expandXIncludes(node: XdmNode): XdmNode = {
    val matcher = new ProcessMatch(config, this, staticContext)
    matcherStack = matcher +: matcherStack
    matcher.process(node, "/|*")
    matcherStack = matcherStack.tail
    matcher.result
  }

  override def startDocument(node: XdmNode): Boolean = {
    matcherStack.head.startDocument(node.getBaseURI)
    true
  }

  override def endDocument(node: XdmNode): Unit = {
    matcherStack.head.endDocument()
  }

  override def startElement(node: XdmNode): Boolean = {
    val matcher = matcherStack.head
    if (node.getNodeName == xi_include) {
      val href = Option(node.getAttributeValue(_href)).getOrElse("")
      var parse = Option(node.getAttributeValue(_parse)).getOrElse("xml")
      var xptr = Option(node.getAttributeValue(_xpointer))
      var fragid = Option(node.getAttributeValue(_fragid))
      val setId = Option(node.getAttributeValue(_set_xml_id))
      val accept = Option(node.getAttributeValue(_accept))
      val accept_lang = Option(node.getAttributeValue(_accept_language))

      if (accept.isDefined && accept.get.matches(".*[^\u0020-\u007e].*")) {
        throw XProcException.xcXIncludeInvalidAccept(accept.get, location)
      }

      if (accept_lang.isDefined && accept_lang.get.matches(".*[^\u0020-\u007e].*")) {
        throw XProcException.xcXIncludeInvalidAcceptLang(accept.get, location)
      }

      // FIXME: Take accept and accept_language into consideration when retrieving resources

      var xifallback = Option.empty[XdmNode]
      for (child <- S9Api.axis(node, Axis.CHILD)) {
        child.getNodeKind match {
          case XdmNodeKind.ELEMENT =>
            if (xi_fallback == child.getNodeName) {
              if (xifallback.isDefined) {
                throw XProcException.xcXIncludeMultipleFallback(location)
              } else {
                xifallback = Some(child)
              }
            } else {
              if (xiNS == child.getNodeName.getNamespaceURI) {
                throw XProcException.xcXIncludeInvalidElement(child.getNodeName, location)
              }
            }
          case _ => ()
        }
      }

      var forceFallback = false
      var xpointer = Option.empty[XPointer]
      var subdoc = Option.empty[XdmNode]

      if (parse.contains(";")) {
        parse = parse.substring(0, parse.indexOf(";")).trim
      }

      if (parse == "xml" || parse == "application/xml" || parse == "text/xml" || parse.endsWith("+xml")) {
        parse = "xml"
      } else if (parse == "text" || parse.startsWith("text/")) {
        parse = "text"
      } else {
        logger.info(s"Unrecognized parse on XInclude: $parse")
        xptr = None
        fragid = None
        forceFallback = true
      }

      if (xptr.isDefined && fragid.isDefined) {
        if (xptr.get != fragid.get) {
          if (parse == "xml") {
            logger.info(s"XInclude specifies different xpointer/fragid, using xpointer for XML: $xptr")
          } else {
            xptr = fragid
            logger.info(s"XInclude specifies different xpointer/fragid, using fragid for $parse: $xptr")
          }
        }
      }

      if (xptr.isEmpty && fragid.isDefined) {
        xptr = fragid
      }

      var trimText = defaultTrimText
      val trim = Option(node.getAttributeValue(cx_trim))
      if (trim.isDefined) {
        if (trim.get == "true" || trim.get == "false") {
          trimText = trim.get == "true"
        } else {
          throw XProcException.xcInvalidTrim(trim.get)
        }
      }

      if (xptr.isDefined) {
        // HACK!
        if (parse == "text") {
          val xtrim = xptr.get.trim
          // What about spaces around the "="
          if (xtrim.startsWith("line=") || xtrim.startsWith("char=")) {
            xptr = Some(s"text($xptr)")
          } else if (xtrim.startsWith("search=")) {
            xptr = Some(s"search($xptr)")
          }
        }
        xpointer = Some(new XPointer(config, xptr.get, readLimit))
      }

      if (forceFallback) {
        fallback(node, href)
        false
      } else if (parse == "text") {
        readText(href, node, node.getBaseURI.toASCIIString, xpointer.get, matcher)
        false
      } else {
        setXmlId = setId +: setXmlId

        var iuri = ""
        val subdoc = readXML(node, href, node.getBaseURI.toASCIIString)

        if (subdoc.isEmpty) {
          logger.debug(s"XInclude parse failed: $href")
          fallback(node, href)
          setXmlId = setXmlId.tail
          return false
        } else {
          iuri = subdoc.get.getBaseURI.toASCIIString
          if (xptr.isDefined) {
            iuri += s"#$xptr"
          }
          if (inside.contains(iuri)) {
            throw XProcException.xcXIncludeLoop(href, location)
          }
          logger.debug(s"Include parsed: $href")
        }

        val subtree = new SaxonTreeBuilder(config)
        subtree.startDocument(subdoc.get.getBaseURI)
        for (snode <- S9Api.axis(subdoc.get, Axis.CHILD)) {
          var child = snode
          if ((fixupBase || fixupLang || copyAttributes) && child.getNodeKind == XdmNodeKind.ELEMENT) {
            val fixup = new Fixup(config, child)
            child = fixup.fixup()
          }

          if (child.getNodeKind == XdmNodeKind.ELEMENT || child.getNodeKind == XdmNodeKind.DOCUMENT) {
            inside = iuri +: inside
            val ex = expandXIncludes(child)
            subtree.addSubtree(ex)
            inside = inside.tail
          } else {
            subtree.addSubtree(child)
          }
        }
        subtree.endDocument()

        if (xpointer.isEmpty) {
          matcher.addSubtree(subtree.result)
        } else {
          val nodes = xpointer.get.selectNodes(config, subtree.result)
          if (nodes.isEmpty) {
            logger.debug(s"XInclude failed parse $href")
            fallback(node, href)
          } else {
            for (child <- nodes) {
              if ((fixupBase || fixupLang || copyAttributes) && child.getNodeKind == XdmNodeKind.ELEMENT) {
                val fixup = new Fixup(config, child)
                matcher.addSubtree(fixup.fixup())
              } else {
                matcher.addSubtree(child)
              }
            }
          }
        }

        setXmlId = setXmlId.tail
        return false
      }
    } else if (node.getNodeName == xi_fallback) {
      throw XProcException.xcXIncludeFallbackPlacement(location)
    } else {
      matcher.addStartElement(node)
      matcher.addAttributes(node)
      matcher.startContent()
      true
    }
  }

  override def endElement(node: XdmNode): Unit = {
    if (node.getNodeName == xi_include) {
      // Do nothing, we've already output the subtree
    } else {
      matcherStack.head.addEndElement()
    }
  }

  override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = false

  override def attribute(node: XdmNode): Unit = {
    throw new UnsupportedOperationException("attribute() called in XInclude?")
  }

  override def text(node: XdmNode): Unit = {
    throw new UnsupportedOperationException("text() called in XInclude?")
  }

  override def comment(node: XdmNode): Unit = {
    throw new UnsupportedOperationException("comment() called in XInclude?")
  }

  override def pi(node: XdmNode): Unit = {
    throw new UnsupportedOperationException("pi() called in XInclude?")
  }

  private def readText(href: String, node: XdmNode, base: String, xpointer: XPointer, matcher: SaxonTreeBuilder): Unit = {
    val uri = staticContext.baseURI.get.resolve(href)
    val request = new DocumentRequest(uri, Some(MediaType.TEXT), location, false)
    val response = config.documentManager.parse(request)
    if (response.contentType.matches(MediaType.TEXT)) {
      matcher.addText(response.value.getUnderlyingValue.getStringValue)
    } else {
      fallback(node, href)
    }
  }

  def readXML(node: XdmNode, href: String, base: String): Option[XdmNode] = {
    if (href == "") {
      var ptr = node
      while (ptr.getParent != null) {
        ptr = ptr.getParent
      }
      Some(ptr)
    } else {
      val uri = staticContext.baseURI.get.resolve(href)
      val request = new DocumentRequest(uri, Some(MediaType.XML), location, false)
      val response = config.documentManager.parse(request)
      if (response.contentType.matches(MediaType.XML)) {
        Some(response.value.asInstanceOf[XdmNode])
      } else {
        None
      }
    }
  }

  def fallback(node: XdmNode, href: String): Unit = {
    // We already know there's at most one
    var fallback = Option.empty[XdmNode]
    for (child <- S9Api.axis(node, Axis.CHILD)) {
      if (child.getNodeKind == XdmNodeKind.ELEMENT && child.getNodeName == xi_fallback) {
        fallback = Some(child)
      }
    }

    if (fallback.isEmpty) {
      throw XProcException.xcXIncludeResourceError(href, location)
    }

    for (child <- S9Api.axis(fallback.get, Axis.CHILD)) {
      var fbc = child
      if (fbc.getNodeKind == XdmNodeKind.ELEMENT) {
        fbc = expandXIncludes(fbc)
      }
      matcherStack.head.addSubtree(fbc)
    }
  }

    private class Fixup(config: XMLCalabashRuntime, xinclude: XdmNode) extends ProcessMatchingNodes {
      private var root = true
      private var matcher: ProcessMatch = _

      def fixup(): XdmNode = {
        matcher = new ProcessMatch(config, this, staticContext)
        matcher.process(xinclude, "*")
        matcher.result
      }

      override def startDocument(node: XdmNode): Boolean = {
        matcher.startDocument(node.getBaseURI)
        true
      }

      override def endDocument(node: XdmNode): Unit = {
        matcher.endDocument()
      }

      override def startElement(node: XdmNode): Boolean = {
        matcher.addStartElement(node)

        val copied = mutable.HashSet.empty[QName]
        if (root) {
          root = false
          if (copyAttributes) {
            val setId = setXmlId.head
            if (setId.isDefined) {
              copied.add(XProcConstants.xml_id)
              if (setId.get != "") {
                matcher.addAttribute(XProcConstants.xml_id, setId.get)
              }
            }

            for (child <- S9Api.axis(node, Axis.ATTRIBUTE)) {
              // Attribute must be in a namespace
              var copy = child.getNodeName.getNamespaceURI != ""
              // But not in the XML namespace
              copy = copy && child.getNodeName.getNamespaceURI != XProcConstants.ns_xml

              if (copy) {
                var aname = child.getNodeName
                if (localAttrNS.equals(aname.getNamespaceURI)) {
                  aname = new QName("", aname.getLocalName)
                }
                copied.add(aname)
                matcher.addAttribute(aname, child.getStringValue)
              }
            }
          }

          for (child <- S9Api.axis(node, Axis.ATTRIBUTE)) {
            if ((fixupBase && child.getNodeName == XProcConstants.xml_base)
              || (fixupLang && child.getNodeName == XProcConstants.xml_lang)) {
              // nop
            } else {
              if (!copied.contains(child.getNodeName)) {
                copied.add(child.getNodeName)
                matcher.addAttribute(child)
              }
            }
          }

          if (fixupBase) {
            copied.add(XProcConstants.xml_base)
            matcher.addAttribute(XProcConstants.xml_base, node.getBaseURI.toASCIIString)
          }

          val lang = getLang(node)
          if (fixupLang && lang.isDefined) {
            copied.add(XProcConstants.xml_lang)
            matcher.addAttribute(XProcConstants.xml_lang, lang.get)
          }
        } else {
          // Careful. Don't copy ones we've already copied...
          for (child <- S9Api.axis(node, Axis.ATTRIBUTE)) {
            if (!copied.contains(child.getNodeName)) {
              copied.add(child.getNodeName)
              matcher.addAttribute(child)
            }
          }
        }

        matcher.startContent()
        true
      }

      override def endElement(node: XdmNode): Unit = {
         matcher.addEndElement()
      }

      override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = false

      override def attribute(node: XdmNode): Unit = {
        throw new UnsupportedOperationException("attribute() called in XInclude fixup")
      }

      override def text(node: XdmNode): Unit = {
        throw new UnsupportedOperationException("text() called in XInclude fixup")
      }

      override def comment(node: XdmNode): Unit = {
        throw new UnsupportedOperationException("comment() called in XInclude fixup")
      }

      override def pi(node: XdmNode): Unit = {
        throw new UnsupportedOperationException("pi() called in XInclude fixup")
      }

      private def getLang(node: XdmNode): Option[String] = {
        var p = node
        var lang = Option.empty[String]
        while (lang.isEmpty && p.getNodeKind == XdmNodeKind.ELEMENT) {
          lang = Option(p.getAttributeValue(XProcConstants.xml_lang))
          p = p.getParent
        }
        lang
      }
    }

  /*

        public XdmNode fixup(XdmNode node) {
            matcher = new ProcessMatch(runtime, this);
            matcher.match(node, new RuntimeValue("*", step.getNode()));
            XdmNode fixed = matcher.getResult();
            return fixed;
        }

        public boolean processStartDocument(XdmNode node) throws SaxonApiException {
            matcher.startDocument(node.getBaseURI());
            return true;
        }

        public void processEndDocument(XdmNode node) throws SaxonApiException {
            matcher.endDocument();
        }

        public boolean processStartElement(XdmNode node) throws SaxonApiException {
            HashSet<QName> copied = new HashSet<QName> ();
            matcher.addStartElement(node);

            if (root) {
                root = false;

                if (copyAttributes) {
                    // Handle set-xml-id; it suppresses copying the xml:id attribute and optionally
                    // provides a value for it. (The value "" removes the xml:id.)
                    String setId = setXmlId.peek();
                    if (setId != null) {
                        copied.add(XProcConstants.xml_id);
                        if (!"".equals(setId)) {
                            matcher.addAttribute(XProcConstants.xml_id, setId);
                        }
                    }

                    XdmSequenceIterator iter = xinclude.axisIterator(Axis.ATTRIBUTE);
                    while (iter.hasNext()) {
                        XdmNode child = (XdmNode) iter.next();

                        // Attribute must be in a namespace
                        boolean copy = !"".equals(child.getNodeName().getNamespaceURI());

                        // But not in the XML namespace
                        copy = copy && !XProcConstants.NS_XML.equals(child.getNodeName().getNamespaceURI());

                        if (copy) {
                            QName aname = child.getNodeName();
                            if (localAttrNS.equals(aname.getNamespaceURI())) {
                                aname = new QName("", aname.getLocalName());
                            }

                            copied.add(aname);
                            matcher.addAttribute(aname, child.getStringValue());
                        }
                    }
                }

                XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
                while (iter.hasNext()) {
                    XdmNode child = (XdmNode) iter.next();
                    if ((XProcConstants.xml_base.equals(child.getNodeName()) && fixupBase)
                        || (XProcConstants.xml_lang.equals(child.getNodeName()) && fixupLang)) {
                        // nop;
                    } else {
                        if (!copied.contains(child.getNodeName())) {
                            copied.add(child.getNodeName());
                            matcher.addAttribute(child);
                        }
                    }
                }
                if (fixupBase) {
                    copied.add(XProcConstants.xml_base);
                    matcher.addAttribute(XProcConstants.xml_base, node.getBaseURI().toASCIIString());
                }
                String lang = getLang(node);
                if (fixupLang && lang != null) {
                    copied.add(XProcConstants.xml_lang);
                    matcher.addAttribute(XProcConstants.xml_lang, lang);
                }
            } else {
                // Careful. Don't copy ones you've already copied...
                XdmSequenceIterator iter = node.axisIterator(Axis.ATTRIBUTE);
                while (iter.hasNext()) {
                    XdmNode child = (XdmNode) iter.next();
                    if (!copied.contains(child.getNodeName())) {
                        matcher.addAttribute(child);
                    }
                }
            }

            matcher.startContent();
            return true;
        }

        public void processAttribute(XdmNode node) throws SaxonApiException {
            throw new XProcException(node, "This can't happen!?");
        }

        public void processEndElement(XdmNode node) throws SaxonApiException {
            matcher.addEndElement();
        }

        public void processText(XdmNode node) throws SaxonApiException {
            throw new XProcException(node, "This can't happen!?");
        }

        public void processComment(XdmNode node) throws SaxonApiException {
            throw new XProcException(node, "This can't happen!?");
        }

        public void processPI(XdmNode node) throws SaxonApiException {
            throw new XProcException(node, "This can't happen!?");
        }

        private String getLang(XdmNode node) {
            String lang = null;
            while (lang == null && node.getNodeKind() == XdmNodeKind.ELEMENT) {
                lang = node.getAttributeValue(XProcConstants.xml_lang);
                node = node.getParent();
            }
            return lang;
        }
    }

   */
}
