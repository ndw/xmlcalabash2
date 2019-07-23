package com.xmlcalabash.util

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.{QName, XdmNode}

class StepErrors(config: XMLCalabashRuntime) {
  private val _code = new QName("", "code")

  def error(code: QName): XdmNode = {
    error(code, None)
  }

  def error(code: QName, message: String): XdmNode = {
    error(code, Some(message))
  }

  def error(code: QName, message: Option[String]): XdmNode = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(config.staticBaseURI)
    builder.addStartElement(XProcConstants.c_errors)
    builder.startContent()
    builder.addStartElement(XProcConstants.c_error)
    builder.addNamespace(code.getPrefix, code.getNamespaceURI)
    builder.addAttribute(_code, code.toString)
    builder.startContent()

    if (message.isDefined) {
      builder.addStartElement(XProcConstants.c_message)
      builder.startContent()
      builder.addText(message.get)
      builder.addEndElement()
    }

    builder.addEndElement()
    builder.addEndElement()
    builder.endDocument()
    builder.result
  }
}
