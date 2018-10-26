package com.xmlcalabash.steps

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType

class ExceptionTranslator() extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(None)
    tree.addStartElement(XProcConstants.c_errors)
    tree.startContent()
    tree.addText(item.asInstanceOf[Exception].getMessage)
    tree.addEndElement()
    tree.endDocument()
    consumer.get.receive("result", tree.result, new XProcMetadata(MediaType.XML))
  }
}
