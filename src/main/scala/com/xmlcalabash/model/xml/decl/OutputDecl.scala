package com.xmlcalabash.model.xml.decl

/**
  * Created by ndw on 10/4/16.
  */
class OutputDecl(val port: String) extends Decl {
  private var _sequence = false
  private[decl] var _primary: Option[Boolean] = None

  def this(port: String, sequence: Boolean = false) {
    this(port)
    _sequence = sequence
  }

  def sequence = _sequence
  def primary = _primary.getOrElse(false)

  def sequence_=(value: Boolean): Unit = {
    _sequence = value
  }
  def primary_=(value: Boolean): Unit = {
    _primary = Some(value)
  }
}
