package com.xmlcalabash.steps

import com.jafpl.steps.PortSpecification

class Identity() extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCESEQ
  override def outputSpec: PortSpecification = PortSpecification.RESULTSEQ

  override def receive(port: String, item: Any): Unit = {
    consumer.get.send("result", item)
  }
}
