package com.xmlcalabash.steps

import com.jafpl.exceptions.StepException
import com.jafpl.messages.Metadata
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, DataConsumer, PortSpecification, Step}
import com.xmlcalabash.runtime.{SaxonRuntimeConfiguration, XmlPortSpecification, XmlStep}

class DefaultStep extends XmlStep {
  protected var consumer: Option[DataConsumer] = None
  protected var config: Option[SaxonRuntimeConfiguration] = None

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
}
