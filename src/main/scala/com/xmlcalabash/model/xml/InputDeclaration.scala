package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.bindings.Binding
import com.xmlcalabash.model.xml.bindings.PipeBinding
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
class InputDeclaration(context: Option[XdmNode], override val port: Option[String]) extends Input(context, port) {
  private var _sequence: Option[Boolean] = None
  private var _primary: Option[Boolean] = None
  private var _contentTypes: Option[List[String]] = None

  if (port.isEmpty) {
    staticError("The port attribute is required on input declarations")
  }

  def sequence = _sequence
  def primary = _primary
  def contentTypes = _contentTypes

  def sequence_=(value: Option[Boolean]): Unit = {
    _sequence = value
  }

  def primary_=(value: Option[Boolean]): Unit = {
    _primary = value
  }

  def contentTypes_=(values: Option[List[String]]): Unit = {
    _contentTypes = values
  }

  def addContentType(contentType: String): Unit = {
    if (_contentTypes.isDefined) {
      _contentTypes = Some(_contentTypes.get ::: List(contentType))
    } else {
      _contentTypes = Some(List(contentType))
    }
  }

  override def addBinding(binding: Binding) = binding match {
    case pipe: PipeBinding => staticError("Pipe binding is not allowed in input declaration")
    case _ => super.addBinding(binding)
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("input-declaration"))
    if (port.isDefined) {
      tree.addAttribute(XProcConstants._port, port.get)
    }
    if (sequence.isDefined) {
      tree.addAttribute(XProcConstants._sequence, sequence.get.toString)
    }
    if (primary.isDefined) {
      tree.addAttribute(XProcConstants._primary, primary.get.toString)
    }
    if (contentTypes.isDefined) {
      tree.addAttribute(XProcConstants._content_types, contentTypes.get.mkString(" "))
    }
    if (select.isDefined) {
      tree.addAttribute(XProcConstants._select, port.get)
    }
    if (bindings.isDefined) {
      bindings.get.foreach { _.dump(tree) }
    }
    tree.addEndElement()
  }
}
