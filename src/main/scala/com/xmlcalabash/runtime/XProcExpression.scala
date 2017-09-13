package com.xmlcalabash.runtime

class XProcExpression(val context: ExpressionContext, val extensionFunctionsAllowed: Boolean) {
  def this(context: ExpressionContext) {
    this(context, false)
  }

  override def toString: String = "{XProcExpression}"
}
