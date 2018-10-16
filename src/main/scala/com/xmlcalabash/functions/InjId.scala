package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.SaxonExpressionEvaluator
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.value.{SequenceType, StringValue}

class InjId private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("cx", XProcConstants.ns_cx, "injectable-id")

  private var runtime: XMLCalabashConfig = _

  def this(runtime: XMLCalabashConfig) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array.empty[SequenceType]

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_STRING

  override def makeCallExpression(): ExtensionFunctionCall = {
    new CwdCall(this)
  }

  class CwdCall(val xdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
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
}
