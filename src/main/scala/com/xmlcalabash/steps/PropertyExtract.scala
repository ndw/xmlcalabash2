package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{QName, XdmAtomicValue, XdmItem, XdmNode}

class PropertyExtract extends DefaultXmlStep {
  private var doc = Option.empty[Any]
  private var meta = Option.empty[XProcMetadata]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("result" -> PortCardinality.EXACTLY_ONE, "properties" -> PortCardinality.EXACTLY_ONE),
    Map("result" -> List("*"), "properties" -> List("application/xml")))

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    doc = Some(item)
    meta = Some(metadata)
  }

  override def run(staticContext: StaticContext) {
    consumer.get.receive("result", doc.get, meta.get)

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(None)
    builder.addStartElement(XProcConstants.c_document_properties)
    builder.addNamespace("xsi", XProcConstants.ns_xsi)
    builder.addNamespace("xs", XProcConstants.ns_xs)
    builder.startContent()
    for ((key,value) <- meta.get.properties) {
      builder.addStartElement(key)
      value match {
        case node: XdmNode =>
          builder.startContent()
          builder.addSubtree(node)
        case atomic: XdmAtomicValue =>
          val xtype = atomic.getTypeName
          if (xtype.getNamespaceURI == XProcConstants.ns_xs && (xtype != XProcConstants.xs_string)) {
            builder.addAttribute(XProcConstants.xsi_type, xtype.toString)
          }
          builder.startContent()
          builder.addValues(value)
        case _ =>
          throw XProcException.xiInvalidPropertyValue(value, location)
      }
      builder.addEndElement()
    }
    builder.addEndElement()
    builder.endDocument()

    consumer.get.receive("properties", builder.result, new XProcMetadata(MediaType.XML))
  }
}
