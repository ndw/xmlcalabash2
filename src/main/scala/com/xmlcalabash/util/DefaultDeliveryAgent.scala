package com.xmlcalabash.util

import com.jafpl.messages.ItemMessage
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.config.{DeliveryAgent, XMLCalabash}
import com.xmlcalabash.runtime.{XmlMetadata, XmlStep}

class DefaultDeliveryAgent(config: XMLCalabash) extends DeliveryAgent {
  override def deliver(message: ItemMessage, consumer: DataConsumer, port: String): Unit = {
    //println(s"Sending ${message.metadata} to $consumer")
    message.metadata match {
      case meta: XmlMetadata =>
        consumer match {
          case step: XmlStep =>
            val ok = step.inputSpec.accepts(port, meta.contentType)
            config.traceEventManager.trace("debug", s"Sending ${meta.contentType} to $step on $port. Accepts: $ok", "accepts")
            consumer.receive(port, message.item, message.metadata)
          case _ =>
            consumer.receive(port, message.item, message.metadata)
        }
      case _ =>
        consumer.receive(port, message.item, message.metadata)
    }
  }

}
