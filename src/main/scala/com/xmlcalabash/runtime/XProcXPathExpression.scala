package com.xmlcalabash.runtime

import net.sf.saxon.value.SequenceType

class XProcXPathExpression(override val context: ExpressionContext, val expr: String, val as: Option[SequenceType])
  extends XProcExpression(context) {

  def this(context: ExpressionContext, expr: String) = {
    this(context, expr, None)
  }

  override def toString: String = expr
}

