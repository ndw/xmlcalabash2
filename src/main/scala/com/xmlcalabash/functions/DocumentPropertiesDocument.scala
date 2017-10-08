package com.xmlcalabash.functions

import java.net.URI

import com.jafpl.messages.ItemMessage
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, XProcMetadata}
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{NodeInfo, Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode}
import net.sf.saxon.value.SequenceType

class DocumentPropertiesDocument private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-properties-document")

  private var runtime: XMLCalabash = _

  def this(runtime: XMLCalabash) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_NODE)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_NODE

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
      val msg = exprEval.dynContext.get.message(doc.asInstanceOf[NodeInfo])
      if (msg.isEmpty) {
        throw XProcException.xiDocPropsUnavail(exprEval.dynContext.get.location, new URI(doc.asInstanceOf[NodeInfo].getBaseURI))
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

      val builder = new SaxonTreeBuilder(runtime)
      builder.startDocument(None)
      builder.addStartElement(XProcConstants.p_document_properties)
      builder.startContent()
      for (key <- props.keySet) {
        builder.addStartElement(key)
        builder.startContent()
        props(key) match {
          case node: XdmNode => builder.addSubtree(node)
          case atomic: XdmItem => builder.addText(atomic.getStringValue)
        }
        builder.addEndElement()
      }
      builder.addEndElement()
      builder.endDocument()

      builder.result.getUnderlyingNode
    }
  }
}
