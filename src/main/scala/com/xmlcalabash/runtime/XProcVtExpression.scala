package com.xmlcalabash.runtime

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.parsers.XPathParser
import net.sf.saxon.expr.parser.ExpressionTool
import net.sf.saxon.stax.XMLStreamWriterDestination

class XProcVtExpression private(override val context: ExpressionContext) extends XProcExpression(context) {
  private var _avt: List[String] = _
  private var _string = false

  def this(context: ExpressionContext, avt: List[String], stringResult: Boolean) {
    this(context)
    _avt = avt
    _string = stringResult
  }

  def this(context: ExpressionContext, avt: List[String]) {
    this(context, avt, false)
  }

  def this(context: ExpressionContext, expr: String, stringResult: Boolean) {
    this(context)
    val avt = ValueParser.parseAvt(expr)
    if (avt.isEmpty) {
      throw XProcException.xiInvalidAVT(context.location, expr)
    }
    _avt = avt.get
    _string = stringResult
  }

  def this(context: ExpressionContext, expr: String) {
    this(context, expr, false)
  }

  def avt: List[String] = _avt
  def stringResult: Boolean = _string

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

  def checkContext(config: XMLCalabashRuntime): Unit = {
    var isexpr = false
    for (xpathexpr <- avt) {
      if (isexpr) {
        val compiler = config.processor.newXPathCompiler()
        val expr = compiler.compile(xpathexpr).getUnderlyingExpression.getInternalExpression
        val focus = ExpressionTool.dependsOnFocus(expr)
        print(focus,":",xpathexpr)
      }
      isexpr = !isexpr
    }
  }
}
