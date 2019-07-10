package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import com.xmlcalabash.config.XMLCalabashConfig
import net.sf.saxon.s9api.{QName, XdmValue}

trait XmlStep {
  def inputSpec: XmlPortSpecification
  def outputSpec: XmlPortSpecification
  def bindingSpec: BindingSpecification
  def setConsumer(consumer: XProcDataConsumer)
  def setLocation(location: Location)
  def receiveBinding(variable: QName, value: XdmValue, context: StaticContext)
  def receive(port: String, item: Any, metadata: XProcMetadata)
  def configure(config: XMLCalabashConfig, params: Option[ImplParams])
  def initialize(config: RuntimeConfiguration)
  def run(context: StaticContext)
  def reset()
  def abort()
  def stop()
}
