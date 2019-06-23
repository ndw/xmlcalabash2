package com.xmlcalabash.steps.text

import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue}

import scala.collection.mutable.ListBuffer

class Sort() extends TextLines {
  private val _order = new QName("", "order")
  private val _case_order = new QName("", "case-order")
  private val _lang = new QName("", "lang")
  private val _data_type = new QName("", "data-type")
  private val _collation = new QName("", "collation")
  private val _stable = new QName("", "stable")

  private var ascending = true

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  def comparitor(a: String, b: String): Boolean = {
    if (ascending) {
      a.compareTo(b) < 0
    } else {
      a.compareTo(b) > 0
    }
  }

  override def run(context: StaticContext): Unit = {
    if (bindings.contains(_order)) {
      ascending = bindings(_order).getStringValue == "ascending"
    }

    val newLines = lines.sortWith(comparitor)
    var hlines = ""
    for (line <- newLines) {
      hlines += line + "\n"
    }

    consumer.get.receive("result", new XdmAtomicValue(hlines), new XProcMetadata(MediaType.TEXT))
  }
}
