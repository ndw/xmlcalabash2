package com.xmlcalabash.functions

import java.net.URI

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, XProcMetadata}
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.functions.AccessorFn.Component
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{NodeInfo, Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmNode, XdmValue}
import net.sf.saxon.value.{QNameValue, SequenceType, StringValue}

class DocumentProperty private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-property")

  private var runtime: XMLCalabash = _

  def this(runtime: XMLCalabash) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_NODE, SequenceType.SINGLE_ATOMIC)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ITEM

  override def makeCallExpression(): ExtensionFunctionCall = {
    new DocPropsCall(this)
  }

  class DocPropsCall(val xdef: ExtensionFunctionDefinition) extends ExtensionFunctionCall {
    var staticContext: StaticContext = _

    override def supplyStaticContext(context: StaticContext, locationId: Int, arguments: Array[Expression]): Unit = {
      staticContext = context
    }

    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator.asInstanceOf[SaxonExpressionEvaluator]
      if (exprEval.dynContext.isEmpty) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val doc = arguments(0).head
      val x = arguments(1).head
      val prop: QName = arguments(1).head match {
        case pval: QNameValue =>
          new QName(pval.getComponent(Component.NAMESPACE).getStringValue, pval.getComponent(Component.LOCALNAME).getStringValue)
        case sval: StringValue =>
          new QName("", sval.getStringValue)
        case _ =>
          throw XProcException.dynamicError(44, arguments(1).head.toString, exprEval.dynContext.get.location)
      }

      val msg = exprEval.dynContext.get.message(doc.asInstanceOf[NodeInfo])
      if (msg.isEmpty) {
        throw XProcException.xiDocPropsUnavail(exprEval.dynContext.get.location, new URI(doc.asInstanceOf[NodeInfo].getBaseURI))
      }

      val props: Map[QName,XdmValue] = msg.get match {
        case item: ItemMessage =>
          item.metadata match {
            case xml: XProcMetadata =>
              xml.properties
            case _ =>
              Map.empty[QName,XdmItem]
          }
        case _ =>
          Map.empty[QName,XdmItem]
      }

      val value = props.getOrElse(prop, new XdmAtomicValue(""))
      value match {
        case node: XdmNode =>
          node.getUnderlyingNode
        case atomic: XdmItem =>
          atomic.getUnderlyingValue
      }
    }
  }
}
