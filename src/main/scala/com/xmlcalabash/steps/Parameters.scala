package com.xmlcalabash.steps

import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap}
import net.sf.saxon.s9api.{QName, XdmValue}

class Parameters() extends DefaultXmlStep {
  private var parameters = Map.empty[QName, XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: QName, value: XdmValue, context: StaticContext): Unit = {
    variable match {
      case XProcConstants._parameters => parameters = ValueParser.parseParameters(value, context)
      case _ =>
        logger.info("Ignoring unexpected option to p:parameters: " + variable)
    }
  }

  override def run(staticContext: StaticContext): Unit = {
    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(staticContext.baseURI)
    builder.addStartElement(XProcConstants.c_param_set)

    for ((name, value) <- parameters) {
      var attr: AttributeMap = EmptyAttributeMap.getInstance()
      attr = attr.put(TypeUtils.attributeInfo(XProcConstants._name, name.getLocalName))
      if (name.getNamespaceURI != null && name.getNamespaceURI != "") {
        attr = attr.put(TypeUtils.attributeInfo(XProcConstants._namespace, name.getNamespaceURI))
      } else {
        attr = attr.put(TypeUtils.attributeInfo(XProcConstants._namespace, ""))
      }

      // FIXME: this is not true in 3.0: XProc document property map values are strings
      var strvalue = ""
      val viter = value.iterator()
      while (viter.hasNext) {
        val item = viter.next()
        strvalue += item.getStringValue
      }
      attr = attr.put(TypeUtils.attributeInfo(XProcConstants._value, strvalue))

      builder.addStartElement(XProcConstants.c_param, attr)
      builder.addEndElement()
    }

    builder.addEndElement()
    builder.endDocument()

    consumer.get.receive("result", builder.result, new XProcMetadata(MediaType.XML))
  }


}
