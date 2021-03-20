package com.xmlcalabash.util.xc

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class XsltStylesheet(runtime: XMLCalabashRuntime, val bindings: Map[String,String], val exclude: List[String], version: String) {
  private val _exclude_result_prefixes = new QName("", "exclude-result-prefixes")
  private val _match = new QName("", "match")
  private val _name = new QName("", "name")
  private val _select = new QName("", "select")
  private val _lang = new QName("", "lang")
  private val _order = new QName("", "order")
  private val _collation = new QName("", "collation")
  private val _stable = new QName("", "stable")
  private val _case_order = new QName("", "case-order")
  private val _data_type = new QName("", "data-type")
  private val _as = new QName("", "as")

  private val xsl_stylesheet = new QName("xsl", XProcConstants.ns_xsl, "stylesheet")
  private val xsl_template = new QName("xsl", XProcConstants.ns_xsl, "template")
  private val xsl_for_each = new QName("xsl", XProcConstants.ns_xsl, "for-each")
  private val xsl_sequence = new QName("xsl", XProcConstants.ns_xsl, "sequence")
  private val xsl_sort = new QName("xsl", XProcConstants.ns_xsl, "sort")
  private val xsl_text = new QName("xsl", XProcConstants.ns_xsl, "text")
  private val xsl_variable = new QName("xsl", XProcConstants.ns_xsl, "variable")
  private val xsl_value_of = new QName("xsl", XProcConstants.ns_xsl, "value-of")
  private val builder = new SaxonTreeBuilder(runtime)
  private val openStack = mutable.Stack.empty[QName]

  builder.startDocument(None)
  builder.addStartElement(xsl_stylesheet)

  var found = false
  for ((prefix,uri) <- bindings) {
    builder.addNamespace(prefix,uri)
    found = found || prefix == "xsl"
  }
  if (!found) {
    builder.addNamespace("xsl", XProcConstants.ns_xsl)
  }

  found = false
  var xrn = ""
  for (pfx <- exclude) {
    if (pfx == "xsl" || bindings.contains(pfx)) {
      xrn = xrn + pfx + " "
      found = found || pfx == "xsl"
    } else {
      throw new RuntimeException("No binding for exclude prefix: " + pfx)
    }
  }
  if (!found) {
    xrn = xrn + "xsl "
  }
  builder.addAttribute(_exclude_result_prefixes, xrn)
  builder.addAttribute(XProcConstants._version, version)
  builder.startContent()
  openStack.push(xsl_stylesheet)

  def this(runtime: XMLCalabashRuntime) = {
    this(runtime, Map.empty[String,String], List(), "2.0")
  }

  def endStylesheet(): XdmNode = {
    builder.addEndElement()
    openStack.pop()
    builder.endDocument()
    builder.result
  }

  def startTemplate(matchPattern: String): Unit = {
    builder.addStartElement(xsl_template)
    builder.addAttribute(_match, matchPattern)
    builder.startContent()
    openStack.push(xsl_template)
  }

  def startNamedTemplate(name: String): Unit = {
    builder.addStartElement(xsl_template)
    builder.addAttribute(_name, name)
    builder.startContent()
    openStack.push(xsl_template)
  }

  def endTemplate(): Unit = {
    builder.addEndElement()
    openStack.pop()
  }

  def startForEach(select: String): Unit = {
    builder.addStartElement(xsl_for_each)
    builder.addAttribute(_select, select)
    builder.startContent()
    openStack.push(xsl_for_each)
  }

  def endForEach(): Unit = {
    builder.addEndElement()
    openStack.pop()
  }

  def startVariable(name: String): Unit = {
    startVariable(name, None)
  }

  def startVariable(name: String, as: String): Unit = {
    startVariable(name, Some(as))
  }

  private def startVariable(name: String, as: Option[String]): Unit = {
    builder.addStartElement(xsl_variable)
    builder.addAttribute(_name, name)
    if (as.isDefined) {
      builder.addAttribute(_as, as.get)
    }
    builder.startContent()
    openStack.push(xsl_variable)
  }

  def endVariable(): Unit = {
    builder.addEndElement()
    openStack.pop()
  }

  def startSort(select: String, lang: Option[String], order: Option[String],
           collation: Option[String], stable: Option[String], case_order: Option[String]): Unit = {
    builder.addStartElement(xsl_sort)
    builder.addAttribute(_select, select)
    if (lang.isDefined) { builder.addAttribute(_lang, lang.get) }
    if (order.isDefined) { builder.addAttribute(_order, order.get) }
    if (collation.isDefined) { builder.addAttribute(_collation, collation.get) }
    if (stable.isDefined) { builder.addAttribute(_stable, stable.get.toString) }
    if (case_order.isDefined) { builder.addAttribute(_case_order, case_order.get) }
    builder.startContent()
    openStack.push(xsl_sort)
  }

  def endSort(): Unit = {
    builder.addEndElement()
    openStack.pop()
  }

  def sequence(select: String): Unit = {
    builder.addStartElement(xsl_sequence)
    builder.addAttribute(_select, select)
    builder.startContent()
    builder.addEndElement()
  }

  def valueOf(select: String): Unit = {
    builder.addStartElement(xsl_value_of)
    builder.addAttribute(_select, select)
    builder.startContent()
    builder.addEndElement()
  }

  def literal(qname: QName, content: String): Unit = {
    builder.addStartElement(qname)
    builder.startContent()
    builder.addText(content)
    builder.addEndElement()
  }

  def text(content: String): Unit = {
    builder.addStartElement(xsl_text)
    builder.startContent()
    builder.addText(content)
    builder.addEndElement()
  }
}
