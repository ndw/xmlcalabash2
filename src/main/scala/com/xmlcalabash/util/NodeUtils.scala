package com.xmlcalabash.util

import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.model.xml.util.RelevantNodes
import net.sf.saxon.Controller
import net.sf.saxon.event.{NamespaceReducer, TreeReceiver}
import net.sf.saxon.expr.instruct.Executable
import net.sf.saxon.om.Item
import net.sf.saxon.s9api.{Axis, XdmDestination, XdmNode, XdmNodeKind}

/**
  * Created by ndw on 10/1/16.
  */
object NodeUtils {
  def getDocumentElement(doc: XdmNode): Option[XdmNode] = {
    if (doc.getNodeKind eq XdmNodeKind.DOCUMENT) {
      for (item <- RelevantNodes.filter(doc, Axis.CHILD, ignore = true)) {
        val node = item.asInstanceOf[XdmNode]
        if (node.getNodeKind eq XdmNodeKind.ELEMENT) {
          return Some(node)
        }
      }
      None
    } else {
      Some(doc)
    }
  }

  def nodesToTree(config: XProcEngine, nodes: List[XdmNode]): XdmNode = {
    var element: XdmNode = null
    for (node <- nodes) {
      if (node.getNodeKind eq XdmNodeKind.ELEMENT) {
        if (element != null) {
          throw new UnsupportedOperationException("two elements?")
        }
        element = node
      }
    }
    if (element == null) {
      throw new UnsupportedOperationException("no elements?")
    }
    val controller = new Controller(config.processor.getUnderlyingConfiguration)
    val exec = new Executable(controller.getConfiguration)
    val destination = new XdmDestination()
    val receiver = new NamespaceReducer(destination.getReceiver(controller.getConfiguration))
    val pipe = controller.makePipelineConfiguration()
    receiver.setPipelineConfiguration(pipe)
    receiver.setSystemId(element.getBaseURI.toASCIIString)
    val tree = new TreeReceiver(receiver)
    tree.open()
    tree.startDocument(0)
    for (value <- nodes) {
      tree.append(value.getUnderlyingValue.asInstanceOf[Item])
    }
    tree.endDocument()
    tree.close()
    destination.getXdmNode
  }
}
