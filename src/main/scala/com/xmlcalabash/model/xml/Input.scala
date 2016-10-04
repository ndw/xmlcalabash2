package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.bindings.Binding
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
class Input(context: Option[XdmNode], override val port: Option[String]) extends IODeclaration(context, port) {
  private var _select: Option[String] = None

  def select = _select

  def select_=(value: Option[String]): Unit = {
    _select = value
  }

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("input"))
    if (port.isDefined) {
      tree.addAttribute(XProcConstants._port, port.get)
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
