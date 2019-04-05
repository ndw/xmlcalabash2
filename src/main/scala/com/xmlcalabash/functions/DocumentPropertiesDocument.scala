package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import net.sf.saxon.expr.{StaticContext, XPathContext}
import net.sf.saxon.om.{Item, Sequence}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmNode, XdmValue}

class DocumentPropertiesDocument(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(staticContext: StaticContext, context: XPathContext, arguments: Array[Sequence[_]]): Sequence[_] = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext.isEmpty) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    val msg = getMessage(arguments(0).head.asInstanceOf[Item[_]], exprEval)

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
