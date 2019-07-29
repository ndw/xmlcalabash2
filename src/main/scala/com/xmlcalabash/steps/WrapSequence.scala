package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode}

import scala.collection.mutable.ListBuffer

class WrapSequence extends DefaultXmlStep {
  private val _wrapper = new QName("", "wrapper")
  private val _group_adjacent = new QName("", "group-adjacent")

  private val inputs = ListBuffer.empty[XdmItem]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case xdm: XdmNode => inputs += xdm
      case xdm: XdmItem => inputs += xdm
      case _ => throw XProcException.xiWrapXML(location)
    }
  }

  override def run(staticContext: StaticContext) {
    val wrapper = qnameBinding(_wrapper).get

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(staticContext.baseURI)
    builder.addStartElement(wrapper)
    builder.startContent()
    for (item <- inputs) {
      item match {
        case xdm: XdmNode => builder.addSubtree(item.asInstanceOf[XdmNode])
        case xdm: XdmItem => builder.addText(xdm.getStringValue)
      }
    }
    builder.addEndElement()
    builder.endDocument()

    consumer.get.receive("result", builder.result, XProcMetadata.XML)
  }

  override def reset(): Unit = {
    super.reset()
    inputs.clear()
  }
}
