package com.xmlcalabash.runtime

class XProcXPathExpression(override val context: ExpressionContext, val expr: String)
  extends XProcExpression(context) {

  override def toString: String = expr
}

