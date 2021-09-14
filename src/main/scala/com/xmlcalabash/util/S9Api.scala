package com.xmlcalabash.util

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XMLCalabashRuntime}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.{AttributeInfo, FingerprintedQName, NameOfNode, NamespaceBinding, NamespaceMap}
import net.sf.saxon.s9api._
import net.sf.saxon.serialize.SerializationProperties
import net.sf.saxon.value.QNameValue
import org.xml.sax.InputSource

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala, SeqHasAsJava}

object S9Api {
  val OPEN_BRACE = new XdmAtomicValue("{")
  val CLOSE_BRACE = new XdmAtomicValue("}")
  val OPEN_SQUARE = new XdmAtomicValue("[")
  val CLOSE_SQUARE = new XdmAtomicValue("]")
  val DOUBLE_QUOTE = new XdmAtomicValue("\"")
  val COMMA = new XdmAtomicValue(",")
  val NEWLINE = new XdmAtomicValue("\n")
  val COLON = new XdmAtomicValue(":")
  val SPACE = new XdmAtomicValue(" ")
  val NULL = new XdmAtomicValue("null")

  val vara = new QName("", "vara")
  val varb = new QName("", "varb")

  def axis(node: XdmNode, axis: Axis): List[XdmNode] = {
    val lb = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(axis)
    while (iter.hasNext) {
      lb += iter.next()
    }
    lb.toList
  }

  def documentElement(doc: XdmNode): Option[XdmNode] = {
    doc.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = doc.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val node = iter.next
          if (node.getNodeKind == XdmNodeKind.ELEMENT) {
            return Some(node)
          }
        }
        None
      case XdmNodeKind.ELEMENT =>
        Some(doc)
      case _ =>
        None
    }
  }

  def inScopeNamespaces(node: XdmNode): Map[String,String] = {
    val nsiter = node.axisIterator(Axis.NAMESPACE)
    val ns = mutable.HashMap.empty[String,String]
    while (nsiter.hasNext) {
      val attr = nsiter.next()
      val prefix = if (attr.getNodeName == null) {
        ""
      } else {
        attr.getNodeName.toString
      }
      val uri = attr.getStringValue
      ns.put(prefix, uri)
    }
    ns.toMap
  }

  // FIXME: THIS METHOD IS A GROTESQUE HACK!
  def xdmToInputSource(config: XMLCalabashConfig, node: XdmNode): InputSource = {
    val out = new ByteArrayOutputStream()
    val serializer = config.processor.newSerializer
    serializer.setOutputStream(out)
    serialize(config, node, serializer)
    val isource = new InputSource(new ByteArrayInputStream(out.toByteArray))
    if (node.getBaseURI != null) {
      isource.setSystemId(node.getBaseURI.toASCIIString)
    }
    isource
  }

  def valuesToString(values: XdmValue): String = {
    var str = ""
    var sep = ""
    for (pos <- 1 to values.size) {
      str = str + sep + values.itemAt(pos - 1).getStringValue
      sep = " "
    }
    str
  }

  def configureSerializer(serializer: Serializer, options: Map[QName,String]): Unit = {
    for (opt <- options.keySet) {
      try {
        serializer.setOutputProperty(opt, options(opt))
      } catch {
        case _: IllegalArgumentException =>
          // You attempted to set a serialization parameter that is unknown. Ignore it.
          ()
        case ex: Exception =>
          throw(ex)
      }
    }
  }

  def serialize(config: XMLCalabashConfig, value: XdmValue, serializer: Serializer): Unit = {
    serialize(config, List(value), serializer)
  }

  def serialize(xproc: XMLCalabashConfig, values: List[XdmValue], serializer: Serializer): Unit = {
    for (value <- values) {
      value match {
        case arr: XdmArray =>
          serializeArr(xproc, arr, serializer)
        case map: XdmMap =>
          serializeMap(xproc, map, serializer)
        case empty: XdmEmptySequence =>
          serializer.serializeXdmValue(NULL)
        case atomic: XdmAtomicValue =>
          atomic.getPrimitiveTypeName match {
            case XProcConstants.xs_string =>
              serializer.serializeXdmValue(DOUBLE_QUOTE)
              serializer.serializeXdmValue(atomic)
              serializer.serializeXdmValue(DOUBLE_QUOTE)
            case _ =>
              serializer.serializeXdmValue(atomic)
          }
        case _ => serializer.serializeXdmValue(value)
      }
    }
  }

  private def serializeMap(xproc: XMLCalabashConfig, value: XdmMap, serializer: Serializer): Unit = {
    serializer.serializeXdmValue(OPEN_BRACE)
    val map = value.asMap()

    var first = true
    for (key <- map.asScala.keySet) {
      val value = map.asScala(key)
      if (!first) {
        serializer.serializeXdmValue(COMMA)
        serializer.serializeXdmValue(NEWLINE)
      }
      first = false

      serializer.serializeXdmValue(DOUBLE_QUOTE)
      serializer.serializeXdmValue(key)
      serializer.serializeXdmValue(DOUBLE_QUOTE)
      serializer.serializeXdmValue(COLON)
      serializer.serializeXdmValue(SPACE)
      serialize(xproc, value, serializer)
    }
    serializer.serializeXdmValue(CLOSE_BRACE)
  }

  private def serializeArr(xproc: XMLCalabashConfig, arr: XdmArray, serializer: Serializer): Unit = {
    serializer.serializeXdmValue(OPEN_SQUARE)

    var idx = 0
    for (idx <- 0  until arr.arrayLength()) {
      val value = arr.get(idx)
      if (idx > 0) {
        serializer.serializeXdmValue(COMMA)
        serializer.serializeXdmValue(SPACE)
      }

      serialize(xproc, value, serializer)
    }
    serializer.serializeXdmValue(CLOSE_SQUARE)
  }

  def serializationPropertyMap(props: SerializationProperties): Map[QName, XdmValue] = {
    val map = mutable.HashMap.empty[QName, XdmValue]
    for ((key,value) <- props.getProperties.asScala) {
      map.put(ValueParser.parseClarkName(key.asInstanceOf[String]), new XdmAtomicValue(value.asInstanceOf[String]))
    }
    map.toMap
  }

  def emptyDocument(config: XMLCalabashRuntime): XdmNode = {
    emptyDocument(config, None)
  }

  def emptyDocument(config: XMLCalabashRuntime, baseURI: Option[URI]): XdmNode = {
    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(baseURI)
    tree.endDocument()
    tree.result
  }

  def forceQNameKeys(inputMap: MapItem, context: StaticContext): XdmMap = {
    var map = new XdmMap()

    val iter = inputMap.keyValuePairs().iterator()
    while (iter.hasNext) {
      val pair = iter.next()
      pair.key.getItemType match {
        case BuiltInAtomicType.STRING =>
          val qname = ValueParser.parseQName(pair.key.getStringValue, context)
          map = map.put(new XdmAtomicValue(qname), XdmValue.wrap(pair.value))
        case BuiltInAtomicType.QNAME =>
          val qvalue = pair.key.asInstanceOf[QNameValue]
          val key = new QName(qvalue.getPrefix, qvalue.getNamespaceURI, qvalue.getLocalName)
          map = map.put(new XdmAtomicValue(key), XdmValue.wrap(pair.value))
        case _ =>
          // FIXME: not sure this works (given that it doesn't work for QNameValues)
          map = map.put(pair.key.asInstanceOf[XdmAtomicValue], XdmValue.wrap(pair.value))
      }
    }

    map
  }

  def excludeInlineURIs(node: XdmNode): Set[String] = {
    val excludeURIs = mutable.HashSet.empty[String] ++ Set(XProcConstants.ns_p)

    var parent = node
    while (parent.getNodeKind == XdmNodeKind.ELEMENT) {
      val excludePrefixes = if (parent.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
        Option(parent.getAttributeValue(XProcConstants._exclude_inline_prefixes))
      } else {
        Option(parent.getAttributeValue(XProcConstants.p_exclude_inline_prefixes))
      }
      if (excludePrefixes.isDefined) {
        excludeURIs ++= S9Api.urisForPrefixes(parent, excludePrefixes.get.split("\\s+").toSet)
      }
      parent = parent.getParent
    }

    excludeURIs.toSet
  }

  def urisForPrefixes(node: XdmNode, prefixList: Set[String]): Set[String] = {
    val uriList = mutable.HashSet.empty[String]
    val nsmap = node.getUnderlyingNode.getAllNamespaces
    var all = false

    for (pfx <- prefixList) {
      var found = false

      if (pfx == "#all") {
        found = true
        all = true
      } else {
        if (pfx == "#default") {
          found = nsmap.getURIForPrefix("", true) != null
          if (found) {
            uriList += nsmap.getURIForPrefix("", true)
          }
        } else {
          found = nsmap.getURIForPrefix(pfx, false) != null
          if (found) {
            uriList += nsmap.getURIForPrefix(pfx, false)
          }
        }
      }

      if (!found) {
        throw new RuntimeException("No binding for prefix: " + pfx)
      }
    }

    if (all) {
      val pfxiter = nsmap.iteratePrefixes()
      while (pfxiter.hasNext) {
        val pfx = pfxiter.next()
        val isdef = (pfx == "")
        val nsuri = nsmap.getURIForPrefix(pfx, isdef)
        // Never exclude the xml: and xmlns: prefixes.
        if (nsuri != XProcConstants.ns_xml && nsuri != XProcConstants.ns_xmlns) {
          uriList += nsuri
        }
      }
    }

    uriList.toSet
  }

  def uniquePrefix(prefixes: Set[String]): String = {
    var acount = 0
    var aprefix = "_0"
    var done = false
    while (!done) {
      acount += 1
      aprefix = s"_$acount"
      done = !prefixes.contains(aprefix)
    }

    aprefix
  }

  def removeNamespaces(config: XMLCalabashConfig, node: XdmNode, excludeNS: Set[String], preserveUsed: Boolean): XdmNode = {
    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(node.getBaseURI)
    removeNamespacesWriter(tree, node, excludeNS, preserveUsed)
    tree.endDocument()
    tree.result
  }

  private def removeNamespacesWriter(tree: SaxonTreeBuilder, node: XdmNode, excludeNS: Set[String], preserveUsed: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        for (child <- S9Api.axis(node, Axis.CHILD)) {
          removeNamespacesWriter(tree, child, excludeNS, preserveUsed)
        }
      case XdmNodeKind.ELEMENT =>
        val nsmap = node.getUnderlyingNode.getAllNamespaces
        val usesDefaultNS = node.getNodeName.getPrefix == "" && node.getNodeName.getNamespaceURI != ""
        val inode = node.getUnderlyingNode
        var excludeDefault = false
        var changed = false

        val newNS = ListBuffer.empty[NamespaceBinding]
        val pfxiter = nsmap.iteratePrefixes()
        while (pfxiter.hasNext) {
          val pfx = pfxiter.next
          val uri = nsmap.getURIForPrefix(pfx, "".equals(pfx))

          var delete = excludeNS.contains(uri)
          excludeDefault = excludeDefault || (pfx == "" && delete)

          // You can't exclude the default namespace if it's in use
          if (pfx == "" && usesDefaultNS && preserveUsed) {
            delete = false
          }

          changed |= delete

          if (!delete) {
            newNS += new NamespaceBinding(pfx, uri)
          }
        }

        val attrs = inode.attributes()
        val attrList = ListBuffer.empty[AttributeInfo]
        var newName = NameOfNode.makeName(inode)
        if (!preserveUsed) {
          val binding = newName.getNamespaceBinding
          if (excludeNS.contains(binding.getURI)) {
            newName = new FingerprintedQName("", "", newName.getLocalPart)
          }

          // In this case we may need to change some attributes too
          for (attr <- attrs.asList().asScala) {
            val attrns = attr.getNodeName.getURI
            if (excludeNS.contains(attrns)) {
              val newAttrName = new FingerprintedQName("", "", attr.getNodeName.getLocalPart)
              val newAttr = new AttributeInfo(newAttrName, attr.getType, attr.getValue, attr.getLocation, attr.getProperties)
              attrList += newAttr
            } else {
              attrList += attr
            }
          }
        }

        tree.addStartElement(newName, attrs, inode.getSchemaType, new NamespaceMap(newNS.toList.asJava))

        for (child <- S9Api.axis(node, Axis.CHILD)) {
          removeNamespacesWriter(tree, child, excludeNS, preserveUsed)
        }

        tree.addEndElement()

      case _ =>
        tree.addSubtree(node)
    }
  }

  def xpathEqual(config: XMLCalabashRuntime, left: XdmValue, right: XdmValue): Boolean = {
    val xcomp = config.processor.newXPathCompiler()
    xcomp.declareVariable(vara)
    xcomp.declareVariable(varb)
    val xexec = xcomp.compile("$vara = $varb")
    val selector = xexec.load()
    selector.setVariable(vara, left)
    selector.setVariable(varb, right)

    val values = selector.iterator()
    val item = values.next.asInstanceOf[XdmAtomicValue]
    item.getBooleanValue
  }

  def xpathDeepEqual(config: XMLCalabashRuntime, left: XdmValue, right: XdmValue): Boolean = {
    val xcomp = config.processor.newXPathCompiler()
    xcomp.declareVariable(vara)
    xcomp.declareVariable(varb)
    val xexec = xcomp.compile("deep-equal($vara,$varb)")
    val selector = xexec.load()
    selector.setVariable(vara, left)
    selector.setVariable(varb, right)

    val values = selector.iterator()
    val item = values.next.asInstanceOf[XdmAtomicValue]
    item.getBooleanValue
  }

  def assertDocument(doc: XdmNode): Unit = {
    if (doc.getNodeKind == XdmNodeKind.DOCUMENT) {
      assertDocumentContent(doc.axisIterator(Axis.CHILD))
    } else if (doc.getNodeKind != XdmNodeKind.ELEMENT) {
      throw new RuntimeException(s"Document root cannot be ${doc.getNodeKind}")
    }
  }

  def assertDocumentContent(iter: XdmSequenceIterator[XdmNode]): Unit = {
    var elemCount = 0
    while (iter.hasNext) {
      val child = iter.next()
      child.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          elemCount += 1
          if (elemCount > 1) {
            throw new RuntimeException("Documents must have exactly one top-level element")
          }
        case XdmNodeKind.PROCESSING_INSTRUCTION =>
          ()
        case XdmNodeKind.COMMENT =>
          ()
        case XdmNodeKind.TEXT =>
          if (child.getStringValue.trim != "") {
            throw new RuntimeException("Only whitespace text nodes can appear at the top level in a document")
          }
        case _ =>
          throw new RuntimeException(s"Document cannot have top level ${child.getNodeKind}")
      }
    }
  }
}
