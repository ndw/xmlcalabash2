package com.xmlcalabash.steps

import java.io.{ByteArrayOutputStream, InputStream}
import java.net.URI
import java.util.Base64

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils}
import net.sf.saxon.s9api.{QName, Serializer, XdmNode, XdmValue}

class B64Encode extends DefaultXmlStep {
  private var source: Option[Any] = None
  private var smeta: Option[XProcMetadata] = None
  private var seropt = Map.empty[Any,Any]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    if (variable == XProcConstants._serialization) {
      seropt ++= TypeUtils.castAsScala(value).asInstanceOf[Map[Any,Any]]
    }
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = Some(item)
    smeta = Some(metadata)
  }

  override def run(context: StaticContext): Unit = {
    val baseValue = if (smeta.isDefined) {
      smeta.get.property(XProcConstants._base_uri)
    } else {
      None
    }
    val baseURI = if (baseValue.isDefined) {
      Some(new URI(baseValue.get.toString))
    } else {
      None
    }

    val encoded = source.get match {
      case is: InputStream =>
        // It seems slightly odd to me that there's no streaming API for the encoder
        val stream = new ByteArrayOutputStream()
        val buf = Array.fill[Byte](4096)(0)
        var len = is.read(buf, 0, buf.length)
        while (len >= 0) {
          stream.write(buf, 0,len)
          len = is.read(buf, 0, buf.length)
        }
        Base64.getMimeEncoder.encodeToString(stream.toByteArray)
      case node: XdmNode =>
        val stream = new ByteArrayOutputStream()
        // FIXME: get serialization parameters from serialization option
        val serializer = config.processor.newSerializer(stream)

        /*
        if (!smeta.get.contentType.xmlContentType) {
          serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
        }
        */

        S9Api.serialize(config.config, node, serializer)
        Base64.getMimeEncoder.encodeToString(stream.toByteArray)
      case _ =>
        throw new RuntimeException(s"Don't know how to encode ${source.get}")
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(baseURI)
    builder.startContent()
    builder.addText(encoded.replace("\r", ""))
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", result, new XProcMetadata(MediaType.TEXT))
  }
}