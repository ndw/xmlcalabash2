package com.xmlcalabash.steps.text

import com.jafpl.messages.Message
import com.xmlcalabash.messages.XdmNodeItemMessage
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XProcXPathExpression, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import net.sf.saxon.s9api.{QName, XdmNode}

class Replace() extends DefaultXmlStep {
  private val _pattern = new QName("", "pattern")
  private val _replacement = new QName("", "replacement")
  private val _flags = new QName("", "flags")
  private var text: XdmNode = _
  private var meta: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.TEXTSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.TEXTRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case node: XdmNode => text = node
      case _ => throw new RuntimeException("non node to text?")
    }
    meta = metadata
  }

  override def run(context: StaticContext): Unit = {
    val pattern = bindings(_pattern).getStringValue.replace("'", "''")
    val replacement = bindings(_replacement).getStringValue.replace("'", "''")
    val flags = if (bindings.contains(_flags)) {
      Some(bindings(_flags).getStringValue)
    } else {
      None
    }

    val evaluator = config.expressionEvaluator
    val replexpr = if (flags.isDefined) {
      s"replace(., '$pattern', '$replacement', '${flags.get}')"
    } else {
      s"replace(., '$pattern', '$replacement')"
    }
    val expr = new XProcXPathExpression(ExpressionContext.NONE, replexpr)
    val context = new XdmNodeItemMessage(text, meta)

    val repl = evaluator.singletonValue(expr, List(context), Map.empty[String,Message], None)

    consumer.get.receive("result", repl.item, meta)
  }

}
