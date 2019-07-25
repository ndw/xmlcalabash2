package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.{HashUtils, TypeUtils}
import net.sf.saxon.s9api.{QName, XdmNode}

class Uuid() extends DefaultXmlStep  with ProcessMatchingNodes {
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var uuid: String = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULT

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    this.metadata = metadata
  }

  override def run(context: StaticContext): Unit = {
    val version = integerBinding(XProcConstants._version)
    if (version.isEmpty || version.get == 4) {
      val id = java.util.UUID.randomUUID
      uuid = id.toString
    } else {
      throw XProcException.xcUnsupportedUuidVersion(version.get, location)
    }

    pattern = stringBinding(XProcConstants._match)
    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    val result = matcher.result
    consumer.get.receive("result", result, checkMetadata(result, metadata))
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
    false
  }

  override def startElement(node: XdmNode): Boolean = {
    matcher.addText(uuid)
    false
  }

  override def endElement(node: XdmNode): Unit = {
    // nop
  }

  override def endDocument(node: XdmNode): Unit = {
    matcher.endDocument()
  }

  override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = false

  override def attribute(node: XdmNode): Unit = {
    matcher.addAttribute(node, uuid)
  }

  override def text(node: XdmNode): Unit = {
    matcher.addText(uuid)
  }

  override def comment(node: XdmNode): Unit = {
    matcher.addText(uuid)
  }

  override def pi(node: XdmNode): Unit = {
    matcher.addText(uuid)
  }
}
