package com.xmlcalabash.steps

import java.io.{FileOutputStream, InputStream}
import java.net.URI

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.{Serializer, XdmNode}

class Store extends DefaultXmlStep {
  private var source: Any = _
  private var smeta: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.EXACTLY_ONE,
      "result-uri" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("*/*"),
      "result-uri" -> List("application/xml")))

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item
    smeta = metadata
  }

  override def run(context: StaticContext): Unit = {
    val href = if (context.baseURI.isDefined) {
      context.baseURI.get.resolve(stringBinding(XProcConstants._href))
    } else {
      new URI(stringBinding(XProcConstants._href))
    }

    if (href.getScheme != "file") {
      throw XProcException.xcCannotStore(href, context.location)
    }

    val os = new FileOutputStream(href.getPath)
    source match {
      case is: InputStream =>
        val bytes = new Array[Byte](8192)
        var count = is.read(bytes)
        while (count >= 0) {
          os.write(bytes, 0, count)
          count = is.read(bytes)
        }
      case node: XdmNode =>
        val serialOpts = serializationOptions()
        val serializer = config.processor.newSerializer(os)

        val contentType = smeta.contentType
        if (!contentType.xmlContentType && !contentType.htmlContentType) {
          serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
        }

        S9Api.configureSerializer(serializer, config.defaultSerializationOptions(contentType))
        S9Api.configureSerializer(serializer, serialOpts)

        S9Api.serialize(config.config, node, serializer)
      case _ =>
        throw XProcException.xiUnexpectedItem(source.toString, context.location)
    }
    os.close()

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(smeta.baseURI)
    builder.startContent()
    builder.addStartElement(XProcConstants.c_result)
    builder.startContent()
    builder.addText(href.toASCIIString)
    builder.addEndElement()
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", source, smeta)
    consumer.get.receive("result-uri", result, new XProcMetadata(MediaType.XML))
  }
}