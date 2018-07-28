package com.xmlcalabash.functions

import java.net.URI

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, XProcMetadata}
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{NodeInfo, Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap}
import net.sf.saxon.value.SequenceType

class DocumentProperties private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-properties")

  private var runtime: XMLCalabash = _

  def this(runtime: XMLCalabash) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_ITEM)

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
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext.isEmpty) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val doc = arguments(0).head
      val msg = exprEval.dynContext.get.message(doc)
      if (msg.isEmpty) {
        val baseURI = doc match {
          case ni: NodeInfo => ni.getBaseURI
          case _ => ""
        }
        throw XProcException.xiDocPropsUnavail(exprEval.dynContext.get.location, new URI(baseURI))
      }

      val props: Map[QName,XdmItem] = msg.get match {
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

      var map = new XdmMap()
      for (key <- props.keySet) {
        val value = props(key)
        if (key.getNamespaceURI == "") {
          map = map.put(new XdmAtomicValue(key.getLocalName), value)
        } else {
          map = map.put(new XdmAtomicValue(key), value)
        }
      }

      map.getUnderlyingValue
    }
  }
}
