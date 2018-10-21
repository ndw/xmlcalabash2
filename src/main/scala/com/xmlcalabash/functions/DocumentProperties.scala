package com.xmlcalabash.functions

import java.net.URI

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XProcItemMessage
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.expr.{Expression, StaticContext, XPathContext}
import net.sf.saxon.lib.{ExtensionFunctionCall, ExtensionFunctionDefinition}
import net.sf.saxon.om.{NodeInfo, Sequence, StructuredQName}
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmMap, XdmValue}
import net.sf.saxon.value.SequenceType
import org.slf4j.{Logger, LoggerFactory}

class DocumentProperties private extends ExtensionFunctionDefinition {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private val funcname = new StructuredQName("p", XProcConstants.ns_p, "document-properties")

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

      // Walk up the tree if we get passed some descendant
      var arg = arguments(0).head
      var doc: NodeInfo = null
      var done = false

      while (!done) {
        arg match {
          case node: NodeInfo =>
            if (node.getParent == null) {
              doc = node
              done = true
            } else {
              arg = node.getParent
            }
          case _ =>
            done = true
        }
      }

      var map = new XdmMap()

      if (doc == null) {
        logger.debug("p:document-properties called with an argument that isn't part of a document")
        map.getUnderlyingValue
      } else {
        val msg = exprEval.dynContext.get.message(doc)
        if (msg.isEmpty) {
          val baseURI = doc match {
            case ni: NodeInfo => ni.getBaseURI
            case _ => ""
          }
          logger.debug(s"p:document-properties-document called with an unknown document: $baseURI")
          map.getUnderlyingValue
        } else {
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

          map.getUnderlyingValue
        }
      }
    }
  }
}
