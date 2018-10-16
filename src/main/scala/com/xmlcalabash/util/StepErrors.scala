package com.xmlcalabash.util

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import net.sf.saxon.s9api.{QName, XdmNode}

class StepErrors(config: XMLCalabashConfig) {
  private val err_errors = new QName("", XProcConstants.ns_err, "errors")
  private val err_error = new QName("", XProcConstants.ns_err, "error")
  private val err_message = new QName("", XProcConstants.ns_err, "message")
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
    builder.addStartElement(err_errors)
    builder.startContent()
    builder.addStartElement(err_error)
    builder.addNamespace(code.getPrefix, code.getNamespaceURI)
    builder.addAttribute(_code, code.toString)
    builder.startContent()

    if (message.isDefined) {
      builder.addStartElement(err_message)
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
