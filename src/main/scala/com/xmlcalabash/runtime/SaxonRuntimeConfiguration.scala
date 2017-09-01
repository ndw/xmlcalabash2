package com.xmlcalabash.runtime

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{ExpressionEvaluator, RuntimeConfiguration}
import com.jafpl.steps.DataConsumer
import net.sf.saxon.s9api.Processor
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class SaxonRuntimeConfiguration(val processor: Processor) extends RuntimeConfiguration {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _expressionEvaluator = new SaxonExpressionEvaluator(this)
  private val enabledTraces = mutable.HashSet.empty[String]
  private val disabledTraces = mutable.HashSet.empty[String]
  private val badTraceLevels = mutable.HashSet.empty[String]

  private val prop = Option(System.getProperty("com.xmlcalabash.trace"))
  if (prop.isDefined) {
    for (trace <- prop.get.split(",").map(_.trim)) {
      var event = trace
      var enable = true

      if (trace.startsWith("-")) {
        event = trace.substring(1)
        enable = false
      } else {
        if (trace.startsWith("+")) {
          event = trace.substring(1)
        }
      }

      if (enable) {
        enabledTraces += event
      } else {
        disabledTraces += event
      }
    }
  }

  override def expressionEvaluator(): ExpressionEvaluator = _expressionEvaluator

  override def traceEnabled(trace: String): Boolean = {
    if (enabledTraces.contains("ALL")) {
      !disabledTraces.contains(trace)
    } else {
      enabledTraces.contains("ALL") || enabledTraces.contains(trace)
    }
  }

  override def watchdogTimeout: Long = 1000L

  override def deliver(message: ItemMessage, consumer: DataConsumer, port: String): Unit = {
    //println(s"Sending ${message.metadata} to $consumer")
    message.metadata match {
      case meta: XmlMetadata =>
        consumer match {
          case step: XmlStep =>
            val ok = step.inputSpec.accepts(port, meta.contentType)
            trace("debug", s"Sending ${meta.contentType} to $step on $port. Accepts: $ok", "accepts")
            consumer.receive(port, message.item, message.metadata)
          case _ =>
            consumer.receive(port, message.item, message.metadata)
        }
      case _ =>
        consumer.receive(port, message.item, message.metadata)
    }
  }

  def trace(msg: String, event: String): Unit = {
    trace("info", msg, event)
  }

  def trace(level: String, msg: String, event: String): Unit = {
    if (traceEnabled(event)) {
      level match {
        case "info" => logger.info(msg)
        case "error" => logger.error(msg)
        case "warn" => logger.warn(msg)
        case "debug" => logger.debug(msg)
        case _ =>
          if (!badTraceLevels.contains(level)) {
            logger.warn(s"Invalid trace level: $level")
            badTraceLevels += level
          }
          logger.info(msg)
      }
    }
  }
}
