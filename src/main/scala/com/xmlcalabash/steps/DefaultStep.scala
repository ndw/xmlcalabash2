package com.xmlcalabash.steps

import com.jafpl.exceptions.{PipelineException, StepException}
import com.jafpl.graph.Location
import com.jafpl.messages.Metadata
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortSpecification, Step}
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, SaxonRuntimeConfiguration, XProcExpression, XmlPortSpecification, XmlStep}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultStep extends XmlStep {
  private var _location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected var config: Option[SaxonRuntimeConfiguration] = None
  protected val bindings = mutable.HashMap.empty[QName,XdmItem]

  def location: Option[Location] = _location

  def dynamicError(code: Int): Unit = {
    throw new PipelineException("notimpl", "dynamic error isn't implemented yet", location)
  }

  def lexicalQName(name: String, bindings: Map[String,String]): QName = {
    if (name.contains(":")) {
      val pos = name.indexOf(":")
      val pfx = name.substring(0, pos)
      val lcl = name.substring(pos+1)
      if (bindings.contains(pfx)) {
        new QName(pfx, bindings(pfx), lcl)
      } else {
        throw new PipelineException("badqname", "Unparsable qname: " + name, None)
      }
    } else {
      new QName("", name)
    }
  }

  // ==========================================================================

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }
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
      receiveBinding(qname, xvalue, Map.empty[String,String])
    } else {
      // FIXME: deal with other types
      val xvalue = new XdmAtomicValue(value.toString)
      receiveBinding(new QName("", variable), xvalue, Map.empty[String,String])
    }
  }

  override def receiveBinding(variable: QName, value: XdmItem, nsBindings: Map[String,String]): Unit = {
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
}
