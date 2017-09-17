package com.xmlcalabash.steps

import java.net.URI

import com.jafpl.messages.Message
import com.xmlcalabash.exceptions.StepException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser}
import com.xmlcalabash.runtime.{XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmItem, XdmNode}

import scala.collection.mutable.ListBuffer

class WrapSequence extends DefaultXmlStep {
  private val _wrapper = new QName("", "wrapper")
  private val _wrapper_prefix = new QName("", "wrapper-prefix")
  private val _wrapper_namespace = new QName("", "wrapper-namespace")
  private val _group_adjacent = new QName("", "group-adjacent")

  private val inputs = ListBuffer.empty[XdmItem]

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    item match {
      case xdm: XdmNode => inputs += xdm
      case xdm: XdmItem => throw new RuntimeException("Sequence of items isn't supported yet")
      case _ => throw new RuntimeException("Only XML is allowed to WrapSequence")
    }
  }

  override def run() {
    var wrapper = new QName("", "INVALID")

    val name = bindings(_wrapper).value.getStringValue
    if (bindings.contains(_wrapper_prefix) || bindings.contains(_wrapper_namespace)) {
      if (name.contains(":")) {
        throw StepException.dynamicError(34)
      }
      if (bindings.contains(_wrapper_prefix)) {
        wrapper = new QName(bindings(_wrapper_prefix).value.getStringValue, bindings(_wrapper_namespace).value.getStringValue, name)
      } else {
        wrapper = new QName(bindings(_wrapper_namespace).value.getStringValue, name)
      }
    } else {
      wrapper = ValueParser.parseQName(name, bindings(_wrapper).context.nsBindings)
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(URI.create("http://example.com/"))
    builder.addStartElement(wrapper)
    builder.startContent()
    for (item <- inputs) {
      builder.addSubtree(item.asInstanceOf[XdmNode])
    }
    builder.addEndElement()
    builder.endDocument()
    consumer.get.receive("result", builder.result, XProcMetadata.XML)
  }
}
