package com.xmlcalabash.steps

import com.jafpl.exceptions.{PipelineException, StepException}
import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.runtime.{XProcDataConsumer, XProcMetadata, XmlPortSpecification, XmlStep}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap, XdmValue}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultXmlStep extends XmlStep {
  private var _location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[XProcDataConsumer] = None
  protected var config: XMLCalabash = _
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

  def parseParameters(value: XdmItem, nsBindings: Map[String,String]): Map[QName, XdmValue] = {
    val params = mutable.HashMap.empty[QName, XdmValue]

    value match {
      case map: XdmMap =>
        // Grovel through a Java Map
        val iter = map.keySet().iterator()
        while (iter.hasNext) {
          val key = iter.next()
          val value = map.get(key)

          val qname = lexicalQName(key.getStringValue, nsBindings)
          params.put(qname, value)
        }
      case _ =>
        throw new PipelineException("notmap", "The parameters must be a map", None)
    }

    params.toMap
  }

  def parseDocumentProperties(value: XdmItem): Map[String, String] = {
    val params = mutable.HashMap.empty[String, String]

    value match {
      case map: XdmMap =>
        // Grovel through a Java Map
        val iter = map.keySet().iterator()
        while (iter.hasNext) {
          val key = iter.next()
          val value = map.get(key)

          var strvalue = ""
          val viter = value.iterator()
          while (viter.hasNext) {
            val item = viter.next()
            strvalue += item.getStringValue
          }

          params.put(key.asInstanceOf[XdmAtomicValue].getStringValue, strvalue)
        }
      case _ =>
        throw new PipelineException("notmap", "The document properties must be a map", None)
    }

    params.toMap
  }

  // ==========================================================================

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: QName, value: XdmItem, nsBindings: Map[String,String]): Unit = {
    bindings.put(variable, value)
  }

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    // nop
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case xmlCalabash: XMLCalabash =>
        this.config = xmlCalabash
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
