package com.xmlcalabash.runtime

class XProcExpression(val nsbindings: Map[String,String], val extensionFunctionsAllowed: Boolean) {
  def this(nsbindings: Map[String,String]) {
    this(nsbindings, false)
  }

  override def toString: String = "{XProcExpression}"
}
