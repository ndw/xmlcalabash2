package com.xmlcalabash.model.xml.bindings

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.Artifact
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.runtime.Identity
import com.xmlcalabash.runtime.io.DocumentReader
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class Document(node: Option[XdmNode], parent: Option[Artifact]) extends Binding(node, parent) {
  val href = node.get.getAttributeValue(XProcConstants._href)

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    super.buildNodes(graph, engine, nodeMap)

    var name = this.toString
    val node = graph.createNode(new DocumentReader(engine, href))
    nodeMap.put(this, node)
  }

  override def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val srcNode = nodeMap(this)
    val srcPort = "result"
    val destNode = nodeMap(parent.get.parent.get)
    val destPort = parent.get.property(XProcConstants._port).get.value

    graph.addEdge(srcNode, srcPort, destNode, destPort)
  }

  override def dumpAdditionalAttributes(tree: TreeWriter): Unit = {
    tree.addAttribute(XProcConstants._href, href)
  }

}
