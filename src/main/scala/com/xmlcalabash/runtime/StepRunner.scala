package com.xmlcalabash.runtime
import com.jafpl.graph.Location
import com.jafpl.messages.{ExceptionMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortCardinality}
import com.xmlcalabash.config.{StepSignature, XMLCalabashConfig}
import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.messages.{AnyItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.xml.DeclareStep
import com.xmlcalabash.util.XProcVarValue
import net.sf.saxon.s9api.{QName, XdmItem, XdmValue}

import scala.collection.mutable

class StepRunner(private val pruntime: XMLCalabashConfig, val decl: DeclareStep, val signature: StepSignature) extends StepExecutable {
  private var runtime: XMLCalabashRuntime = _
  private var _location = Option.empty[Location]
  private val consumers = mutable.HashMap.empty[String, ConsumerMap]

  private val cardMap = mutable.HashMap.empty[String,PortCardinality]
  private val typeMap = mutable.HashMap.empty[String,List[String]]
  for (port <- signature.inputPorts) {
    val portSig = signature.input(port, decl.location.get)
    portSig.cardinality match {
      case "1" => cardMap.put(portSig.port, new PortCardinality(1,1))
      case "*" => cardMap.put(portSig.port, new PortCardinality(0))
      case "+" => cardMap.put(portSig.port, new PortCardinality(1))
      case _ => throw new RuntimeException("WTF? Cardinality=" + portSig.cardinality)
    }
    typeMap.put(portSig.port, List("application/octet-stream")) // FIXME: THIS IS A LIE
  }

  private val iSpec = new XmlPortSpecification(cardMap.toMap, typeMap.toMap)
  cardMap.clear()
  typeMap.clear()
  for (port <- signature.outputPorts) {
    val portSig = signature.output(port, decl.location.get)
    portSig.cardinality match {
      case "1" => cardMap.put(portSig.port, new PortCardinality(1,1))
      case "*" => cardMap.put(portSig.port, new PortCardinality(0))
      case "+" => cardMap.put(portSig.port, new PortCardinality(1))
      case _ => throw new RuntimeException("WTF? Cardinality=" + portSig.cardinality)
    }
    typeMap.put(portSig.port, List("application/octet-stream")) // FIXME: THIS IS A LIE
  }
  private val oSpec = new XmlPortSpecification(cardMap.toMap, typeMap.toMap)

  override def inputSpec: XmlPortSpecification = iSpec

  override def outputSpec: XmlPortSpecification = oSpec

  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    // It's too early to set in the runtime, save for later
    for (port <- signature.outputPorts) {
      consumers.put(port, new ConsumerMap(port, consumer))
    }
  }

  override def setLocation(location: Location): Unit = {
    _location = Some(location)
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    runtime.option(variable, new XProcVarValue(value, new StaticContext(context)))
  }

  // Input to the pipeline
  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case value: XdmItem => runtime.input(port, value, metadata)
      case _ => throw new RuntimeException("Unexpected value sent to StepRunner")
    }
  }

  override def configure(config: XMLCalabashConfig, params: Option[ImplParams]): Unit = {
    // nop
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    //decl.dump()
    runtime = decl.runtime()
    for ((port, consumer) <- consumers) {
      runtime.output(port, consumer)
    }
  }

  override def run(context: StaticContext): Unit = {
    runtime.run()
  }

  override def reset(): Unit = {
    runtime.reset()
  }

  override def abort(): Unit = {
    throw new RuntimeException("Don't know how to abort a StepRunner")
  }

  override def stop(): Unit = {
    runtime.stop()
  }

  class ConsumerMap(val result_port: String, val consumer: XProcDataConsumer) extends DataConsumer {
    override def receive(port: String, message: Message): Unit = {
      // The data consumer always receives input on its "source" port. We have to construct
      // this consumer so that it knows what output port to deliver to.

      //println(s"RECEIVE: $result_port: $message")

      // Get exceptions out of the way
      message match {
        case msg: ExceptionMessage =>
          msg.item match {
            case ex: StepException =>
              if (ex.errors.isDefined) {
                consumer.receive(result_port, ex.errors.get, XProcMetadata.XML)
              } else {
                consumer.receive(result_port, msg.item, XProcMetadata.EXCEPTION)
              }
            case _ =>
              consumer.receive(result_port, msg.item, XProcMetadata.EXCEPTION)
          }
          return
        case _ => Unit
      }

      message match {
        case msg: XdmValueItemMessage =>
          consumer.receive(result_port, msg.item, msg.metadata)
        case msg: AnyItemMessage =>
          consumer.receive(result_port, msg.shadow, msg.metadata)
        case _ =>
          throw XProcException.xiInvalidMessage(None, message)
      }
    }
  }
}
