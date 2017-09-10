package com.xmlcalabash.runtime

import com.jafpl.exceptions.{PipelineException, StepException}
import com.jafpl.graph.Location
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, Step}
import com.xmlcalabash.config.XMLCalabash
import net.sf.saxon.s9api.{QName, XdmItem}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class StepProxy(step: XmlStep) extends Step with XProcDataConsumer {
  private var location = Option.empty[Location]
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected var consumer: Option[DataConsumer] = None
  protected var config: Option[XMLCalabash] = None
  protected val bindings = mutable.HashMap.empty[QName,XdmItem]

  // =============================================================================================

  override def toString: String = {
    "proxy:" + step.toString
  }

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
    step.setConsumer(this)
  }
  override def setLocation(location: Location): Unit = {
    this.location = Some(location)
    step.setLocation(location)
  }
  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    val qname = if (bindmsg.name.startsWith("{")) {
      val clarkName = "\\{(.*)\\}(.*)".r
      val qname = bindmsg.name match {
        case clarkName(uri,name) => new QName(uri,name)
        case _ => throw new PipelineException("badname", s"Name isn't a Clark name: ${bindmsg.name}", None)
      }
      qname
    } else {
      new QName("", bindmsg.name)
    }

    bindmsg.message match {
      case item: ItemMessage =>
        step.receiveBinding(qname, item.item.asInstanceOf[XdmItem], Map.empty[String,String])
      case _ =>
        throw new PipelineException("unkmsg", "Unexpected binding message: " + bindmsg.message, None)
    }
  }
  override def initialize(config: RuntimeConfiguration): Unit = {
    config match {
      case saxon: XMLCalabash =>
        this.config = Some(saxon)
      case _ => throw new StepException("badconfig", "Supplied configuration is unusable")
    }
    step.initialize(config)
  }
  override def run(): Unit = {
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
  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        item.metadata match {
          case xmlmeta: XProcMetadata =>
            step.receive(port, item.item, xmlmeta)
          case _ => throw new PipelineException("badmeta", "Unexpected metadata: " + item.metadata, None)
        }
      case _ => throw new PipelineException("badmsg", "Unexpected message: " + message, None)
    }
  }

  // =======================================================================================

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    consumer.get.receive(port, new ItemMessage(item, metadata))
  }
}
