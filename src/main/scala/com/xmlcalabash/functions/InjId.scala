package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.om.Sequence
import net.sf.saxon.value.StringValue

class InjId(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(context: XPathContext, arguments: Array[Sequence[_]]): Sequence[_] = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext == null) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    if (exprEval.dynContext.get.injId.isDefined) {
      new StringValue(exprEval.dynContext.get.injId.get)
    } else {
      throw XProcException.xiNotInInjectable()
    }
  }
}
