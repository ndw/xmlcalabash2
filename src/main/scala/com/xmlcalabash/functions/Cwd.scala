package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.om.Sequence
import net.sf.saxon.value.AnyURIValue

class Cwd(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(context: XPathContext, arguments: Array[Sequence[_]]): Sequence[_] = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext == null) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    val cwd = runtime.staticBaseURI.toASCIIString
    new AnyURIValue(cwd)
  }
}
