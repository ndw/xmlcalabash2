package com.xmlcalabash.steps

import com.jafpl.exceptions.StepException
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.{BindingSpecification, PortSpecification, Step, StepDataProvider}
import com.xmlcalabash.runtime.SaxonRuntimeConfiguration

class DefaultStep extends Step {
  protected var consumer: Option[StepDataProvider] = None
  protected var config: Option[SaxonRuntimeConfiguration] = None

  override def inputSpec: PortSpecification = PortSpecification.ANY
  override def outputSpec: PortSpecification = PortSpecification.ANY
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: String, value: Any): Unit = {
    // nop
  }

  override def setConsumer(consumer: StepDataProvider): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any): Unit = {
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
