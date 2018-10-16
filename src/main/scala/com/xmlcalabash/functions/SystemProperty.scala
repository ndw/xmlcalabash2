package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.SaxonExpressionEvaluator
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.value.{SequenceType, StringValue}

class SystemProperty private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "system-property")

  private var runtime: XMLCalabashConfig = _

  def this(runtime: XMLCalabashConfig) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_STRING)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ATOMIC

  override def makeCallExpression(): ExtensionFunctionCall = {
    new SysPropCall(this)
  }

  class SysPropCall(val xdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    var staticContext: StaticContext = _

    override def supplyStaticContext(context: StaticContext, locationId: Int, arguments: Array[Expression]): Unit = {
      staticContext = context
    }

    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
      if (exprEval.dynContext == null) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val lexicalQName = arguments(0).head().getStringValue
      val propertyName = StructuredQName.fromLexicalQName(lexicalQName, false, false, staticContext.getNamespaceResolver)

      val uri = propertyName.getURI
      val local = propertyName.getLocalPart
      var value = ""

      uri match {
        case XProcConstants.ns_p =>
          local match {
            case "episode" =>
              value = runtime.episode
            case "language" =>
              value = runtime.language
            case "product-name" =>
              value = runtime.productName
            case "product-version" =>
              value = runtime.productVersion
            case "vendor" =>
              value = runtime.vendor
            case "vendor-uri" =>
              value = runtime.vendorURI
            case "version" =>
              value = runtime.xprocVersion
            case "xpath-version" =>
              value = runtime.xpathVersion
            case "psvi-supported" =>
              value = runtime.psviSupported.toString
            case _ =>
              Unit
          }
        case XProcConstants.ns_cx =>
          local match {
            case "transparent-json" =>
              value = "false"
            case "json-flavor" =>
              value = "none"
            case "general-values" =>
              value = "true"
            case "allow-text-results" =>
              value = "true"
            case "xpointer-on-text" =>
              value = "true"
            case "use-xslt-1.0" =>
              value = "false"
            case "html-serializer" =>
              value = "none"
            case "saxon-version" =>
              value = runtime.processor.getSaxonProductVersion
            case "saxon-edition" =>
              value = runtime.processor.getSaxonEdition
            case _ =>
              Unit
          }
        case _ =>
          Unit
      }

      new StringValue(value)
    }
  }
}
