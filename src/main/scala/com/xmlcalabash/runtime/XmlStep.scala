package com.xmlcalabash.runtime

import com.jafpl.steps.Step
import net.sf.saxon.s9api.{QName, XdmItem}

trait XmlStep extends Step {
  override def inputSpec: XmlPortSpecification
  override def outputSpec: XmlPortSpecification
  def receiveBinding(variable: QName, value: XdmItem)
}
