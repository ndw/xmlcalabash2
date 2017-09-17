package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import com.jafpl.runtime.RuntimeConfiguration
import com.jafpl.steps.BindingSpecification
import net.sf.saxon.s9api.{QName, XdmItem}

trait XmlStep {
  def inputSpec: XmlPortSpecification
  def outputSpec: XmlPortSpecification
  def bindingSpec: BindingSpecification
  def setConsumer(consumer: XProcDataConsumer)
  def setLocation(location: Location)
  def receiveBinding(variable: QName, value: XdmItem, context: ExpressionContext)
  def receive(port: String, item: Any, metadata: XProcMetadata)
  def initialize(config: RuntimeConfiguration)
  def run(context: StaticContext)
  def reset()
  def abort()
  def stop()
}
