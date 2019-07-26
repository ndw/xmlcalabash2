package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

import scala.collection.JavaConverters._
import scala.collection.mutable

class SetProperties() extends DefaultXmlStep {
  private val _merge = new QName("merge")

  private var source: Any = _
  private var metadata: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      source = item
      this.metadata = metadata
    } else {
      throw new IllegalArgumentException(s"p:set-properties received input on port: $port")
    }
  }

  override def run(context: StaticContext): Unit = {
    val properties = mapBinding(XProcConstants._properties)

    val newprops = mutable.HashMap.empty[QName,XdmValue]

    if (booleanBinding(_merge).getOrElse(false)) {
      for ((name, value) <- metadata.properties) {
        newprops.put(name, value)
      }
    }

    for ((name,value) <- properties.asMap.asScala) {
      val qname = name.getQNameValue
      if (qname == XProcConstants._content_type) {
        throw XProcException.xcContentTypeNotAllowed(context.location)
      }
      newprops.put(qname, value)
    }

    val newmeta = new XProcMetadata(metadata.contentType, newprops.toMap)
    consumer.get.receive("result", source, newmeta)
  }
}
