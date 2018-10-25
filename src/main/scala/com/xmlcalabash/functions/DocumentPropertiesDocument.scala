package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{NodeInfo, Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmNode, XdmValue}
import net.sf.saxon.value.SequenceType
import org.slf4j.{Logger, LoggerFactory}

class DocumentPropertiesDocument private extends ExtensionFunctionDefinition {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-properties-document")

  private var runtime: XMLCalabashConfig = _

  def this(runtime: XMLCalabashConfig) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_ITEM)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_NODE

  override def makeCallExpression(): ExtensionFunctionCall = {
    new DocPropsCall(this)
  }

  class DocPropsCall(val xdef: ExtensionFunctionDefinition) extends MessageAwareExtensionFunctionCall {
    var staticContext: StaticContext = _

    override def supplyStaticContext(context: StaticContext, locationId: Int, arguments: Array[Expression]): Unit = {
      staticContext = context
    }

    override def call(context: XPathContext, arguments: Array[Sequence]): Sequence = {
      val exprEval = runtime.expressionEvaluator
      if (exprEval.dynContext.isEmpty) {
        throw XProcException.xiExtFunctionNotAllowed()
      }

      val msg = getMessage(arguments(0).head, exprEval)

      val builder = new SaxonTreeBuilder(runtime)
      builder.startDocument(None)
      builder.addStartElement(XProcConstants.c_document_properties)
      builder.addNamespace("xsi", XProcConstants.ns_xsi)
      builder.addNamespace("xs", XProcConstants.ns_xs)
      builder.startContent()

      if (msg.isEmpty) {
        logger.debug("p:document-properties-document called with an argument that isn't part of a document")
        builder.endDocument()
        builder.result.getUnderlyingNode
      } else {
        val props: Map[QName,XdmValue] = msg.get match {
          case item: XProcItemMessage =>
            item.metadata.properties
          case _ =>
            Map.empty[QName,XdmItem]
        }

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
            case value: XdmValue =>
              builder.startContent()
              builder.addValues(value)
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
}
