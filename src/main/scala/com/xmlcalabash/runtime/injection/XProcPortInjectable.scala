package com.xmlcalabash.runtime.injection

import com.jafpl.injection.PortInjectable
import com.jafpl.messages.{BindingMessage, ItemMessage}
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.model.xml.Injectable
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XProcXPathExpression}
import com.xmlcalabash.util.{URIUtils, ValueUtils}

class XProcPortInjectable(injectable: Injectable) extends XProcInjectable(injectable) with PortInjectable {
  protected var _port: Option[String] = injectable.port

  def declPort: Option[String] = _port
  def declPort_=(port: String): Unit = {
    _port = Some(port)
  }

  def port: String = declPort.get

  override def run(context: ItemMessage): Unit = {
    val opts = new SaxonExpressionOptions(Map("inj.name" -> name, "inj.type" -> stepType, "inj.id" -> id))
    val nctx = new ExpressionContext(conditionXPath.context, opts)
    val nxpr = new XProcXPathExpression(nctx, conditionXPath.expr, conditionXPath.as, conditionXPath.values)
    val eval = config.expressionEvaluator
    val cond = eval.booleanValue(nxpr, List(context), bindings.toMap)

    logger.debug(s"Port injectable condition: ${conditionXPath.expr}=$cond")

    if (cond) {
      if (messageXPath.isDefined) {
        val result = eval.value(messageXPath.get, List(context), bindings.toMap)
        println(ValueUtils.stringValue(result.item))
      } else {
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(baseURI.getOrElse(URIUtils.cwdAsURI))
        builder.startContent()
        for (node <- messageNodes.get) {
          expandTVT(node, builder, List(context), ExpressionContext.NONE)
        }
        builder.endDocument()
        val result = builder.result
        println(result)
      }
    }
  }

  override def receiveBinding(message: BindingMessage): Unit = {
    bindings.put(message.name, message.message)
  }
}
