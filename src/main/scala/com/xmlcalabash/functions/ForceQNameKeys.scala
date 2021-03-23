package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.StaticContext
import com.xmlcalabash.util.S9Api
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.{Item, Sequence}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}
import net.sf.saxon.value.QNameValue

class ForceQNameKeys(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext.isEmpty) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    val inputMap = arguments(0).head match {
      case map: MapItem => map
      case _ => throw new RuntimeException("arg to fqk must be a map")
    }

    S9Api.forceQNameKeys(inputMap, new StaticContext(runtime)).getUnderlyingValue
  }
}
