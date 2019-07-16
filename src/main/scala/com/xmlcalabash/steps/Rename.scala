package com.xmlcalabash.steps

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.{XdmNodeItemMessage, XdmValueItemMessage}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime._
import net.sf.saxon.s9api.{Axis, QName, XdmAtomicValue, XdmNode}
import net.sf.saxon.value.QNameValue

class Rename() extends DefaultXmlStep with ProcessMatchingNodes {
  private val _new_name = new QName("new-name")

  private var context: StaticContext = _
  private var newName: QName = _
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.XMLSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULT

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val qn = bindings(_new_name).value.getUnderlyingValue.asInstanceOf[QNameValue]
    newName = new QName(qn.getPrefix, qn.getNamespaceURI, qn.getLocalName)
    pattern = bindings(XProcConstants._match).getStringValue
    this.context = context

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def startElement(node: XdmNode): Boolean = {
    matcher.addStartElement(newName)
    matcher.addAttributes(node)
    matcher.startContent()
    true
  }

  override def endElement(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def endDocument(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "document", location)
  }

  override def allAttributes(node: XdmNode, matching: List[XdmNode]): Boolean = {
    val iter = node.axisIterator(Axis.ATTRIBUTE)
    while (iter.hasNext) {
      val attr = iter.next()

      // If we're renaming A to B,
      // 1. Don't output A
      // 2. Don't output any existing attribute named B
      if (matching.contains(attr) || attr.getNodeName == newName) {
        // nop
      } else {
        matcher.addAttribute(attr)
      }
    }

    matcher.addAttribute(newName, matching.head.getStringValue)

    false
  }

  override def attribute(node: XdmNode): Unit = {
    throw new RuntimeException("attribute called in rename")
  }

  override def text(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "text", location)
  }

  override def comment(node: XdmNode): Unit = {
    throw XProcException.xcInvalidSelection(pattern, "comment", location)
  }

  override def pi(node: XdmNode): Unit = {
    if (newName.getNamespaceURI != "") {
      throw XProcException.xcBadRenamePI(newName, location)
    }
    matcher.addPI(newName.getLocalName, node.getStringValue)
  }
}
