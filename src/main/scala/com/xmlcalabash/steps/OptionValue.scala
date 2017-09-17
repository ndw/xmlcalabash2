package com.xmlcalabash.steps

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode}

class OptionValue extends DefaultXmlStep {
  var value = Option.empty[XdmItem]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: QName, value: XdmItem, context: ExpressionContext): Unit = {
    this.value = Some(value)
  }

  override def run(staticContext: StaticContext) {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_result)
    builder.startContent()
    value.get match {
      case node: XdmNode => builder.addSubtree(node)
      case _ => builder.addText(value.get.getStringValue)
    }
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata("application/xml"))
  }
}
