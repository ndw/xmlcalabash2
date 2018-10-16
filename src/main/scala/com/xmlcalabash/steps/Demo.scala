package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmValue}

class Demo() extends DefaultXmlStep {
  private val _number = new QName("", "number")
  var number = 0

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    if (variable == _number) {
      number = value.getUnderlyingValue.head.getStringValue.toInt
    }
  }

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    // ignore the inputs
  }

  override def run(context: StaticContext): Unit = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(URI.create("http://example.com/"))
    builder.startContent()
    builder.addStartElement(new QName("", "doc"))
    builder.startContent()
    builder.addText("Hello, world.")
    builder.addEndElement()
    builder.endDocument()
    val result = builder.result

    consumer.get.receive("result", result, new XProcMetadata(MediaType.XML))
  }
}
