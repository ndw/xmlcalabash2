package com.xmlcalabash.steps

import java.net.URI

import com.jafpl.exceptions.PipelineException
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.model.xml.XProcConstants
import com.xmlcalabash.runtime.{XmlMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmItem, XdmMap, XdmValue}

class Parameters() extends DefaultStep {
  private val _parameters = new QName("", "parameters")
  private var parameters = Map.empty[QName, XdmValue]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receiveBinding(variable: QName, value: XdmItem, nsBindings: Map[String,String]): Unit = {
    variable match {
      case `_parameters` => parameters = parseParameters(value, nsBindings)
      case _ =>
        logger.info("Ignoring unexpected option to p:parameters: " + variable)
    }
  }

  override def run(): Unit = {
    val builder = new SaxonTreeBuilder(config.get)
    builder.startDocument(URI.create("http://example.com/"))
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

    consumer.get.receive("result", builder.result, new XmlMetadata("application/xml"))
  }


}
