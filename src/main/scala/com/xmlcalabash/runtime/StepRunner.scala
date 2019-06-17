package com.xmlcalabash.runtime
import com.jafpl.graph.Location
import com.jafpl.messages.{ExceptionMessage, JoinGateMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortCardinality}
import com.xmlcalabash.config.StepSignature
import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.messages.{AnyItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.xml.DeclareStep
import com.xmlcalabash.util.XProcVarValue
import net.sf.saxon.s9api.{QName, XdmItem, XdmValue}

import scala.collection.mutable

class StepRunner(private val pruntime: XMLCalabashRuntime, val decl: DeclareStep, val signature: StepSignature) extends StepExecutable {
  private val runtime = pruntime.runtime(decl)

  private val cardMap = mutable.HashMap.empty[String,PortCardinality]
  private val typeMap = mutable.HashMap.empty[String,List[String]]
  for (port <- signature.inputPorts) {
    val portSig = signature.input(port, decl.location.get)
    portSig.cardinality match {
      case "1" => cardMap.put(portSig.name, new PortCardinality(1,1))
      case "*" => cardMap.put(portSig.name, new PortCardinality(0))
      case "+" => cardMap.put(portSig.name, new PortCardinality(1))
      case _ => throw new RuntimeException("WTF? Cardinality=" + portSig.cardinality)
    }
    typeMap.put(portSig.name, List("application/octet-stream")) // FIXME: THIS IS A LIE
  }

  private val iSpec = new XmlPortSpecification(cardMap.toMap, typeMap.toMap)
  cardMap.clear()
  typeMap.clear()
  for (port <- signature.outputPorts) {
    val portSig = signature.output(port, decl.location.get)
    portSig.cardinality match {
      case "1" => cardMap.put(portSig.name, new PortCardinality(1,1))
      case "*" => cardMap.put(portSig.name, new PortCardinality(0))
      case "+" => cardMap.put(portSig.name, new PortCardinality(1))
      case _ => throw new RuntimeException("WTF? Cardinality=" + portSig.cardinality)
    }
    typeMap.put(portSig.name, List("application/octet-stream")) // FIXME: THIS IS A LIE
  }
  private val oSpec = new XmlPortSpecification(cardMap.toMap, typeMap.toMap)

  override def inputSpec: XmlPortSpecification = iSpec

  override def outputSpec: XmlPortSpecification = oSpec

  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def setConsumer(consumer: XProcDataConsumer): Unit = {
    for (port <- signature.outputPorts) {
      runtime.output(port, new ConsumerMap(port, consumer))
    }
  }

  override def setLocation(location: Location): Unit = {
    // nop
  }

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    runtime.option(variable, new XProcVarValue(value, ExpressionContext.NONE))
  }

  // Input to the pipeline
  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (item == runtime.joinGateMarker) {
      // This is a hack to tunnel the JoinGateMessage through...
      runtime.inputMessage(port, new JoinGateMessage())
      return
    }

    item match {
      case value: XdmItem => runtime.input(port, value, metadata)
      case _ => throw new RuntimeException("Unexpected value sent to StepRunner")
    }
  }

  override def initialize(config: RuntimeConfiguration, params: Option[ImplParams]): Unit = {
    // nop?
  }

  override def run(context: StaticContext): Unit = {
    runtime.runAsStep()
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
      // I'm not sure why port is incorrect here. Because it's the output from the consumer, maybe?
      // Anyway, we construct these mapping objects so that result_port is the correct output
      // port name.

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
