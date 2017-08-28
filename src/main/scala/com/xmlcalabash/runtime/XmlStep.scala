package com.xmlcalabash.runtime

import com.jafpl.steps.Step

trait XmlStep extends Step {
  override def inputSpec: XmlPortSpecification
  override def outputSpec: XmlPortSpecification

}
