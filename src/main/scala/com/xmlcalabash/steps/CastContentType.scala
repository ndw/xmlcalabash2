package com.xmlcalabash.steps

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.URI
import java.util.Base64

import com.jafpl.messages.Message
import com.xmlcalabash.config.DocumentRequest
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, S9Api, TypeUtils, ValueUtils}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode, XdmValue}
import org.apache.http.util.ByteArrayBuffer

import scala.collection.mutable

class CastContentType() extends DefaultXmlStep {
  private var item = Option.empty[Any]
  private var metadata = Option.empty[XProcMetadata]
  private var castTo = MediaType.OCTET_STREAM
  private var parameters = Option.empty[XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    this.item = Some(item)
    this.metadata = Some(metadata)
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    variable match {
      case XProcConstants._content_type =>
        castTo = MediaType.parse(ValueUtils.singletonStringValue(value, context.location))
      case XProcConstants._parameters =>
        parameters = Some(value)
      case _ => Unit
    }
  }

  override def run(context: StaticContext): Unit = {
    if (castTo.xmlContentType) {
      castToXML(context)
    } else if (castTo.jsonContentType) {
      castToJSON(context)
    } else if (castTo.htmlContentType) {
      castToHTML(context)
    } else if (castTo.textContentType) {
      castToText(context)
    } else {
      throw new RuntimeException("impossilbe content type cast")
    }
  }

  def castToXML(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    contentType.classification match {
      case MediaType.XML =>
        consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))
      case MediaType.HTML =>
        consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))
      case MediaType.TEXT =>
        val text = item.asInstanceOf[XdmNode].getStringValue
        val bais = new ByteArrayInputStream(text.getBytes("UTF-8"))
        val baseURI = metadata.get.baseURI.getOrElse(new URI(""))
        val req = new DocumentRequest(baseURI, contentType)
        val resp = config.documentManager.parse(req, bais)
        consumer.get.receive("result", resp.value, new XProcMetadata(castTo, metadata.get.properties))
      case MediaType.JSON =>
        // Step 1, convert the map into a JSON text string
        var expr = new XProcXPathExpression(context, "serialize($map, map {\"method\": \"json\"})")
        val bindingsMap = mutable.HashMap.empty[String, Message]
        var vmsg = new XdmValueItemMessage(item.get.asInstanceOf[XdmItem], XProcMetadata.XML, context)
        bindingsMap.put("{}map", vmsg)
        var smsg = config.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)

        // Step 2, convert the JSON to XML
        expr = new XProcXPathExpression(context, "json-to-xml($json)")
        bindingsMap.clear()
        vmsg = new XdmValueItemMessage(smsg.item, XProcMetadata.XML, context)
        bindingsMap.put("{}json", vmsg)
        smsg = config.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)

        consumer.get.receive("result", smsg.item, new XProcMetadata(castTo, metadata.get.properties))
      case MediaType.OCTET_STREAM =>
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

        // A binary should be an input stream...
        item.get match {
          case binary: BinaryNode =>
            val is = binary.stream
            val bos = new ByteArrayOutputStream()
            var totBytes = 0L
            val pagesize = 4096
            val tmp = new Array[Byte](4096)
            var length = 0
            length = is.read(tmp)
            while (length >= 0) {
              bos.write(tmp, 0, length)
              totBytes += length
              length = is.read(tmp)
            }
            // The string may contain CRLF line endings, remove the CRs
            val base64str = Base64.getMimeEncoder.encodeToString(bos.toByteArray).replace("\r", "")
            builder.addText(base64str)
          case _ =>
            throw XProcException.xiUnexpectedItem(item.get.toString, location)
        }

        builder.addEndElement()
        builder.endDocument()

        val doc = builder.result
        consumer.get.receive("result", doc, new XProcMetadata(castTo, metadata.get.properties))
    }
  }

  def castToText(context: StaticContext): Unit = {
    val contentType = metadata.get.contentType

    contentType.classification match {
      case MediaType.TEXT =>
        consumer.get.receive("result", item.get, new XProcMetadata(castTo, metadata.get.properties))

      case MediaType.XML =>
        serializeNodes(item.asInstanceOf[XdmNode], contentType)

      case MediaType.HTML =>
        serializeNodes(item.asInstanceOf[XdmNode], contentType)

      case MediaType.JSON =>
        var expr = new XProcXPathExpression(context, "serialize($map, map {\"method\": \"json\"})")
        val bindingsMap = mutable.HashMap.empty[String, Message]
        var vmsg = new XdmValueItemMessage(item.get.asInstanceOf[XdmValue], XProcMetadata.XML, context)
        bindingsMap.put("{}map", vmsg)
        var smsg = config.expressionEvaluator.singletonValue(expr, List(), bindingsMap.toMap, None)

        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(metadata.get.baseURI)
        builder.addText(smsg.item.toString)
        builder.endDocument()
        consumer.get.receive("result", builder.result, new XProcMetadata(castTo, metadata.get.properties))

      case MediaType.OCTET_STREAM =>
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(metadata.get.baseURI)
        builder.addStartElement(XProcConstants.c_data)
        builder.addAttribute(XProcConstants._content_type, contentType.toString)
        builder.addAttribute(XProcConstants._encoding, "base64")
        builder.startContent()

        val stream = item.get.asInstanceOf[BinaryNode].stream
        val bos = new ByteArrayOutputStream()
        var totBytes = 0L
        val pagesize = 4096
        val tmp = new Array[Byte](4096)
        var length = 0
        length = stream.read(tmp)
        while (length >= 0) {
          bos.write(tmp, 0, length)
          totBytes += length
          length = stream.read(tmp)
        }
        bos.close()
        stream.close()

        // The string may contain CRLF line endings, remove the CRs
        val base64str = Base64.getMimeEncoder.encodeToString(bos.toByteArray).replace("\r", "")
        builder.addText(base64str)

        builder.addEndElement()
        builder.endDocument()

        val doc = builder.result
        consumer.get.receive("result", doc, new XProcMetadata(castTo, metadata.get.properties))
    }
  }

  private def serializeNodes(item: XdmNode, contentType: MediaType): Unit = {
    val serialOpts = mutable.HashMap.empty[QName, String]
    if (parameters.isDefined) {
      val opts = TypeUtils.castAsScala(parameters.get).asInstanceOf[Map[Any, Any]]
      for (opt <- opts.keySet) {
        opt match {
          case name: QName =>
            serialOpts.put(name, opt.toString)
          case name: String =>
            if (!name.contains(":")) {
              serialOpts.put(new QName(name), opt.toString)
            }
        }
      }
    }

    val stream = new ByteArrayOutputStream()
    val serializer = config.processor.newSerializer(stream)

    S9Api.configureSerializer(serializer, config.defaultSerializationOptions(contentType))
    S9Api.configureSerializer(serializer, serialOpts.toMap)

    S9Api.serialize(config.config, item, serializer)

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(metadata.get.baseURI)
    builder.addText(stream.toString("UTF-8"))
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(castTo, metadata.get.properties))
  }

  def castToJSON(context: StaticContext): Unit = {
    throw new UnsupportedOperationException("Casting to JSON hasn't been implemented")
  }

  def castToHTML(context: StaticContext): Unit = {
    throw new UnsupportedOperationException("Casting to HTML hasn't been implemented")
  }
}
