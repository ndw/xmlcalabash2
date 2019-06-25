package com.xmlcalabash.steps.internal

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.util.Base64

import com.jafpl.messages.Message
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{AnyItemMessage, XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, DynamicContext, ExpressionContext, SaxonExpressionEvaluator, XProcExpression, XProcMetadata, XProcVtExpression, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{Axis, QName, SaxonApiException, XdmAtomicValue, XdmItem, XdmMap, XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class InlineLoader(private val baseURI: Option[URI],
                   private val nodes: List[XdmNode],
                   private val context: ExpressionContext,
                   private val expandText: Boolean,
                   private val excludeUriBindings: Set[String],
                   private val declContentType: Option[MediaType],
                   private val docPropsExpr: Option[String],
                   private val encoding: Option[String]) extends DefaultStep {
  private var docProps = Map.empty[QName, XdmItem]
  private val excludeURIs = mutable.HashSet.empty[String]

  excludeURIs += XProcConstants.ns_p
  for (uri <- excludeUriBindings) {
    excludeURIs += uri
  }

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

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
        throw XProcException.xdCannotEncodeXml(encoding.get, contentType, location)
      }
      if (encoding.get != "base64") {
        throw XProcException.xsUnsupportedEncoding(encoding.get, location)
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
        throw XProcException.xdNoMarkupAllowed(location)
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
      for (node <- trim(nodes)) {
        expandTVT(node, builder, expandText)
      }
      builder.endDocument()
      val result = builder.result
      val metadata = new XProcMetadata(contentType, props.toMap)
      val message = new XdmNodeItemMessage(result, metadata)
      consumer.get.receive("result", message)
    } else if (contentType.htmlContentType) {
      val builder = new SaxonTreeBuilder(config.get)
      builder.startDocument(baseURI)
      builder.startContent()
      for (node <- trim(nodes)) {
        expandTVT(node, builder, expandText)
      }
      builder.endDocument()
      val result = builder.result

      val baos = new ByteArrayOutputStream()
      val serializer = config.get.config.processor.newSerializer(baos)
      S9Api.serialize(config.get.config, result, serializer)
      val stream = new ByteArrayInputStream(baos.toByteArray)

      val request = new DocumentRequest(baseURI.getOrElse(new URI("")), contentType, location)
      val response = config.get.documentManager.parse(request, stream)
      val metadata = new XProcMetadata(response.contentType, response.props)

      response.value match {
        case node: XdmNode =>
          consumer.get.receive("result", new XdmNodeItemMessage(node, metadata))
        case _ =>
          throw new RuntimeException("Unexpected node type from parseHtml")
      }
    } else {
      val text = if (expandText) {
        val builder = new SaxonTreeBuilder(config.get)
        builder.startDocument(baseURI)
        builder.startContent()
        for (node <- nodes) {
          expandTVT(node, builder, expandText)
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
        val vmsg = new XdmValueItemMessage(new XdmAtomicValue(text), XProcMetadata.JSON, ExpressionContext.NONE)
        bindingsMap.put("{}json", vmsg)
        try {
          val smsg = config.get.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)
          consumer.get.receive("result", new XdmValueItemMessage(smsg.item, new XProcMetadata(contentType, props.toMap)))
        } catch {
          case ex: SaxonApiException =>
            if (ex.getMessage.contains("Invalid JSON")) {
              throw XProcException.xdInvalidJson(ex.getMessage, location)
            } else {
              throw ex
            }
          case ex: Exception =>
            throw ex
        }
      } else if (contentType.textContentType) {
        props.put(XProcConstants._content_length, new XdmAtomicValue(text.length))

        val builder = new SaxonTreeBuilder(config.get)
        builder.startDocument(baseURI)
        builder.startContent()
        builder.addText(text)
        builder.endDocument()
        val result = builder.result
        consumer.get.receive("result", new XdmNodeItemMessage(result, new XProcMetadata(contentType, props.toMap)))
      } else {
        throw new IllegalArgumentException(s"Unexected content type: $contentType")
      }
    }
  }

  private def trim(nodes: List[XdmNode]): List[XdmNode] = {
    if (config.get.trimInlineWhitespace) {
      var count = 1
      var trimmed = ListBuffer.empty[XdmNode]
      trimmed ++= nodes
      if (nodes.nonEmpty) {
        if (nodes.head.getNodeKind == XdmNodeKind.TEXT && nodes.head.getStringValue.trim == "") {
          trimmed = trimmed.drop(1)
        }
        if (nodes.length >= 2) {
          if (nodes.last.getNodeKind == XdmNodeKind.TEXT && nodes.last.getStringValue.trim == "") {
            trimmed = trimmed.dropRight(1)
          }
        }
      }
      trimmed.toList
    } else {
      nodes
    }
  }

  private def dealWithEncodedText(contentType: MediaType, props: mutable.HashMap[QName, XdmItem]): Unit = {
    var str = ""
    for (node <- nodes) {
      str += node.getStringValue
    }
    val decoded = Base64.getMimeDecoder.decode(str)

    val metadata = new XProcMetadata(contentType, props.toMap)

    if (contentType.xmlContentType || contentType.htmlContentType || contentType.textContentType || contentType.jsonContentType) {
      val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), contentType)
      val result = config.get.documentManager.parse(req, new ByteArrayInputStream(decoded))
      if (result.shadow.isDefined) {
        val binary = new BinaryNode(config.get, result.shadow)
        consumer.get.receive("result", new AnyItemMessage(S9Api.emptyDocument(config.get), binary, metadata))
      } else {
        result.value match {
          case node: XdmNode =>
            consumer.get.receive("result", new XdmNodeItemMessage(node, metadata))
          case _ =>
            consumer.get.receive("result", new XdmValueItemMessage(result.value, metadata))
        }
      }
    } else {
      // Octet stream, I guess
      props.put(XProcConstants._content_length, new XdmAtomicValue(decoded.length))
      val binary = new BinaryNode(config.get, decoded)
      consumer.get.receive("result", new AnyItemMessage(S9Api.emptyDocument(config.get), binary, new XProcMetadata(contentType, props.toMap)))
    }
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
          var discardAttribute = false
          val attr = iter.next().asInstanceOf[XdmNode]
          if (attr.getNodeName == XProcConstants.p_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              throw XProcException.xsInlineExpandTextNotAllowed(location)
            }
            discardAttribute = true
            newExpand = attr.getStringValue == "true"
          }
          if (attr.getNodeName == XProcConstants._inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              discardAttribute = true
              newExpand = attr.getStringValue == "true"
            }
          }
          if (!discardAttribute) {
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
          expandTVT(child, builder, newExpand)
        }
        builder.addEndElement()
      case XdmNodeKind.TEXT =>
        var str = node.getStringValue
        if (expandText && str.contains("{")) {
          expandNodes(str, builder)
        } else {
          builder.addText(str.replace("}}", "}"))
        }
      case _ =>
        builder.addSubtree(node)
    }
  }

  private def expandString(text: String): String = {
    val evaluator = config.get.expressionEvaluator
    val expr = new XProcVtExpression(context, text)
    var s = ""
    var string = ""
    val iter = evaluator.value(expr, List.empty[Message], config.get.runtimeBindings(bindings.toMap), None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      string = string + s + next.getStringValue
      s = " "
    }
    string
  }

  private def expandNodes(text: String, builder: SaxonTreeBuilder): Unit = {
    val evaluator = config.get.expressionEvaluator
    val expr = new XProcVtExpression(context, text)

    val iter = evaluator.value(expr, List.empty[Message], config.get.runtimeBindings(bindings.toMap), None).item.iterator()
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
    msg.asInstanceOf[XdmValueItemMessage].item
  }
}
