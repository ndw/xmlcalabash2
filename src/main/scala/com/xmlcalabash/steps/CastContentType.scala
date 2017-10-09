package com.xmlcalabash.steps

import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Base64

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode}

class CastContentType() extends DefaultXmlStep {
  private var item = Option.empty[Any]
  private var metadata = Option.empty[XProcMetadata]
  private var castTo = "application/octet-stream"

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    this.item = Some(item)
    this.metadata = Some(metadata)
  }

  override def receiveBinding(variable: QName, value: XdmItem, context: ExpressionContext): Unit = {
    if (variable == XProcConstants._content_type) {
      castTo = value.getStringValue
    }
  }

  override def run(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    if (ValueParser.xmlContentType(castTo)) {
      if (ValueParser.xmlContentType(contentType)) {
        consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))
      } else if (ValueParser.textContentType(castTo)) {
        val builder = new SaxonTreeBuilder(config)

        val baseURI = if (metadata.get.properties.contains(XProcConstants._base_uri)) {
          Some(new URI(metadata.get.properties(XProcConstants._base_uri).getStringValue))
        } else {
          None
        }

        builder.startDocument(baseURI)
        builder.addStartElement(XProcConstants.c_data)
        builder.addAttribute(XProcConstants._content_type, contentType)
        builder.startContent()
        builder.addText(item.get.asInstanceOf[XdmNode].getStringValue)
        builder.addEndElement()
        builder.endDocument()
        consumer.get.receive("result", builder.result, new XProcMetadata(castTo, metadata.get.properties))
      } else {
        val builder = new SaxonTreeBuilder(config)

        val baseURI = if (metadata.get.properties.contains(XProcConstants._base_uri)) {
          Some(new URI(metadata.get.properties(XProcConstants._base_uri).getStringValue))
        } else {
          None
        }

        builder.startDocument(baseURI)
        builder.addStartElement(XProcConstants.c_data)
        builder.addAttribute(XProcConstants._content_type, contentType)
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
    } else if (ValueParser.textContentType(castTo)) {
      if (ValueParser.xmlContentType(contentType)) {
        val baos = new ByteArrayOutputStream()
        val serializer = config.processor.newSerializer(baos)
        S9Api.serialize(config, item.get.asInstanceOf[XdmNode], serializer)

        val builder = new SaxonTreeBuilder(config)

        val baseURI = if (metadata.get.properties.contains(XProcConstants._base_uri)) {
          Some(new URI(metadata.get.properties(XProcConstants._base_uri).getStringValue))
        } else {
          None
        }

        builder.startDocument(baseURI)
        builder.addText(baos.toString)
        builder.addEndElement()
        builder.endDocument()
        consumer.get.receive("result", builder.result, new XProcMetadata(castTo, metadata.get.properties))
      } else if (ValueParser.textContentType(castTo)) {
        consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))
      } else {
        // FIXME: Must handle the c:data case...
        throw XProcException.stepError(1003)
      }
    } else {
      throw XProcException.stepError(1003)
    }
  }
}
