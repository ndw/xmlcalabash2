package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import net.sf.saxon.expr.{StaticContext, XPathContext}
import net.sf.saxon.om.{Item, Sequence}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap, XdmValue}
import net.sf.saxon.value.AtomicValue

class DocumentProperties(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(staticContext: StaticContext, context: XPathContext, arguments: Array[Sequence]): AtomicValue = {
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

      // Is this right? It used to return just the underlying value but that's not an AtomicValue anymore?
      map.getUnderlyingValue.atomize().head()
    } else {
      logger.debug("p:document-properties called with an argument that isn't part of a document")
      map.getUnderlyingValue.atomize().head()
    }
  }

}
