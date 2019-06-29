package com.xmlcalabash.runtime.injection

import java.util.Calendar

import com.jafpl.injection.StepInjectable
import com.jafpl.messages.BindingMessage
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.model.xml.Injectable
import com.xmlcalabash.runtime.{ExpressionContext, SaxonExpressionOptions, XProcXPathExpression}
import com.xmlcalabash.util.URIUtils

class XProcStepInjectable(injectable: Injectable) extends XProcInjectable(injectable) with StepInjectable {
  var startTime: Long = _
  var elapsed: Double = _

  override def beforeRun(): Unit = {
    startTime = Calendar.getInstance().getTimeInMillis
    if (injectable.itype == XProcConstants.p_start) {
      run()
    }
  }

  override def afterRun(): Unit = {
    elapsed = (Calendar.getInstance().getTimeInMillis - startTime) / 1000.0
    if (injectable.itype == XProcConstants.p_end) {
      run()
    }
  }

  def run(): Unit = {
    val opts = new SaxonExpressionOptions(Map("inj.elapsed" -> elapsed, "inj.name" -> name,
      "inj.type" -> stepType, "inj.id" -> id))
    val nctx = new ExpressionContext(conditionXPath.context, opts)
    val nxpr = new XProcXPathExpression(nctx, conditionXPath.expr, conditionXPath.as, conditionXPath.values)
    val eval = config.expressionEvaluator
    val cond = eval.booleanValue(nxpr, List(), bindings.toMap)

    logger.debug(s"Step injectable condition: ${conditionXPath.expr}=$cond")

    if (cond) {
      if (messageXPath.isDefined) {
        val result = eval.value(messageXPath.get, List(), bindings.toMap)
        var s = ""
        val iter = result.item.iterator()
        while (iter.hasNext) {
          s += iter.next.getStringValue
        }
        println(s)
      } else {
        val builder = new SaxonTreeBuilder(config)
        builder.startDocument(baseURI.getOrElse(URIUtils.cwdAsURI))
        builder.startContent()
        for (node <- messageNodes.get) {
          expandTVT(node, builder, List(), ExpressionContext.NONE)
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
