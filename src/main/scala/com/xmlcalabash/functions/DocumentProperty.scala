package com.xmlcalabash.functions

import java.net.URI

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.SaxonExpressionEvaluator
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.functions.AccessorFn.Component
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{NodeInfo, Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmEmptySequence, XdmItem, XdmNode, XdmValue}
import net.sf.saxon.value.{QNameValue, SequenceType, StringValue}

class DocumentProperty private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-property")

  private var runtime: XMLCalabashConfig = _

  def this(runtime: XMLCalabashConfig) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_NODE, SequenceType.SINGLE_ATOMIC)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.OPTIONAL_ITEM

  override def makeCallExpression(): ExtensionFunctionCall = {
    new DocPropsCall(this)
  }

  class DocPropsCall(val xdef: ExtensionFunctionDefinition) extends MessageAwareExtensionFunctionCall {
    var staticContext: StaticContext = _

    override def supplyStaticContext(context: StaticContext, locationId: Int, arguments: Array[Expression]): Unit = {
      staticContext = context
    }

    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
      if (exprEval.dynContext.isEmpty) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val msg = getMessage(arguments(0).head, exprEval)
      val prop: QName = arguments(1).head match {
        case pval: QNameValue =>
          new QName(pval.getComponent(Component.NAMESPACE).getStringValue, pval.getComponent(Component.LOCALNAME).getStringValue)
        case sval: StringValue =>
          new QName("", sval.getStringValue)
        case _ =>
          throw XProcException.dynamicError(44, arguments(1).head.toString, exprEval.dynContext.get.location)
      }

      if (msg.isEmpty) {
        XdmEmptySequence.getInstance().getUnderlyingValue
      } else {
        val properties: Map[QName,XdmValue] = msg.get match {
          case item: XProcItemMessage =>
            item.metadata.properties
          case _ =>
            Map.empty[QName,XdmItem]
        }

        if (properties.contains(prop)) {
          properties(prop) match {
            case node: XdmNode =>
              node.getUnderlyingNode
            case atomic: XdmItem =>
              atomic.getUnderlyingValue
            case _ =>
              XdmEmptySequence.getInstance().getUnderlyingValue
          }
        } else {
          XdmEmptySequence.getInstance().getUnderlyingValue
        }
      }
    }
  }
}
