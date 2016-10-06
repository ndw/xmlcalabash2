package com.xmlcalabash.model.xml

import com.xmlcalabash.graph.{Graph, Node}
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/5/16.
  */
class OutputEdge(parent: Artifact) extends Step(None, Some(parent)) {
  _xmlname = "output-edge"

  override def buildNodes(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val node = graph.createOutputNode(this.toString)
    nodeMap.put(this, node)
  }
}
