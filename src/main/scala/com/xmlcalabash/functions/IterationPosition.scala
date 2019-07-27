package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.expr.{StaticContext, XPathContext}
import net.sf.saxon.om.Sequence
import net.sf.saxon.value.Int64Value

class IterationPosition(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(staticContext: StaticContext, context: XPathContext, arguments: Array[Sequence[_]]): Sequence[_] = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext == null) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    val dynContext = exprEval.dynContext
    new Int64Value(dynContext.get.iterationPosition)
  }
}
