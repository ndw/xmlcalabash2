package com.xmlcalabash.model.xml.bindings

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/1/16.
  */
class PipeBinding(override val context: Option[XdmNode]) extends Binding(context: Option[XdmNode]) {
  var _stepName: Option[String] = None
  var _portName: Option[String] = None

  def stepName = _stepName
  def portName = _portName

  def stepName_=(value: Option[String]): Unit = {
    _stepName = value
  }

  def portName_=(value: Option[String]): Unit = {
    _portName = value
  }

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("pipe"))
    if (stepName.isDefined) {
      tree.addAttribute(XProcConstants._step, stepName.get)
    }
    if (portName.isDefined) {
      tree.addAttribute(XProcConstants._port, portName.get)
    }
    tree.addEndElement()
  }
}
