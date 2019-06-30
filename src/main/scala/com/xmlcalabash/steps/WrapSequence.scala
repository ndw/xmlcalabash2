package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.{StepException, XProcException}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, ValueParser}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
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
      case xdm: XdmItem => throw XProcException.xiWrapItems(location)
      case _ => throw XProcException.xiWrapXML(location)
    }
  }

  override def run(staticContext: StaticContext) {
    var wrapper = new QName("", "INVALID")

    val name = bindings(_wrapper).getStringValue
    if (bindings.contains(_wrapper_prefix) || bindings.contains(_wrapper_namespace)) {
      if (name.contains(":")) {
        throw new RuntimeException("colon in error?")
      }
      if (bindings.contains(_wrapper_prefix)) {
        wrapper = new QName(bindings(_wrapper_prefix).getStringValue, bindings(_wrapper_namespace).getStringValue, name)
      } else {
        wrapper = new QName(bindings(_wrapper_namespace).getStringValue, name)
      }
    } else {
      val scontext = new StaticContext()
      scontext.inScopeNS = bindings(_wrapper).context.nsBindings
      if (location.isDefined) {
        scontext.location = location.get
      }
      wrapper = ValueParser.parseQName(name, scontext)
    }

    val builder = new SaxonTreeBuilder(config)
    builder.startDocument(staticContext.baseURI)
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
