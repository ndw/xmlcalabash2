package com.xmlcalabash.util

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.config.{DeliveryAgent, XMLCalabash}
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.model.xml.XProcConstants
import com.xmlcalabash.runtime.{XProcMetadata, XmlStep}
import net.sf.saxon.s9api.XdmNode

class DefaultDeliveryAgent(config: XMLCalabash) extends DeliveryAgent {
  override def deliver(message: Message, consumer: DataConsumer, port: String): Unit = {
    consumer.receive(port, message)
    /*
    message match {
      case itemmsg: ItemMessage =>
        itemmsg.metadata match {
          case meta: XProcMetadata =>
            consumer match {
              case step: XmlStep =>
                val ok = step.inputSpec.accepts(port, meta.contentType)
                config.traceEventManager.trace("info", s"Sending ${meta.contentType} to $step on $port. Accepts: $ok", "accepts")
                if (ok) {
                  consumer.receive(port, message)
                } else {
                  if (!ok && (port.startsWith("#") || step.inputSpec.accepts(port, "application/xml"))) {
                    val xml = proxy(meta.properties)
                    consumer.receive(port, new ItemMessage(xml, meta))
                  } else {
                    throw new PipelineException("notaccepted", s"Content type not accepted: ${meta.contentType}", None)
                  }
                }
              case _ =>
                consumer.receive(port, message)
            }
          case _ =>
            consumer.receive(port, message)
        }
      case _ =>
        consumer.receive(port, message)
    }
    */
  }

  private def proxy(props: Map[String,String]): XdmNode = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_document_properties)
    builder.startContent()
    for ((key,value) <- props) {
      builder.addStartElement(XProcConstants.c_property)
      builder.addAttribute(XProcConstants._name, key)
      builder.addAttribute(XProcConstants._value, value)
      builder.startContent()
      builder.addEndElement()
    }
    builder.addEndElement()
    builder.endDocument()
    builder.result
  }
}
