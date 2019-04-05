package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.expr.XPathContext
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.{Item, Sequence}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}
import net.sf.saxon.value.QNameValue

class ForceQNameKeys(runtime: XMLCalabashConfig) extends FunctionImpl() {
  def call(context: XPathContext, arguments: Array[Sequence[_]]): Sequence[_] = {
    val exprEval = runtime.expressionEvaluator
    if (exprEval.dynContext.isEmpty) {
      throw XProcException.xiExtFunctionNotAllowed()
    }

    val inputMap = arguments(0).head match {
      case map: MapItem => map
      case _ => throw new RuntimeException("arg to fqk must be a map")
    }

    var map = new XdmMap()
    val iter = inputMap.keyValuePairs().iterator()
    while (iter.hasNext) {
      val pair = iter.next()
      pair.key.getItemType match {
        case BuiltInAtomicType.STRING =>
          val key = new QName("", "", pair.key.getStringValue)
          map = map.put(new XdmAtomicValue(key), XdmValue.wrap(pair.value))
        case BuiltInAtomicType.QNAME =>
          val qvalue = pair.key.asInstanceOf[QNameValue]
          val key = new QName(qvalue.getPrefix, qvalue.getNamespaceURI, qvalue.getLocalName)
          map = map.put(new XdmAtomicValue(key), XdmValue.wrap(pair.value))
        case _ =>
          // FIXME: not sure this works (given that it doesn't work for QNameValues
          map = map.put(pair.key.asInstanceOf[XdmAtomicValue], XdmValue.wrap(pair.value))
      }
    }

    map.getUnderlyingValue
  }
}
