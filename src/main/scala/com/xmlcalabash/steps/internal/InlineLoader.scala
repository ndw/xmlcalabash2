package com.xmlcalabash.steps.internal

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.util.Base64
import com.jafpl.messages.Message
import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.params.InlineLoaderParams
import com.xmlcalabash.runtime.{BinaryNode, ImplParams, StaticContext, XProcMetadata, XProcVtExpression, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils}
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.event.ReceiverOption
import net.sf.saxon.om.{AttributeInfo, AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{Axis, QName, SaxonApiException, XdmAtomicValue, XdmItem, XdmNode, XdmNodeKind, XdmValue}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.ListHasAsScala

// N.B. This looks like a step, but it isn't really. It gets passed all of the variable bindings
// and the context item and it evaluates its "options" directly. This is necessary because in
// the case where this is a default binding, it must *not* evaluate its options if the default
// is not used.

class InlineLoader() extends AbstractLoader {
  private var node: XdmNode = _
  private var encoding = Option.empty[String]
  private var exclude_inline_prefixes = Option.empty[String]
  private val excludeURIs = mutable.HashSet.empty[String]
  private var expandText = false
  private var contextProvided = false

  private val fq_inline_expand_text = TypeUtils.fqName(XProcConstants._inline_expand_text)
  private val fq_p_inline_expand_text = TypeUtils.fqName(XProcConstants.p_inline_expand_text)

  override def inputSpec: XmlPortSpecification = {
    if (contextProvided) {
      XmlPortSpecification.ANYSOURCESEQ
    } else {
      XmlPortSpecification.NONE
    }
  }
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def configure(config: XMLCalabashConfig, stepType: QName, stepName: Option[String], params: Option[ImplParams]): Unit = {
    if (params.isEmpty) {
      throw new RuntimeException("inline loader params required")
    }

    params.get match {
      case doc: InlineLoaderParams =>
        node = doc.document
        content_type = doc.content_type
        encoding = doc.encoding
        _document_properties = doc.document_properties
        exclude_inline_prefixes = doc.exclude_inline_prefixes
        expandText = doc.expand_text
        contextProvided = doc.context_provided
        exprContext = doc.context
      case _ =>
        throw new RuntimeException("document loader params wrong type")
    }
  }

  override def run(context: StaticContext): Unit = {
    if (disabled) {
      return
    }

    super.run(context)

    val propContentType = if (docProps.contains(XProcConstants._content_type)) {
      Some(MediaType.parse(docProps.get(XProcConstants._content_type).toString))
    } else {
      None
    }

    contentType = if (propContentType.isDefined) {
      if (content_type.isDefined) {
        if (!content_type.get.matches(propContentType.get)) {
          throw XProcException.xdMismatchedContentType(content_type.get, propContentType.get, exprContext.location)
        }
      }
      propContentType.get
    } else {
      if (content_type.isDefined) {
        content_type.get
      } else {
        MediaType.XML
      }
    }

    if (encoding.isDefined) {
      if (contentType.xmlContentType || contentType.htmlContentType) {
        throw XProcException.xdCannotEncodeXml(encoding.get, contentType, exprContext.location)
      }
      if (encoding.get != "base64") {
        throw XProcException.xsUnsupportedEncoding(encoding.get, exprContext.location)
      }
    }

    val props = mutable.HashMap.empty[QName, XdmValue]
    props ++= docProps
    if (!props.contains(XProcConstants._base_uri)) {
      props.put(XProcConstants._base_uri, new XdmAtomicValue(node.getBaseURI))
    }

    // If it's not an XML content type, make sure it doesn't contain any elements
    if (!contentType.xmlContentType && !contentType.htmlContentType) {
      val iter = node.axisIterator(Axis.CHILD)
      while (iter.hasNext) {
        val child = iter.next()
        child.getNodeKind match {
          case XdmNodeKind.TEXT => ()
          case _ =>
            if (encoding.isDefined) {
              throw XProcException.xdNoMarkupAllowedEncoded(child.getNodeName, exprContext.location)
            } else {
              throw XProcException.xdNoMarkupAllowed(child.getNodeName, exprContext.location)
            }
        }
      }
    }

    if (encoding.isDefined) {
      // See https://github.com/xproc/3.0-specification/issues/561
      dealWithEncodedText(contentType, props)
      return
    }

    if (contentType.xmlContentType || contentType.htmlContentType) {
      val builder = new SaxonTreeBuilder(config)
      builder.startDocument(node.getBaseURI)
      // FIXME: trim whitespace
      expandTVT(node, builder, expandText)
      builder.endDocument()
      val result = builder.result
      val metadata = new XProcMetadata(contentType, props.toMap)
      consumer.get.receive("result", result, metadata)
    } else if (contentType.jsonContentType) {
      val text = if (expandText) {
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(node.getBaseURI)
        expandTVT(node, builder, expandText)
        builder.endDocument()
        val result = builder.result
        result.getStringValue
      } else {
        node.getStringValue
      }

      val expr = new XProcXPathExpression(context, "parse-json($json)")
      val bindingsMap = mutable.HashMap.empty[String, Message]
      val vmsg = new XdmValueItemMessage(new XdmAtomicValue(text), XProcMetadata.JSON, context)
      bindingsMap.put("{}json", vmsg)
      try {
        val smsg = config.expressionEvaluator.newInstance().singletonValue(expr, List(), bindingsMap.toMap, None)
        consumer.get.receive("result", smsg.item, new XProcMetadata(contentType, props.toMap))
      } catch {
        case ex: SaxonApiException =>
          if (ex.getMessage.contains("Invalid JSON")) {
            throw XProcException.xdInvalidJson(ex.getMessage, exprContext.location)
          } else {
            throw ex
          }
        case ex: Exception =>
          throw ex
      }
    } else {
      val text = if (expandText) {
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(node.getBaseURI)
        expandTVT(node, builder, expandText)
        builder.endDocument()
        val result = builder.result
        result.getStringValue
      } else {
        node.getStringValue
      }

      if (contentType.textContentType) {
        props.put(XProcConstants._content_length, new XdmAtomicValue(text.length))

        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(node.getBaseURI)
        builder.addText(text)
        builder.endDocument()
        val result = builder.result
        consumer.get.receive("result", result, new XProcMetadata(contentType, props.toMap))
      } else {
        // FIXME: what's the right answer for unexpected content types?
        props.put(XProcConstants._content_length, new XdmAtomicValue(text.length))

        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(node.getBaseURI)
        builder.addText(text)
        builder.endDocument()
        val result = builder.result
        consumer.get.receive("result", result, new XProcMetadata(contentType, props.toMap))
      }
    }
  }

  private def dealWithEncodedText(contentType: MediaType, props: mutable.HashMap[QName, XdmValue]): Unit = {
    var str = node.getStringValue
    val decoded = Base64.getMimeDecoder.decode(str)

    val metadata = new XProcMetadata(contentType, props.toMap)

    if (contentType.xmlContentType || contentType.htmlContentType || contentType.textContentType || contentType.jsonContentType) {
      val req = new DocumentRequest(metadata.baseURI.getOrElse(new URI("")), contentType)
      val result = config.documentManager.parse(req, new ByteArrayInputStream(decoded))
      if (result.shadow.isDefined) {
        val binary = new BinaryNode(config, result.shadow)
        consumer.get.receive("result", binary, metadata)
      } else {
        result.value match {
          case node: XdmNode =>
            consumer.get.receive("result", node, metadata)
          case _ =>
            consumer.get.receive("result", result.value, metadata)
        }
      }
    } else {
      // Octet stream, I guess
      props.put(XProcConstants._content_length, new XdmAtomicValue(decoded.length))
      val binary = new BinaryNode(config, decoded)
      consumer.get.receive("result", binary, new XProcMetadata(contentType, props.toMap))
    }
  }

  private def trim(nodes: List[XdmNode]): List[XdmNode] = {
    if (config.trimInlineWhitespace) {
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

  private def expandTVT(node: XdmNode, builder: SaxonTreeBuilder, expandText: Boolean): Unit = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next()
          expandTVT(child, builder, expandText)
        }
      case XdmNodeKind.ELEMENT =>
        var nsmap = NamespaceMap.emptyMap()
        val iter = node.getUnderlyingNode.getAllNamespaces.iterator()
        while (iter.hasNext) {
          val ns = iter.next()
          if (!excludeURIs.contains(ns.getURI)) {
            val prefix = Option(ns.getPrefix).getOrElse("")
            nsmap = nsmap.put(prefix, ns.getURI)
          }
        }

        var newExpand = expandText

        var amap: AttributeMap = EmptyAttributeMap.getInstance()
        for (attr <- node.getUnderlyingNode.attributes().asList().asScala) {
          var discardAttribute = false
          if (attr.getNodeName == fq_p_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              throw XProcException.xsInlineExpandTextNotAllowed(exprContext.location)
            }
            discardAttribute = true
            newExpand = attr.getValue == "true"
          }
          if (attr.getNodeName == fq_inline_expand_text) {
            if (node.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
              discardAttribute = true
              newExpand = attr.getValue == "true"
            }
          }
          if (!discardAttribute) {
            if (expandText) {
              amap = amap.put(new AttributeInfo(attr.getNodeName, BuiltInAtomicType.UNTYPED_ATOMIC, expandString(attr.getValue), null, ReceiverOption.NONE))
            } else {
              amap = amap.put(attr)
            }
          }
        }

        builder.addStartElement(node.getNodeName, amap, nsmap)
        val citer = node.axisIterator(Axis.CHILD)
        while (citer.hasNext) {
          val child = citer.next()
          expandTVT(child, builder, newExpand)
        }
        builder.addEndElement()
      case XdmNodeKind.TEXT =>
        val str = node.getStringValue
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
    val expr = new XProcVtExpression(exprContext, text)
    var s = ""
    var string = ""
    val evaluator = config.expressionEvaluator.newInstance()
    val iter = evaluator.value(expr, contextItem.toList, msgBindings.toMap, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      string = string + s + next.getStringValue
      s = " "
    }
    string
  }

  private def expandNodes(text: String, builder: SaxonTreeBuilder): Unit = {
    val expr = new XProcVtExpression(exprContext, text)

    val evaluator = config.expressionEvaluator.newInstance()
    val iter = evaluator.value(expr, contextItem.toList, msgBindings.toMap, None).item.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      next match {
        case node: XdmNode => builder.addSubtree(node)
        case _ => builder.addText(next.getStringValue)
      }
    }
  }
}
