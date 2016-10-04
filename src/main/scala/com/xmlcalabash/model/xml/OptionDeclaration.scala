package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 10/1/16.
  */

// I'd have just called this Option except that that interferes with Scala Option.
class OptionDeclaration(context: Option[XdmNode], val name: QName) extends Artifact(context: Option[XdmNode]) {
  private var _as: Option[String] = None
  private var _required: Option[Boolean] = None
  private var _select: Option[String] = None

  def as = _as
  def required = _required
  def select = _select

  def as_=(value: String): Unit = {
    _as = Some(value)
  }

  def required_=(value: Boolean): Unit = {
    _required = Some(value)
    checkRequired()
  }

  def select_=(value: String): Unit = {
    _select = Some(value)
    checkRequired()
  }

  private def checkRequired(): Unit = {
    if (_required.isDefined && _required.get && _select.isDefined) {
      staticError("You cannot have a select attribute on a required option")
    }
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.px("option-declaration"))
    tree.addAttribute(XProcConstants._name, name.toString)
    if (required.isDefined) {
      tree.addAttribute(XProcConstants._required, required.get.toString)
    }
    if (as.isDefined) {
      tree.addAttribute(XProcConstants._as, as.get)
    }
    if (select.isDefined) {
      tree.addAttribute(XProcConstants._select, select.get)
    }
    tree.addEndElement()
  }
}
