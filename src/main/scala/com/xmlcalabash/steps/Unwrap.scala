package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{Axis, XdmNode, XdmNodeKind}

class Unwrap() extends DefaultXmlStep  with ProcessMatchingNodes {
  private var source: XdmNode = _
  private var source_metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    source_metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    pattern = stringBinding(XProcConstants._match)

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    consumer.get.receive("result", result, checkMetadata(result, source_metadata))
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode): Boolean = {
    true
  }

  override def endElement(node: XdmNode): Unit = {
    // nop
  }

  override def endDocument(node: XdmNode): Unit = {
    // nop
  }

  override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = true

  override def attribute(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "attribute", location)
  }

  override def text(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "text", location)
  }

  override def comment(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "comment", location)
  }

  override def pi(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "processing-instruction", location)
  }
}
