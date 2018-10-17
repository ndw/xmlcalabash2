package com.xmlcalabash.steps

import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Base64

import com.jafpl.messages.Message
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, ValueUtils}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode, XdmValue}

import scala.collection.mutable

class CastContentType() extends DefaultXmlStep {
  private var item = Option.empty[Any]
  private var metadata = Option.empty[XProcMetadata]
  private var castTo = MediaType.OCTET_STREAM

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    this.item = Some(item)
    this.metadata = Some(metadata)
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    if (variable == XProcConstants._content_type) {
      castTo = MediaType.parse(ValueUtils.singletonStringValue(value, context.location))
    }
  }

  override def run(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    if (castTo.xmlContentType) {
      castToXML(context)
    } else if (castTo.textContentType) {
      castToText(context)
    } else {
      throw XProcException.stepError(1003)
    }
  }

  def castToXML(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    if (contentType.xmlContentType) {
      consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))
    } else if (castTo.textContentType) {
      val builder = new SaxonTreeBuilder(config)

      val baseURI = if (metadata.get.properties.contains(XProcConstants._base_uri)) {
        Some(new URI(S9Api.valuesToString(metadata.get.properties(XProcConstants._base_uri))))
      } else {
        None
      }

      builder.startDocument(baseURI)
      builder.addStartElement(XProcConstants.c_data)
      builder.addAttribute(XProcConstants._content_type, contentType.toString)
      builder.startContent()
      builder.addText(item.get.asInstanceOf[XdmNode].getStringValue)
      builder.addEndElement()
      builder.endDocument()
      consumer.get.receive("result", builder.result, new XProcMetadata(castTo, metadata.get.properties))
    } else if (contentType.jsonContentType) {
      // Step 1, convert the map into a JSON text string
      var expr = new XProcXPathExpression(ExpressionContext.NONE, "serialize($map, map {\"method\": \"json\"})")
      val bindingsMap = mutable.HashMap.empty[String, Message]
      var vmsg = new XPathItemMessage(item.get.asInstanceOf[XdmItem], XProcMetadata.XML, ExpressionContext.NONE)
      bindingsMap.put("{}map", vmsg)
      var smsg = config.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)

      // Step 2, convert the JSON to XML
      expr = new XProcXPathExpression(ExpressionContext.NONE, "json-to-xml($json)")
      bindingsMap.clear()
      vmsg = new XPathItemMessage(smsg.item, XProcMetadata.XML, ExpressionContext.NONE)
      bindingsMap.put("{}json", vmsg)
      smsg = config.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)

      consumer.get.receive("result", smsg.item, new XProcMetadata(castTo, metadata.get.properties))
    } else {
      val builder = new SaxonTreeBuilder(config)

      val baseURI = if (metadata.get.properties.contains(XProcConstants._base_uri)) {
        Some(new URI(S9Api.valuesToString(metadata.get.properties(XProcConstants._base_uri))))
      } else {
        None
      }

      builder.startDocument(baseURI)
      builder.addStartElement(XProcConstants.c_data)
      builder.addAttribute(XProcConstants._content_type, contentType.toString)
      builder.addAttribute(XProcConstants._encoding, "base64")
      builder.startContent()

      // The string may contain CRLF line endings, remove the CRs
      val base64str = Base64.getMimeEncoder.encodeToString(item.get.asInstanceOf[Array[Byte]]).replace("\r", "")
      builder.addText(base64str)

      builder.addEndElement()
      builder.endDocument()

      val doc = builder.result
      consumer.get.receive("result", doc, new XProcMetadata(castTo, metadata.get.properties))
    }
  }

  def castToText(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    if (contentType.xmlContentType) {
      val baos = new ByteArrayOutputStream()
      val serializer = config.config.processor.newSerializer(baos)
      S9Api.serialize(config.config, item.get.asInstanceOf[XdmNode], serializer)

      val builder = new SaxonTreeBuilder(config)

      val baseURI = if (metadata.get.properties.contains(XProcConstants._base_uri)) {
        Some(new URI(S9Api.valuesToString(metadata.get.properties(XProcConstants._base_uri))))
      } else {
        None
      }

      builder.startDocument(baseURI)
      builder.addText(baos.toString)
      builder.addEndElement()
      builder.endDocument()
      consumer.get.receive("result", builder.result, new XProcMetadata(castTo, metadata.get.properties))
    } else if (castTo.textContentType) {
      consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))
    } else {
      // FIXME: Must handle the c:data case...
      throw XProcException.stepError(1003)
    }
  }

  def castToJSON(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType
    throw new UnsupportedOperationException("Casting to JSON hasn't been implemented")
  }

}
