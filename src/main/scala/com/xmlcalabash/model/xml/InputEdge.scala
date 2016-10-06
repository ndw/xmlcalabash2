package com.xmlcalabash.model.xml

import java.io.PrintWriter

import com.xmlcalabash.graph.{Graph, Node}
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/5/16.
  */
class InputEdge(val port: String, parent: Artifact) extends Step(None, Some(parent)) {
  _xmlname = "input-edge"

  override def buildNodes(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val node = graph.createInputNode(port)
    nodeMap.put(this, node)
  }
}
