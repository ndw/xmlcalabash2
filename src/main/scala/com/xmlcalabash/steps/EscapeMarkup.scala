package com.xmlcalabash.steps

import java.io.StringWriter

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.{StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, XdmMap, XdmNode, XdmNodeKind}

class EscapeMarkup() extends DefaultXmlStep {
  private var source: XdmNode = _
  private var metadata: XProcMetadata = _

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCESEQ

  override def outputSpec: XmlPortSpecification = XmlPortSpecification.ANYRESULTSEQ

  override def receive(port: String, item: Any, meta: XProcMetadata): Unit = {
    source = item.asInstanceOf[XdmNode]
    metadata = meta
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val options = if (bindings.contains(XProcConstants._serialization)) {
      bindings(XProcConstants._serialization).value.asInstanceOf[XdmMap]
    } else {
      new XdmMap()
    }

    val tree = new SaxonTreeBuilder(config)
    tree.startDocument(source.getBaseURI)

    val serializer = makeSerializer(options)

    var topLevelElement = false
    val iter = source.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next()
      child.getNodeKind match {
        case XdmNodeKind.COMMENT => tree.addComment(child.getStringValue)
        case XdmNodeKind.PROCESSING_INSTRUCTION => tree.addPI(child.getNodeName.getLocalName, child.getStringValue)
        case XdmNodeKind.TEXT => tree.addText(child.getStringValue)
        case _ => // ELEMENT
          if (topLevelElement) {
            throw XProcException.xcMultipleTopLevelElements(location)
          }
          topLevelElement = true

          tree.addStartElement(child)
          tree.addAttributes(child)
          tree.startContent()

          val sw = new StringWriter()
          serializer.setOutputWriter(sw)

          for (cnode <- S9Api.axis(child, Axis.CHILD)) {
            S9Api.serialize(config.config, cnode, serializer)
          }

          var data = sw.toString
          data = data.replaceAll("^<.*?>", "")     // Strip off the start tag...
          data = data.replaceAll("<[^<>]*?>$", "") // Strip off the end tag

          tree.addText(data)
          tree.addEndElement()
      }
    }

    tree.endDocument()
    consumer.get.receive("result", tree.result, metadata)
  }
}
