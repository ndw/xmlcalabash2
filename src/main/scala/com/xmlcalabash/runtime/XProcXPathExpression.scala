package com.xmlcalabash.runtime

import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.value.SequenceType

class XProcXPathExpression(override val context: ExpressionContext, val expr: String, val as: Option[SequenceType], val values: Option[List[XdmAtomicValue]])
  extends XProcExpression(context) {

  def this(context: ExpressionContext, expr: String) = {
    this(context, expr, None, None)
  }

  def this(context: ExpressionContext, expr: String, as: Option[SequenceType]) = {
    this(context, expr, as, None)
  }

  override def toString: String = expr
}

