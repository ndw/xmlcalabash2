package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.{StaticContext, XPathContext}
import net.sf.saxon.om.{Item, Sequence, StructuredQName}
import net.sf.saxon.value.StringValue

class SystemProperty(runtime: XMLCalabashConfig) extends FunctionImpl() {
  private val _localhost = "http://localhost/"

  def call(staticContext: StaticContext, context: XPathContext, arguments: Array[Sequence[_]]): Sequence[_] = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext == null) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    val lexicalQName = arguments(0).head().asInstanceOf[Item[_]].getStringValue
    val propertyName = if (lexicalQName.trim.startsWith("Q{")) {
      StructuredQName.fromClarkName(lexicalQName)
    } else {
      StructuredQName.fromLexicalQName(lexicalQName, false, false, staticContext.getNamespaceResolver)
    }

    val uri = propertyName.getURI
    val local = propertyName.getLocalPart
    var value = ""

    uri match {
      case XProcConstants.ns_p =>
        local match {
          case "episode" =>
            value = runtime.episode
          case "locale" =>
            value = runtime.locale
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
      case _localhost =>
        value = System.getProperty(local)
      case _ =>
        Unit
    }

    new StringValue(value)
  }
}
