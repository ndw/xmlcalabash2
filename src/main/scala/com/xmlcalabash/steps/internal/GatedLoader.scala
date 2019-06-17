package com.xmlcalabash.steps.internal

import com.jafpl.messages.JoinGateMessage
import com.xmlcalabash.runtime.XmlPortSpecification

class GatedLoader() extends DefaultStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def run(): Unit = {
    consumer.get.receive("result", new JoinGateMessage())
  }
}
