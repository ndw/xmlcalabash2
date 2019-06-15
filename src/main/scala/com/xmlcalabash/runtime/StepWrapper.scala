package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.StepSignature
import net.sf.saxon.s9api.{QName, XdmValue}

class StepWrapper(private val step: XmlStep, val signature: StepSignature) extends StepExecutable {
  override def inputSpec: XmlPortSpecification = step.inputSpec
  override def outputSpec: XmlPortSpecification = step.outputSpec
  override def bindingSpec: BindingSpecification = step.bindingSpec
  override def setConsumer(consumer: XProcDataConsumer): Unit = step.setConsumer(consumer)
  override def setLocation(location: Location): Unit = step.setLocation(location)
  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    step.receiveBinding(variable, value, context)
  }
  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = step.receive(port,item,metadata)
  override def initialize(config: RuntimeConfiguration, params: Option[ImplParams]): Unit = {
    step.initialize(config, params)
  }
  override def run(context: StaticContext): Unit = step.run(context)
  override def reset(): Unit = step.reset()
  override def abort(): Unit = step.abort()
  override def stop(): Unit = step.stop()
}
