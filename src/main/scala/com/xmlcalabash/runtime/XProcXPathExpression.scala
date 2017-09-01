package com.xmlcalabash.runtime

class XProcXPathExpression(override val nsbindings: Map[String,String], val expr: String)
  extends XProcExpression(nsbindings) {

  override def toString: String = expr
}

