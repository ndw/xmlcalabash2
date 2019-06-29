package com.xmlcalabash.steps

import java.io.{File, FileOutputStream, InputStream}
import java.net.URI
import java.util.Base64

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.s9api.{QName, Serializer, XdmMap, XdmNode}

import scala.collection.mutable

class Store extends DefaultXmlStep {
  private var source: Option[Any] = None
  private var smeta: Option[XProcMetadata] = None

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = Some(item)
    smeta = Some(metadata)
  }

  override def run(context: StaticContext): Unit = {
    val href = if (bindings.contains(XProcConstants._href)) {
      val _href = bindings(XProcConstants._href).getStringValue
      if (context.baseURI.isDefined) {
        context.baseURI.get.resolve(_href)
      } else {
        new URI(_href)
      }
    } else {
      // This can't actually happen, but ...
      throw XProcException.xsMissingRequiredOption(XProcConstants._href, location)
    }

    if (href.getScheme != "file") {
      throw XProcException.xcCannotStore(href, context.location)
    }

    val os = new FileOutputStream(href.getPath)
    source.get match {
      case is: InputStream =>
        val bytes = new Array[Byte](8192)
        var count = is.read(bytes)
        while (count >= 0) {
          os.write(bytes, 0, count)
          count = is.read(bytes)
        }
      case node: XdmNode =>
        // FIXME: get serialization parameters from serialization option
        val serialOpts = mutable.HashMap.empty[QName,String]
        if (bindings.contains(XProcConstants._serialization)) {
          val bs = bindings(XProcConstants._serialization).value
          val bx = S9Api.forceQNameKeys(bs.getUnderlyingValue.asInstanceOf[MapItem])
          val iter = bx.keySet.iterator()
          while (iter.hasNext) {
            val key = iter.next()
            serialOpts.put(key.getQNameValue, bx.get(key).toString)
          }
        }

        val serializer = config.processor.newSerializer(os)

        val contentType = smeta.get.contentType
        if (!contentType.xmlContentType && !contentType.htmlContentType) {
          serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes")
        }

        S9Api.configureSerializer(serializer, config.defaultSerializationOptions(contentType))
        S9Api.configureSerializer(serializer, serialOpts.toMap)

        S9Api.serialize(config.config, node, serializer)
      case _ =>
        throw XProcException.xiUnexpectedItem(source.get.toString, context.location)
    }
    os.close()

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(smeta.get.baseURI)
    builder.startContent()
    builder.addStartElement(XProcConstants.c_result)
    builder.startContent()
    builder.addText(href.toASCIIString)
    builder.addEndElement()
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", result, new XProcMetadata(MediaType.XML))
  }
}