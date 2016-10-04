package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.bindings.Binding
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
class OutputDeclaration(context: Option[XdmNode], override val port: Option[String]) extends IODeclaration(context, port) {
  private var _primary: Option[Boolean] = None
  private var _sequence: Option[Boolean] = None
  private var _select: Option[String] = None

  if (port.isEmpty) {
    staticError("The port attribute is required on output declarations")
  }

  def primary = _primary
  def sequence = _sequence
  def select = _select

  def select_=(value: String): Unit = {
    _select = Some(value)
  }

  def sequence_=(value: Option[Boolean]): Unit = {
    _sequence = value
  }

  def primary_=(value: Option[Boolean]): Unit = {
    _primary = value
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("output-declaration"))
    tree.addAttribute(XProcConstants._port, port.get)
    if (select.isDefined) {
      tree.addAttribute(XProcConstants._select, select.get)
    }
    if (bindings.isDefined) {
      bindings.get.foreach { _.dump(tree) }
    }
    tree.addEndElement()
  }
}
