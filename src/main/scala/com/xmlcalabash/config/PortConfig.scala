package com.xmlcalabash.config

class PortConfig(val name: String) {
  private var _cardinality = "1"
  private var _primary = Option.empty[Boolean]

  def cardinality: String = _cardinality
  protected[config] def cardinality_=(card: String): Unit = {
    _cardinality = card
  }

  def primary: Option[Boolean] = _primary
  protected[config] def primary_=(primary: Boolean): Unit = {
    _primary = Some(primary)
  }

}
