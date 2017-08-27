package com.xmlcalabash.steps

import com.jafpl.steps.{BindingSpecification, PortSpecification, Step, StepDataProvider}

class DefaultStep extends Step {
  protected var consumer: Option[StepDataProvider] = None

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

  override def initialize(): Unit = {
    // nop
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
