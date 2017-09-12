package com.xmlcalabash.runtime

import com.jafpl.exceptions.PipelineException
import com.xmlcalabash.model.util.StringParsers

class XProcAvtExpression private (override val nsbindings: Map[String,String])
  extends XProcExpression(nsbindings) {
  private var _avt: List[String] = _

  def this(nsbindings: Map[String,String], avt: List[String]) {
    this(nsbindings)
    _avt = avt
  }

  def this(nsbindings: Map[String,String], expr: String) {
    this(nsbindings)
    val avt = StringParsers.parseAvt(expr)
    if (avt.isEmpty) {
      throw new PipelineException("invalid", "Invalid AVT expression: " + expr, None)
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
