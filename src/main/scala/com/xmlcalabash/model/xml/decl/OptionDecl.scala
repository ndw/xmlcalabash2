package com.xmlcalabash.model.xml.decl

import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/4/16.
  */
class OptionDecl(val name: QName) extends Decl {
  private var _required = false
  private var _select: Option[String] = None

  def this(name: QName, required: Boolean = false, select: String = null) {
    this(name)
    _required = required
    if (select != null) {
      _select = Some(select)
    }
  }

  def required = _required
  def select = _select

  def required_=(value: Boolean): Unit = {
    _required = value
  }
  def select_=(value: String): Unit = {
    _select = Some(value)
  }


}
