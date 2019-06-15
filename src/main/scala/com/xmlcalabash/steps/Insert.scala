package com.xmlcalabash.steps

import com.jafpl.steps.PortCardinality
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{ProcessMatch, ProcessMatchingNodes, StaticContext, XProcMetadata, XmlPortSpecification}
import net.sf.saxon.s9api.{QName, XdmNode, XdmNodeKind}

import scala.collection.mutable.ListBuffer

class Insert() extends DefaultXmlStep  with ProcessMatchingNodes {
  private val _position = new QName("", "position")
  private var source: XdmNode = _
  private var source_metadata: XProcMetadata = _
  private var insertion: ListBuffer[XdmNode] = ListBuffer.empty[XdmNode]
  private var pattern: String = _
  private var matcher: ProcessMatch = _
  private var position: String = _

  override def inputSpec: XmlPortSpecification = new XmlPortSpecification(
    Map("source" -> PortCardinality.EXACTLY_ONE, "insertion" -> PortCardinality.ONE_OR_MORE),
    Map("source" -> List("application/xml", "text/plain"), "insertion" -> List("application/xml", "text/plain"))
  )
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    if (port == "source") {
      source = item.asInstanceOf[XdmNode]
      source_metadata = metadata
    } else {
      insertion += item.asInstanceOf[XdmNode]
    }
  }

  override def run(context: StaticContext): Unit = {
    position = bindings(_position).getStringValue
    pattern = bindings(XProcConstants._match).getStringValue

    matcher = new ProcessMatch(config, this, context)
    matcher.process(source, pattern)

    consumer.get.receive("result", matcher.result, source_metadata)
  }

  override def startDocument(node: XdmNode): Boolean = {
    throw new RuntimeException("Cannot insert here")
  }

  override def startElement(node: XdmNode): Boolean = {
    if (position == "before") {
      doInsert()
    }

    matcher.addStartElement(node)
    matcher.addAttributes(node)
    matcher.startContent()

    if (position == "first-child") {
      doInsert()
    }

    true
  }

  override def endElement(node: XdmNode): Unit = {
    if (position == "last-child") {
      doInsert()
    }

    matcher.addEndElement()

    if (position == "after") {
      doInsert()
    }
  }

  override def endDocument(node: XdmNode): Unit = {
    matcher.addEndElement()
  }

  override def attribute(node: XdmNode): Unit = {
    throw new RuntimeException("Cannot insert at an attribute")
  }

  override def text(node: XdmNode): Unit = {
    process(node)
  }

  override def comment(node: XdmNode): Unit = {
    process(node)
  }

  override def pi(node: XdmNode): Unit = {
    process(node)
  }

  def doInsert(): Unit = {
    for (node <- insertion) {
      matcher.addSubtree(node)
    }
  }

  private def process(node: XdmNode): Unit = {
    if ("before" == position) {
      doInsert()
    }

    node.getNodeKind match {
      case XdmNodeKind.COMMENT => matcher.addComment(node.getStringValue)
      case XdmNodeKind.PROCESSING_INSTRUCTION => matcher.addPI(node.getNodeName.getLocalName, node.getStringValue)
      case XdmNodeKind.TEXT => matcher.addText(node.getStringValue)
      case _ => throw new IllegalArgumentException("What kind of node was that!?")

    }

    if ("after" == position) {
      doInsert()
    }

    if ("first-child" == position || "last-child" == position) {
      throw new RuntimeException("Can't do that")
    }
  }

}
