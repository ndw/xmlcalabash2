package com.xmlcalabash.steps.internal

import com.xmlcalabash.runtime.XmlPortSpecification

class EmptyLoader() extends DefaultStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def run(): Unit = {
    // Produces nothing
  }
}
