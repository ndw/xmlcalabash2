package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap, XdmValue}
import net.sf.saxon.value.SequenceType

class DocumentProperties(runtime: XMLCalabashConfig) extends FunctionImpl {
  val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-properties")

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_ITEM)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ITEM

  override def makeCallExpression(): ExtensionFunctionCall = {
    new DocPropsCall(this)
  }

  private class DocPropsCall(funcdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    private var staticContext: StaticContext = _

    override def supplyStaticContext(context: StaticContext, locationId: Int, arguments: Array[Expression]): Unit = {
      super.supplyStaticContext(context, locationId, arguments)
      staticContext = context
    }

    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext.isEmpty) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val msg = getMessage(arguments(0).head, exprEval)

      var map = new XdmMap()

      if (msg.isDefined) {
        val props: Map[QName,XdmValue] = msg.get match {
          case item: XProcItemMessage =>
            item.metadata.properties
          case _ =>
            Map.empty[QName,XdmItem]
        }

        for (key <- props.keySet) {
          val value = props(key)
          if (key.getNamespaceURI == "") {
            map = map.put(new XdmAtomicValue(key.getLocalName), value)
          } else {
            map = map.put(new XdmAtomicValue(key), value)
          }
        }

        map.getUnderlyingValue
      } else {
        logger.debug("p:document-properties called with an argument that isn't part of a document")
        map.getUnderlyingValue
      }
    }
  }
}
