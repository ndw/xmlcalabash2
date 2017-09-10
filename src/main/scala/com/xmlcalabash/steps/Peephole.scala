package com.xmlcalabash.steps

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}

class Peephole extends DefaultXmlStep {
  private var item = Option.empty[Any]
  private var metadata = Option.empty[XProcMetadata]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    this.item = Some(item)
    this.metadata = Some(metadata)
  }

  override def run() {
    if (item.isDefined) {
      println("cx:peephole: " + item.get)
      consumer.get.receive("result", item.get, metadata.get)
    } else {
      println("cx:peephole received no documents")
    }
  }
}
