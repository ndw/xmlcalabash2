package com.xmlcalabash.runtime.injection

import com.jafpl.injection.PortInjectable
import com.jafpl.messages.{BindingMessage, ItemMessage}
import com.xmlcalabash.messages.XPathItemMessage
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.model.xml.Injectable
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions}
import com.xmlcalabash.util.URIUtils

class XProcPortInjectable(injectable: Injectable) extends XProcInjectable(injectable) with PortInjectable {
  protected var _port: Option[String] = injectable.port

  def declPort: Option[String] = _port
  def declPort_=(port: String): Unit = {
    _port = Some(port)
  }

  def port: String = declPort.get

  override def run(context: ItemMessage): Unit = {
    val opts = new SaxonExpressionOptions(Map("inj.name" -> name, "inj.type" -> stepType, "inj.id" -> id))
    val eval = config.expressionEvaluator
    val cond = eval.booleanValue(conditionXPath, List(context), bindings.toMap, Some(opts))

    logger.debug(s"Port injectable condition: ${conditionXPath.expr}=$cond")

    if (cond) {
      if (messageXPath.isDefined) {
        val result = eval.value(messageXPath.get, List(context), bindings.toMap, Some(opts))
        var s = ""
        for (ritem <- result) {
          val item = ritem.asInstanceOf[XPathItemMessage]
          s += item.item.getStringValue
        }
        println(s)
      } else {
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(baseURI.getOrElse(URIUtils.cwdAsURI))
        builder.startContent()
        for (node <- messageNodes.get) {
          expandTVT(node, builder, List(context), ExpressionContext.NONE, Some(opts))
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
