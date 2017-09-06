package com.xmlcalabash.steps

import javax.xml.transform.sax.SAXSource

import com.xmlcalabash.runtime.{XmlMetadata, XmlPortSpecification}
import org.xml.sax.InputSource

class Document extends DefaultStep {
  private var _href = ""

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: String, value: Any): Unit = {
    config.get.trace("debug", s"Document receives binding: $variable: $value", "stepBindings")
    if (variable == "href") {
      _href = value.toString
    }
  }

  override def run(): Unit = {
    val node = config.get.documentManager.parse(_href)
    consumer.get.receive("result", node, new XmlMetadata("application/xml"))
  }

}
