package com.xmlcalabash.steps

import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmNode}

class Delete() extends DefaultXmlStep  with ProcessMatchingNodes {
  private val _position = new QName("", "position")
  private var source: XdmNode = _
  private var source_metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    source_metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    pattern = bindings(XProcConstants._match).getStringValue

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, source_metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    false
  }

  override def startElement(node: XdmNode): Boolean = {
    false
  }

  override def endElement(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def endDocument(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def attribute(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def text(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def comment(node: XdmNode): Unit = {
    // nop, deleted
  }

  override def pi(node: XdmNode): Unit = {
    // nop, deleted
  }
}
