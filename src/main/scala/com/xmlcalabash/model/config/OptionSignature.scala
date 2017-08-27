package com.xmlcalabash.model.config

class OptionSignature(val name: String, val optType: String, val required: Boolean, val default: Option[String]) {
  def this(name: String, optType: String, required: Boolean, default: String) {
    this(name, optType, required, Some(default))
  }
  def this(name: String, optType: String, required: Boolean) {
    this(name, optType, required, None)
  }
}
