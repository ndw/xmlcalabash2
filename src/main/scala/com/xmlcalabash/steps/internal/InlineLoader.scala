package com.xmlcalabash.steps.internal

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.util.Base64

import com.jafpl.messages.{ItemMessage, Message}
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{DynamicContext, ExpressionContext, SaxonExpressionEvaluator, XProcExpression, XProcMetadata, XProcVtExpression, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{Axis, QName, SaxonApiException, XdmAtomicValue, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}
import org.xml.sax.InputSource

import scala.collection.mutable

class InlineLoader(private val baseURI: Option[URI],
                   private val nodes: List[XdmNode],
                   private val context: ExpressionContext,
                   private val expandText: Boolean,
                   private val excludeInlinePrefixes: Map[String,String],
                   private val declContentType: Option[MediaType],
                   private val docPropsExpr: Option[String],
                   private val encoding: Option[String]) extends DefaultStep {
  private var include_expand_text_attribute = false
  private var docProps = Map.empty[QName, XdmItem]
  private val excludeURIs = mutable.HashSet.empty[String]
  private var _allowExpandText = true

  excludeURIs += XProcConstants.ns_p
  for (uri <- excludeInlinePrefixes.values) {
    excludeURIs += uri
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  protected[xmlcalabash] def allowExpandText: Boolean = _allowExpandText
  protected[xmlcalabash] def allowExpandText_=(allow: Boolean): Unit = {
    _allowExpandText = allow
    include_expand_text_attribute = !allow
  }

  override def run(): Unit = {
    if (docPropsExpr.isDefined) {
      val expr = new XProcXPathExpression(context, docPropsExpr.get)
      val result = xpathValue(expr)
      docProps = result match {
        case map: XdmMap =>
          ValueParser.parseDocumentProperties(map, location)
        case _ =>
          throw XProcException.xsBadTypeValue("document-properties", "map")
      }
    }

    var propContentType = if (docProps.contains(XProcConstants._content_type)) {
      Some(MediaType.parse(docProps.get(XProcConstants._content_type).toString))
    } else {
      None
    }

    val contentType = if (propContentType.isDefined) {
      if (declContentType.isDefined) {
        if (!declContentType.get.matches(propContentType.get)) {
          throw XProcException.xdMismatchedContentType(declContentType.get, propContentType.get, location)
        }
      }
      propContentType.get
    } else {
      if (declContentType.isDefined) {
        declContentType.get
      } else {
        MediaType.XML
      }
    }

    if (encoding.isDefined) {
      if (contentType.xmlContentType) {
        throw XProcException.staticError(70, List(encoding.get, contentType), location)
      }
      if (encoding.get != "base64") {
        throw XProcException.staticError(69, List(encoding.get), location)
      }
    }

    val props = mutable.HashMap.empty[QName, XdmItem]
    props ++= docProps

    if (baseURI.isDefined) {
      props.put(XProcConstants._base_uri, new XdmAtomicValue(baseURI.get))
    }

    // If it's not an XML content type, make sure it doesn't contain any elements
    if (!contentType.xmlContentType && !contentType.htmlContentType) {
      for (node <- nodes.filter(_.getNodeKind != XdmNodeKind.TEXT)) {
        throw XProcException.xsInvalidNodeType(node.getNodeKind.toString, location)
      }
    }

    if (encoding.isDefined) {
      /* See https://github.com/xproc/3.0-specification/issues/561
      if (expandText) {
        throw XProcException.xsTvtForbidden(location)
      }
      */
      dealWithEncodedText(contentType, props)
      return
    }

    if (contentType.xmlContentType) {
      val builder = new SaxonTreeBuilder(config.get)
      builder.startDocument(baseURI)
      builder.startContent()
      for (node <- nodes) {
        if (allowExpandText) {
          expandTVT(node, builder, expandText)
        } else {
          builder.addSubtree(node)
        }
      }
      builder.endDocument()
      val result = builder.result
      consumer.get.receive("result", new ItemMessage(result, new XProcMetadata(contentType, props.toMap)))
    } else if (contentType.htmlContentType) {
      val builder = new SaxonTreeBuilder(config.get)
      builder.startDocument(baseURI)
      builder.startContent()
      for (node <- nodes) {
        if (allowExpandText) {
          expandTVT(node, builder, expandText)
        } else {
          builder.addSubtree(node)
        }
      }
      builder.endDocument()
      val result = builder.result

      val baos = new ByteArrayOutputStream()
      val serializer = config.get.processor.newSerializer(baos)
      S9Api.serialize(config.get, result, serializer)
      val stream = new ByteArrayInputStream(baos.toByteArray)
      // FIXME: it's bogus that I have to makeup a DocumentRequest to call parseHtml
      val request = new DocumentRequest(baseURI.getOrElse(new URI("")), Some(contentType), false)
      val response = config.get.documentManager.parseHtml(request, new InputSource(stream))
      consumer.get.receive("result", new ItemMessage(response.value, new XProcMetadata(response.contentType, response.props.toMap)))
    } else {
      val text = if (allowExpandText) {
        val builder = new SaxonTreeBuilder(config.get)
        builder.startDocument(baseURI)
        builder.startContent()
        for (node <- nodes) {
          if (allowExpandText) {
            expandTVT(node, builder, expandText)
          } else {
            builder.addSubtree(node)
          }
        }
        builder.endDocument()
        val result = builder.result
        // No sneaky poking an element in there!
        val iter = result.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next.asInstanceOf[XdmNode]
          if (child.getNodeKind != XdmNodeKind.TEXT) {
            throw XProcException.xsInvalidNodeType(child.getNodeKind.toString, location)
          }
        }
        result.getStringValue
      } else {
        var str = ""
        for (node <- nodes) {
          str += node.getStringValue
        }
        str
      }

      if (contentType.jsonContentType) {
        val expr = new XProcXPathExpression(ExpressionContext.NONE, "parse-json($json)")
        val bindingsMap = mutable.HashMap.empty[String, Message]
        val vmsg = new XPathItemMessage(new XdmAtomicValue(text), XProcMetadata.JSON, ExpressionContext.NONE)
        bindingsMap.put("{}json", vmsg)
        try {
          val smsg = config.get.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)
          consumer.get.receive("result", new ItemMessage(smsg.item, new XProcMetadata(contentType, props.toMap)))
        } catch {
          case ex: SaxonApiException =>
            if (ex.getMessage.contains("Invalid JSON")) {
              throw XProcException.xdInvalidJson(text, ex.getMessage, location)
            } else {
              throw ex
            }
          case ex: Exception =>
            throw ex
        }
      } else if (contentType.htmlContentType) {
        val stream = new ByteArrayInputStream(text.getBytes("UTF-8"))
        // FIXME: it's bogus that I have to makeup a DocumentRequest to call parseHtml
        val request = new DocumentRequest(baseURI.getOrElse(new URI("")), Some(contentType), false)
        val response = config.get.documentManager.parseHtml(request, new InputSource(stream))
        consumer.get.receive("result", new ItemMessage(response.value, new XProcMetadata(response.contentType, response.props.toMap)))
      } else if (contentType.textContentType) {
        props.put(XProcConstants._content_length, new XdmAtomicValue(text.length))

        val builder = new SaxonTreeBuilder(config.get)
        builder.startDocument(baseURI)
        builder.startContent()
        builder.addText(text)
        builder.endDocument()
        val result = builder.result
        consumer.get.receive("result", new ItemMessage(result, new XProcMetadata(contentType, props.toMap)))
      } else {
        throw new IllegalArgumentException(s"Unexected content type: $contentType")
      }
    }
  }

  private def dealWithEncodedText(contentType: MediaType, props: mutable.HashMap[QName, XdmItem]): Unit = {
    var str = ""
    for (node <- nodes) {
      str += node.getStringValue
    }

    // FIXME: There's no support here for any kind of media type!!!

    val decoded = Base64.getMimeDecoder.decode(str)
    props.put(XProcConstants._content_length, new XdmAtomicValue(decoded.length))
    consumer.get.receive("result", new ItemMessage(decoded, new XProcMetadata(contentType, props.toMap)))
  }

  private def expandTVT(node: XdmNode, builder: SaxonTreeBuilder, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          expandTVT(child, builder, expandText)
        }
      case XdmNodeKind.ELEMENT =>
        builder.addStartElement(node.getNodeName)
        var iter = node.axisIterator(Axis.NAMESPACE)
        while (iter.hasNext) {
          val ns = iter.next().asInstanceOf[XdmNode]
          if (!excludeURIs.contains(ns.getStringValue)) {
            val prefix = if (Option(ns.getNodeName).isDefined) {
              ns.getNodeName.getLocalName
            } else {
              ""
            }
            builder.addNamespace(prefix, ns.getStringValue)
          }
        }
        var newExpand = expandText
        iter = node.axisIterator(Axis.ATTRIBUTE)
        while (iter.hasNext) {
          val attr = iter.next().asInstanceOf[XdmNode]
          if (attr.getNodeName == XProcConstants.p_expand_text) {
            newExpand = attr.getStringValue == "true"
          }
          if (include_expand_text_attribute || (attr.getNodeName != XProcConstants.p_expand_text)) {
            if (expandText) {
              builder.addAttribute(attr.getNodeName, expandString(attr.getStringValue))
            } else {
              builder.addAttribute(attr)
            }
          }
        }
        iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          expandTVT(child, builder, newExpand && _allowExpandText)
        }
        builder.addEndElement()
      case XdmNodeKind.TEXT =>
        var str = node.getStringValue
        if (expandText && str.contains("{")) {
          expandNodes(str, builder)
        } else {
          builder.addText(str)
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  private def expandString(text: String): String = {
    val evaluator = config.get.expressionEvaluator
    val expr = new XProcVtExpression(context, text)
    var s = ""
    val iter = evaluator.value(expr, List.empty[Message], bindings.toMap, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      s += next.getStringValue
    }
    s
  }

  private def expandNodes(text: String, builder: SaxonTreeBuilder): Unit = {
    val evaluator = config.get.expressionEvaluator
    val expr = new XProcVtExpression(context, text)
    val iter = evaluator.value(expr, List.empty[Message], bindings.toMap, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      next match {
        case node: XdmNode => builder.addSubtree(node)
        case _ => builder.addText(next.getStringValue)
      }
    }
  }

  def xpathValue(expr: XProcExpression): XdmValue = {
    val eval = config.get.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
    val dynContext = new DynamicContext()
    val msg = eval.withContext(dynContext) { eval.singletonValue(expr, List.empty[Message], bindings.toMap, None) }
    msg.asInstanceOf[XPathItemMessage].item
  }
}
