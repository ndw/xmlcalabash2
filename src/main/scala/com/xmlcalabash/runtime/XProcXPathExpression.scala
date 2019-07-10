package com.xmlcalabash.runtime

import net.sf.saxon.s9api.{SequenceType, XdmAtomicValue}

class XProcXPathExpression(override val context: StaticContext,
                           val expr: String,
                           val as: Option[SequenceType],
                           val values: Option[List[XdmAtomicValue]],
                           val params: Option[ExprParams])
  extends XProcExpression(context) {

  def this(context: StaticContext, expr: String) = {
    this(context, expr, None, None, None)
  }

  def this(context: StaticContext, expr: String, as: Option[SequenceType]) = {
    this(context, expr, as, None, None)
  }

  def this(context: StaticContext, expr: String, as: Option[SequenceType], values: Option[List[XdmAtomicValue]], params: ExprParams) {
    this(context, expr, as, values, Some(params))
  }

  override def toString: String = expr
}

