package com.xmlcalabash.steps

import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}

// THIS IS NOT AN INSERT STEP. THIS IS A COPY OF IDENTITY!

class Insert() extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    consumer.get.receive("result", item, metadata)
  }
}
