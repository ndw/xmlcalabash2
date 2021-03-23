package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.runtime.SaxonExpressionEvaluator
import net.sf.saxon.expr.{StaticContext, XPathContext}
import net.sf.saxon.functions.AccessorFn.Component
import net.sf.saxon.om.{Item, Sequence}
import net.sf.saxon.s9api.{QName, XdmEmptySequence, XdmItem, XdmNode, XdmValue}
import net.sf.saxon.tree.iter.ArrayIterator
import net.sf.saxon.value.{QNameValue, StringValue}

class DocumentProperty(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(staticContext: StaticContext, context: XPathContext, arguments: Array[Sequence]): Sequence = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext.isEmpty) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    val msg = getMessage(arguments(0).head, exprEval)
    val prop: QName = arguments(1).head match {
      case pval: QNameValue =>
        new QName(pval.getComponent(Component.NAMESPACE).getStringValue, pval.getComponent(Component.LOCALNAME).getStringValue)
      case sval: StringValue =>
        val colonName = sval.getStringValue
        if (colonName.contains(":")) {
          val pfx = colonName.substring(0, colonName.indexOf(":"))
          val local = colonName.substring(colonName.indexOf(":")+1)
          val uri = Option(staticContext.getNamespaceResolver.getURIForPrefix(pfx, false))
          if (uri.isDefined) {
            new QName(pfx, uri.get, local)
          } else {
            throw XProcException.xdKeyIsInvalidQName(colonName, None)
          }
        } else {
          new QName("", colonName)
        }
      case _ =>
        throw new RuntimeException("Unexected argument to document property")
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
            // I wonder if there's a better way?
            val iter = new ArrayIterator[Item](Array(atomic.getUnderlyingValue))
            iter.materialize()
          case _ =>
            XdmEmptySequence.getInstance().getUnderlyingValue
        }
      } else {
        XdmEmptySequence.getInstance().getUnderlyingValue
      }
    }
  }
}
