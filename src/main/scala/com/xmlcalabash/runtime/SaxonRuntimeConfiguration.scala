package com.xmlcalabash.runtime

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{ExpressionEvaluator, RuntimeConfiguration}
import com.jafpl.steps.DataConsumer
import net.sf.saxon.s9api.Processor

class SaxonRuntimeConfiguration(val processor: Processor) extends RuntimeConfiguration {
  private val _expressionEvaluator = new SaxonExpressionEvaluator(this)

  override def expressionEvaluator(): ExpressionEvaluator = _expressionEvaluator

  override def traceEnabled(trace: String): Boolean = {
    false
  }

  override def watchdogTimeout: Long = 1000L

  override def deliver(message: ItemMessage, consumer: DataConsumer, port: String): Unit = {
    //println(s"Sending ${message.metadata} to $consumer")
    message.metadata match {
      case meta: XmlMetadata =>
        consumer match {
          case step: XmlStep =>
            val ok = step.inputSpec.accepts(port, meta.contentType)
            println(s"Sending ${meta.contentType} to $step on $port. Accepts: $ok")
            consumer.receive(port, message.item, message.metadata)
          case _ =>
            consumer.receive(port, message.item, message.metadata)
        }
      case _ =>
        consumer.receive(port, message.item, message.metadata)
    }
  }
}
