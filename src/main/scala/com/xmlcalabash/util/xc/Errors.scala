package com.xmlcalabash.util.xc

import com.jafpl.graph.Location
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import net.sf.saxon.`type`.ValidationFailure
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class Errors(config: XMLCalabashConfig) {
  private val builder = new SaxonTreeBuilder(config)
  private val openStack = mutable.Stack.empty[QName]
  private val inLibrary = false

  builder.startDocument(None)
  builder.addStartElement(XProcConstants.c_errors)
  builder.addNamespace("c", XProcConstants.ns_c)
  builder.addNamespace("err", XProcConstants.ns_xqt_errors)
  builder.startContent()
  openStack.push(XProcConstants.c_errors)

  def endErrors(): XdmNode = {
    builder.addEndElement()
    openStack.pop()
    builder.endDocument()
    builder.result
  }

  private def end(): Unit = {
    builder.addEndElement()
    openStack.pop()
  }

  def xsdValidationError(msg: String, fail: ValidationFailure): Unit = {
    builder.addStartElement(XProcConstants.c_error)
    builder.addAttribute(XProcConstants._message, msg)

    if (Option(fail.getSystemId).isDefined) {
      builder.addAttribute(XProcConstants._source_uri, fail.getSystemId)
    }

    if (fail.getLineNumber > 0) {
      builder.addAttribute(XProcConstants._source_line, fail.getLineNumber.toString)
      if (fail.getColumnNumber > 0) {
        builder.addAttribute(XProcConstants._source_column, fail.getColumnNumber.toString)
      }
    }

    if (Option(fail.getAbsolutePath).isDefined) {
      builder.addAttribute(XProcConstants._path, fail.getAbsolutePath.toString)
    }

    if (Option(fail.getErrorCodeQName).isDefined) {
      builder.addNamespace("err", fail.getErrorCodeQName.getURI)
      builder.addAttribute(XProcConstants._code, fail.getErrorCodeQName.toString)
    }

    if (Option(fail.getSchemaPart).isDefined) {
      builder.addAttribute(XProcConstants._schema_part, fail.getSchemaPart.toString)
    }

    if (Option(fail.getConstraintName).isDefined) {
      builder.addAttribute(XProcConstants._constraint_name, fail.getConstraintName)
    }

    if (Option(fail.getConstraintClauseNumber).isDefined) {
      builder.addAttribute(XProcConstants._constraint_cause, fail.getConstraintClauseNumber)
    }

    if (Option(fail.getSchemaType).isDefined) {
      builder.addAttribute(XProcConstants._schema_type, fail.getSchemaType.toString)
    }

    builder.startContent()
    builder.addEndElement()
  }

  def xsdValidationError(msg: String): Unit = {
    builder.addStartElement(XProcConstants.c_error)
    builder.addAttribute(XProcConstants._message, msg)
    builder.startContent()
    builder.addEndElement()
  }
}
