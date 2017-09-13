package com.xmlcalabash.functions

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.ItemMessage
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{SaxonExpressionEvaluator, XProcMetadata}
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{NodeInfo, Sequence, StructuredQName}
import net.sf.saxon.value.{SequenceType, StringValue}

class DocumentProperties private extends ExtensionFunctionDefinition {
  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-properties")

  private var runtime: XMLCalabash = _

  def this(runtime: XMLCalabash) = {
    this()
    this.runtime = runtime
  }

  override def getFunctionQName: StructuredQName = funcname

  override def getArgumentTypes: Array[SequenceType] = Array(SequenceType.SINGLE_NODE, SequenceType.SINGLE_STRING)

  override def getResultType(suppliedArgumentTypes: Array[SequenceType]): SequenceType = SequenceType.SINGLE_ATOMIC

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
        throw new PipelineException("notallowed", s"You cannot call the XProc extension function $funcname here", None)
      }

      val doc = arguments(0).head
      val prop = arguments(1).head.getStringValue
      val msg = exprEval.dynContext.get.message(doc.asInstanceOf[NodeInfo])

      if (msg.isEmpty) {
        throw new PipelineException("noprops", s"Properties requested for a document that has no properties", None)
      }

      val props: Map[String,String] = msg.get match {
        case item: ItemMessage =>
          item.metadata match {
            case xml: XProcMetadata =>
              xml.properties
            case _ =>
              Map.empty[String,String]
          }
        case _ =>
          Map.empty[String,String]
      }

      val value = props.getOrElse(prop, "")
      new StringValue(value)
    }
  }
}
