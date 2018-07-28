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
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmNode}
import net.sf.saxon.value.SequenceType

class DocumentPropertiesDocument private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-properties-document")

  private var runtime: XMLCalabash = _

  def this(runtime: XMLCalabash) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_ITEM)

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

      val builder = new SaxonTreeBuilder(runtime)
      builder.startDocument(None)
      builder.addStartElement(XProcConstants.p_document_properties)
      builder.addNamespace("xsi", XProcConstants.ns_xsi)
      builder.addNamespace("xs", XProcConstants.ns_xs)
      builder.startContent()
      for (key <- props.keySet) {
        builder.addText("\n  ")
        builder.addStartElement(key)
        props(key) match {
          case node: XdmNode =>
            builder.startContent()
            builder.addSubtree(node)
          case atomic: XdmAtomicValue =>
            val xtype = atomic.getTypeName
            if (xtype.getNamespaceURI == XProcConstants.ns_xs && (xtype != XProcConstants.xs_string)) {
              builder.addAttribute(XProcConstants.xsi_type, xtype.toString)
            }
            builder.startContent()
            builder.addText(atomic.getStringValue)
          case item: XdmItem =>
            builder.startContent()
            builder.addText(item.getStringValue)
        }
        builder.addEndElement()
      }
      builder.addText("\n")
      builder.addEndElement()
      builder.endDocument()

      builder.result.getUnderlyingNode
    }
  }
}
