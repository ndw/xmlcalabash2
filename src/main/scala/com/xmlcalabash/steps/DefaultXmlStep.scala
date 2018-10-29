package com.xmlcalabash.steps

import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.{ExpressionContext, ImplParams, StaticContext, XMLCalabashRuntime, XProcDataConsumer, XProcMetadata, XmlPortSpecification, XmlStep}
import com.xmlcalabash.util.XProcVarValue
import net.sf.saxon.s9api.{QName, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultXmlStep extends XmlStep {
  private var _location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[XProcDataConsumer] = None
  protected var config: XMLCalabashRuntime = _
  protected val bindings = mutable.HashMap.empty[QName,XProcVarValue]

  def location: Option[Location] = _location

  // ==========================================================================

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    bindings.put(variable, new XProcVarValue(value, context))
  }

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    // nop
  }

  override def initialize(config: RuntimeConfiguration, params: Option[ImplParams]): Unit = {
    config match {
      case xmlCalabash: XMLCalabashRuntime =>
        this.config = xmlCalabash
      case _ => throw XProcException.xiNotXMLCalabash()
    }
  }

  override def run(context: StaticContext): Unit = {
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

  def stringBinding(name: QName): Option[String] = {
    if (bindings.contains(name)) {
      Some(bindings(name).getStringValue)
    } else {
      None
    }
  }

  def booleanBinding(name: QName): Option[Boolean] = {
    if (bindings.contains(name)) {
      Some(bindings(name).getStringValue == "true")
    } else {
      None
    }
  }

  def integerBinding(name: QName): Option[Integer] = {
    if (bindings.contains(name)) {
      Some(bindings(name).getStringValue.toInt)
    } else {
      None
    }
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
