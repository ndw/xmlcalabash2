package com.xmlcalabash.steps

import com.jafpl.steps.PortSpecification

class Sink extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.NONE
}
