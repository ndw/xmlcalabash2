package com.xmlcalabash.steps

import com.jafpl.exceptions.StepException
import com.jafpl.messages.Metadata
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortSpecification, Step}
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, SaxonRuntimeConfiguration, XProcExpression, XmlPortSpecification, XmlStep}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultStep extends XmlStep {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected var config: Option[SaxonRuntimeConfiguration] = None
  protected val bindings = mutable.HashMap.empty[String,Any]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: String, value: Any): Unit = {
    // nop
  }

  override def setConsumer(consumer: DataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    // nop
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case saxon: SaxonRuntimeConfiguration =>
        this.config = Some(saxon)
      case _ => throw new StepException("badconfig", "Supplied configuration is unusable")
    }
  }

  override def run(): Unit = {
    // nop
  }

  override def reset(): Unit = {
    // nop
  }

  override def abort(): Unit = {
    // nop
  }

  override def stop(): Unit = {
    // nop
  }

  override def toString: String = {
    val defStr = super.toString
    if (defStr.startsWith("com.xmlcalabash.steps")) {
      val objstr = ".*\\.([^\\.]+)@[0-9a-f]+$".r
      defStr match {
        case objstr(name) => name
        case _ => defStr

      }
    } else {
      defStr
    }
  }

  def xpathValue(expr: XProcExpression): Any = {
    val eval = config.get.expressionEvaluator().asInstanceOf[SaxonExpressionEvaluator]
    eval.withContext(this) { eval.value(expr, List.empty[Any], bindings.toMap) }
  }

  def xpathValue(expr: XProcExpression, context: Any): Any = {
    val eval = config.get.expressionEvaluator().asInstanceOf[SaxonExpressionEvaluator]
    eval.withContext(this) { eval.value(expr, List(context), bindings.toMap) }
  }
}
