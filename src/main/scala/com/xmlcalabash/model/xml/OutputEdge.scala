package com.xmlcalabash.model.xml

import com.xmlcalabash.graph.{Graph, Node}
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/5/16.
  */
class OutputEdge(val port: String, parent: Artifact) extends Step(None, Some(parent)) {
  _xmlname = "output-edge"

  override def buildNodes(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val node = graph.createOutputNode(port)
    nodeMap.put(this, node)
  }
}
