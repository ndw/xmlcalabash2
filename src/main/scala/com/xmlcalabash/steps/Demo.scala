package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmValue}

class Demo() extends DefaultXmlStep {
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

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
