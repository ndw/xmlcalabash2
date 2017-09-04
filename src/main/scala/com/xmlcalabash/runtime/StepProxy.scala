package com.xmlcalabash.runtime

import com.jafpl.exceptions.{PipelineException, StepException}
import com.jafpl.graph.Location
import com.jafpl.messages.Metadata
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, Step}
import com.xmlcalabash.model.xml.util.WithOptionData
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class StepProxy(step: Step,
                options: Map[QName, XProcExpression],
                withOptions: List[WithOptionData],
                nsBindings: Map[String,String]) extends XmlStep {
  private var location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected var config: Option[SaxonRuntimeConfiguration] = None
  protected val bindings = mutable.HashMap.empty[QName,XdmItem]
  private val cache = mutable.HashMap.empty[String, Any]

  def computeOptions(): Unit = {
    for ((name, value) <- options) {
      val result = xpathValue(value)
      step match {
        case xstep: XmlStep =>
          xstep.receiveBinding(name, result)
        case _ =>
          step.receiveBinding(name.getClarkName, result)
      }
    }

    for (data <- withOptions) {
      val expr = new XProcXPathExpression(data.nsBindings, data.select)
      val result = xpathValue(expr, cache(data.port))
      step match {
        case xstep: XmlStep =>
          xstep.receiveBinding(data.name, result)
        case _ =>
          step.receiveBinding(data.name.getClarkName, result)
      }
    }
  }

  def xpathValue(expr: XProcExpression): XdmItem = {
    val eval = config.get.expressionEvaluator().asInstanceOf[SaxonExpressionEvaluator]
    eval.withContext(this) { eval.value(expr, List.empty[Any], bindings.toMap) }
  }

  def xpathValue(expr: XProcExpression, context: Any): XdmItem = {
    val eval = config.get.expressionEvaluator().asInstanceOf[SaxonExpressionEvaluator]
    eval.withContext(this) { eval.value(expr, List(context), bindings.toMap) }
  }

  // =============================================================================================

  override def inputSpec: XmlPortSpecification = {
    step match {
      case xstep: XmlStep => xstep.inputSpec
      case _ =>
        val portMap = mutable.HashMap.empty[String,String]
        val typeMap = mutable.HashMap.empty[String,List[String]]
        for (key <- step.inputSpec.ports()) {
          portMap.put(key, step.inputSpec.cardinality(key).getOrElse("*"))
          typeMap.put(key, List("application/octet-stream"))
        }
        new XmlPortSpecification(portMap.toMap, typeMap.toMap)
    }
  }

  override def outputSpec: XmlPortSpecification = {
    step match {
      case xstep: XmlStep => xstep.outputSpec
      case _ =>
        val portMap = mutable.HashMap.empty[String,String]
        val typeMap = mutable.HashMap.empty[String,List[String]]
        for (key <- step.outputSpec.ports()) {
          portMap.put(key, step.outputSpec.cardinality(key).getOrElse("*"))
          typeMap.put(key, List("application/octet-stream"))
        }
        new XmlPortSpecification(portMap.toMap, typeMap.toMap)
    }
  }
  override def bindingSpec: BindingSpecification = step.bindingSpec
  override def setConsumer(consumer: DataConsumer): Unit = {
    this.consumer = Some(consumer)
    step.setConsumer(consumer)
  }
  override def setLocation(location: Location): Unit = {
    this.location = Some(location)
    step.setLocation(location)
  }
  override def receiveBinding(variable: String, value: Any): Unit = {
    val clarkName = "{(.*)}(.*)".r
    val qname = variable match {
      case clarkName(uri,name) => new QName(uri,name)
      case _ => throw new PipelineException("badname", "Name isn't a Clark name", None)
    }
    // FIXME: deal with other types
    val xvalue = new XdmAtomicValue(value.toString)
    receiveBinding(qname, xvalue)
  }
  override def receiveBinding(variable: QName, value: XdmItem): Unit = {
    bindings.put(variable, value)
  }
  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case saxon: SaxonRuntimeConfiguration =>
        this.config = Some(saxon)
      case _ => throw new StepException("badconfig", "Supplied configuration is unusable")
    }
    step.initialize(config)
  }
  override def run(): Unit = {
    computeOptions()
    step.run()
  }
  override def reset(): Unit = {
    step.reset()
  }
  override def abort(): Unit = {
    step.abort()
  }
  override def stop(): Unit = {
    step.stop()
  }
  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    //println(s"RECV $port: $item")
    if (port.startsWith("#")) {
      if (cache.contains(port)) {
        throw new PipelineException("badcontext", s"A sequence is not allowed: $port", None)
      }
      cache.put(port, item)
    }
    step.receive(port, item, metadata)
  }
}
