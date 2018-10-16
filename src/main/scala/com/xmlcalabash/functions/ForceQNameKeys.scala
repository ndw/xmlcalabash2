package com.xmlcalabash.functions

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.`type`.BuiltInAtomicType
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.ma.map.MapItem
import net.sf.saxon.om.{Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmMap, XdmValue}
import net.sf.saxon.value.{QNameValue, SequenceType}

class ForceQNameKeys private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "force-qname-keys")

  private var runtime: XMLCalabashConfig = _

  def this(runtime: XMLCalabashConfig) = {
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

      val inputMap = arguments(0).head match {
        case map: MapItem => map
        case _ => throw new RuntimeException("arg to fqk must be a map")
      }

      var map = new XdmMap()
      val iter = inputMap.iterator()
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
}
