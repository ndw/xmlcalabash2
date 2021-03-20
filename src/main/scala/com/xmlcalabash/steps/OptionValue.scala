package com.xmlcalabash.steps

import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

class OptionValue extends DefaultXmlStep {
  var value = Option.empty[XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    this.value = Some(value)
  }

  override def run(staticContext: StaticContext): Unit = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_result)
    builder.startContent()
    val iter = value.get.iterator()
    while (iter.hasNext) {
      val item = iter.next()
      item match {
        case node: XdmNode => builder.addSubtree(node)
        case _ => builder.addText(item.getStringValue)
      }
    }
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }
}
