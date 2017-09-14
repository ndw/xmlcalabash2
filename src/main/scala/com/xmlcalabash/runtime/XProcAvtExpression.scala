package com.xmlcalabash.runtime

import com.jafpl.exceptions.PipelineException
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.ValueParser

class XProcAvtExpression private (override val context: ExpressionContext)
  extends XProcExpression(context) {
  private var _avt: List[String] = _

  def this(context: ExpressionContext, avt: List[String]) {
    this(context)
    _avt = avt
  }

  def this(context: ExpressionContext, expr: String) {
    this(context)
    val avt = ValueParser.parseAvt(expr)
    if (avt.isEmpty) {
      throw XProcException.xiInvalidAVT(context.location, expr)
    }
    _avt = avt.get
  }

  def avt: List[String] = _avt

  override def toString: String = {
    var str = ""
    var isavt = false
    for (item <- avt) {
      if (isavt) {
        str += "{" + item + "}"
      } else {
        str += item
      }
      isavt = !isavt
    }
    str
  }
}
