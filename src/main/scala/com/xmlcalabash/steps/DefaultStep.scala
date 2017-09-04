package com.xmlcalabash.steps

import com.jafpl.exceptions.{PipelineException, StepException}
import com.jafpl.messages.Metadata
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortSpecification, Step}
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, SaxonRuntimeConfiguration, XProcExpression, XmlPortSpecification, XmlStep}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultStep extends XmlStep {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected var config: Option[SaxonRuntimeConfiguration] = None
  protected val bindings = mutable.HashMap.empty[QName,XdmItem]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: String, value: Any): Unit = {
    if (variable.startsWith("{")) {
      val clarkName = "\\{(.*)\\}(.*)".r
      val qname = variable match {
        case clarkName(uri,name) => new QName(uri,name)
        case _ => throw new PipelineException("badname", s"Name isn't a Clark name: $variable", None)
      }
      // FIXME: deal with other types
      val xvalue = new XdmAtomicValue(value.toString)
      receiveBinding(qname, xvalue)
    } else {
      // FIXME: deal with other types
      val xvalue = new XdmAtomicValue(value.toString)
      receiveBinding(new QName("", variable), xvalue)
    }
  }

  override def receiveBinding(variable: QName, value: XdmItem): Unit = {
    bindings.put(variable, value)
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
