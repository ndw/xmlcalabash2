package com.xmlcalabash.model.xml.decl

/**
  * Created by ndw on 10/4/16.
  */
class InputDecl(val port: String) extends Decl {
  private var _sequence = false
  private[decl] var _primary: Option[Boolean] = None
  private var _kind = "document"
  private var _select: Option[String] = None

  def this(port: String, sequence: Boolean = false) {
    this(port)
    _sequence = sequence
  }

  def sequence = _sequence
  def primary = _primary.getOrElse(false)
  def kind = _kind
  def select = _select

  def sequence_=(value: Boolean): Unit = {
    _sequence = value
  }
  def primary_=(value: Boolean): Unit = {
    _primary = Some(value)
  }
  def kind_=(value: String): Unit = {
    _kind = value
  }
  def select_=(value: String): Unit = {
    _select = Some(value)
  }
}
