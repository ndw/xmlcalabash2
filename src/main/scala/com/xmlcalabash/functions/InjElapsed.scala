package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.om.Sequence
import net.sf.saxon.value.DoubleValue

class InjElapsed(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext == null) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    if (exprEval.dynContext.get.injElapsed.isDefined) {
      new DoubleValue(exprEval.dynContext.get.injElapsed.get)
    } else {
      throw XProcException.xiNotInInjectable()
    }
  }
}
