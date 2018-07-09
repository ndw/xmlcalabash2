package com.xmlcalabash.steps

import java.net.URI

import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmItem, XdmValue}

class Parameters() extends DefaultXmlStep {
  private var parameters = Map.empty[QName, XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: QName, value: XdmValue, context: ExpressionContext): Unit = {
    variable match {
      case XProcConstants._parameters => parameters = ValueParser.parseParameters(value, context.nsBindings, context.location)
      case _ =>
        logger.info("Ignoring unexpected option to p:parameters: " + variable)
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(staticContext.baseURI)
    builder.addStartElement(XProcConstants.c_param_set)

    for ((name, value) <- parameters) {
      builder.addStartElement(XProcConstants.c_param)

      if (name.getNamespaceURI != "") {
        builder.addAttribute(XProcConstants._namespace, name.getNamespaceURI)
      }
      builder.addAttribute(XProcConstants._name, name.getLocalName)

      // XProc document property map values are strings
      var strvalue = ""
      val viter = value.iterator()
      while (viter.hasNext) {
        val item = viter.next()
        strvalue += item.getStringValue
      }
      builder.addAttribute(XProcConstants._value, strvalue)

      builder.addEndElement()
    }

    builder.addEndElement()
    builder.endDocument()

    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }


}
