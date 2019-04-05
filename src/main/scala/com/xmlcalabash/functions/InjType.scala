package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.om.Sequence
import net.sf.saxon.value.QNameValue

class InjType(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(context: XPathContext, arguments: Array[Sequence[_]]): Sequence[_] = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext == null) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    if (exprEval.dynContext.get.injType.isDefined) {
      val qn = exprEval.dynContext.get.injType.get
      new QNameValue(qn.getPrefix, qn.getNamespaceURI, qn.getLocalName)
    } else {
      throw XProcException.xiNotInInjectable()
    }
  }
}
