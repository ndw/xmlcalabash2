package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.SaxonExpressionEvaluator
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.value.{QNameValue, SequenceType}

class InjType private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("cx", XProcConstants.ns_cx, "step-type")

  private var runtime: XMLCalabashConfig = _

  def this(runtime: XMLCalabashConfig) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array.empty[SequenceType]

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_QNAME

  override def makeCallExpression(): ExtensionFunctionCall = {
    new CwdCall(this)
  }

  class CwdCall(val xdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
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
}
