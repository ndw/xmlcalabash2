package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.xml.DeclareStep
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.expr.{StaticContext, XPathContext}
import net.sf.saxon.om.{Item, Sequence, StructuredQName}
import net.sf.saxon.s9api.QName
import net.sf.saxon.value.BooleanValue

class StepAvailable(runtime: XMLCalabashConfig) extends FunctionImpl() {
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

    var available = false

    // This is a bit of a hack; it relies on artifact having been passed to the
    // dynamic context and I'm not confident that's always going to have happened.
    // But it appears to have happened in the common case.
    var art = exprEval.dynContext.get.artifact
    while (art.isDefined && !art.get.isInstanceOf[DeclareStep]) {
      art = art.get.parent
    }
    if (art.isDefined) {
      val decl = art.get.asInstanceOf[DeclareStep]
      val qname = new QName(propertyName.getPrefix, propertyName.getURI, propertyName.getLocalPart)
      val sig = decl.declaration(qname)
      if (sig.isDefined) {
        val impl = sig.get.implementation
        if (impl.isEmpty) {
          available = !sig.get.declaration.get.atomic
        } else {
          available = true
        }
      }
    }

    new BooleanValue(available, BuiltInAtomicType.BOOLEAN)
  }
}
