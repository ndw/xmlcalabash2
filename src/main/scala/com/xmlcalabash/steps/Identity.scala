package com.xmlcalabash.steps

import com.jafpl.messages.Metadata
import com.xmlcalabash.runtime.XmlPortSpecification

class Identity() extends DefaultStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    consumer.get.receive("result", item, metadata)
  }
}
