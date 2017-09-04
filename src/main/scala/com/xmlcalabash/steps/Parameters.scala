package com.xmlcalabash.steps

import java.net.URI

import com.jafpl.exceptions.PipelineException
import com.xmlcalabash.model.util.SaxonTreeBuilder
import com.xmlcalabash.model.xml.XProcConstants
import com.xmlcalabash.runtime.{XmlMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmMap}

class Parameters() extends DefaultStep {
  private val _parameters = new QName("", "parameters")
  override def inputSpec: XmlPortSpecification = XmlPortSpecification.NONE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def run(): Unit = {
    val builder = new SaxonTreeBuilder(config.get)
    builder.startDocument(URI.create("http://example.com/"))
    builder.addStartElement(XProcConstants.c_param_set)

    if (bindings.contains(_parameters)) {
      val value = bindings(_parameters)
      value match {
        case map: XdmMap =>
          // Grovel through a Java Map
          val iter = map.keySet().iterator()
          while (iter.hasNext) {
            val key = iter.next()
            val value = map.get(key)

            // XProc document property map values are strings
            var strvalue = ""
            val viter = value.iterator()
            while (viter.hasNext) {
              val item = viter.next()
              strvalue += item.getStringValue
            }

            builder.addStartElement(XProcConstants.c_param)
            builder.addAttribute(XProcConstants._name, key.getStringValue)
            builder.addAttribute(XProcConstants._value, strvalue)
            builder.addEndElement()
          }

        case _ =>
          throw new PipelineException("notmap", "parameters property is not a map", None)
      }
    }

    builder.addEndElement()
    builder.endDocument()

    consumer.get.receive("result", builder.result, new XmlMetadata("application/xml"))
  }


}
