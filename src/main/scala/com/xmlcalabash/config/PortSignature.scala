package com.xmlcalabash.config

class PortSignature(val name: String) {
  private var _cardinality = "1"
  private var _primary = Option.empty[Boolean]

  def this(name: String, primary: Boolean, sequence: Boolean) {
    this(name)
    _primary = Some(primary)
    if (sequence) {
      _cardinality = "*"
    }
  }

  def sequence: Boolean = (_cardinality == "*")
  def cardinality: String = _cardinality
  def cardinality_=(card: String): Unit = {
    _cardinality = card
  }

  def primary: Boolean = _primary.getOrElse(false)
  def primary_=(primary: Boolean): Unit = {
    _primary = Some(primary)
  }
  def primaryDeclared: Boolean = _primary.isDefined

}