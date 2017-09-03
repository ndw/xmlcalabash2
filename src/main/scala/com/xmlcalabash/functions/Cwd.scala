package com.xmlcalabash.functions

import com.xmlcalabash.model.xml.XProcConstants
import com.xmlcalabash.runtime.SaxonRuntimeConfiguration
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.value.{AnyURIValue, SequenceType}

class Cwd private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("exf", XProcConstants.ns_exf, "cwd")

  private var runtime: SaxonRuntimeConfiguration = _

  def this(runtime: SaxonRuntimeConfiguration) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array.empty[SequenceType]

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ATOMIC

  override def makeCallExpression(): ExtensionFunctionCall = {
    new CwdCall(this)
  }

  class CwdCall(val xdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val cwd = runtime.staticBaseURI.toASCIIString
      new AnyURIValue(cwd)
    }
  }

}
