package com.xmlcalabash.steps

import javax.xml.transform.sax.SAXSource

import org.xml.sax.InputSource

class Document extends DefaultStep {
  private var _href = ""

  override def receiveBinding(variable: String, value: Any): Unit = {
    println(s"Document receives binding: $variable: $value")
    if (variable == "href") {
      _href = value.toString
    }
  }

  override def run(): Unit = {
    val builder = config.get.processor.newDocumentBuilder()
    val source = new SAXSource(new InputSource(_href))
    builder.setDTDValidation(false)
    builder.setLineNumbering(true)
    val node = builder.build(source)
    consumer.get.send("result", node)
  }

}
