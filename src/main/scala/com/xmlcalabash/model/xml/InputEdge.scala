package com.xmlcalabash.model.xml

import java.io.PrintWriter

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.XProcEngine
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/5/16.
  */
class InputEdge(val port: String, parent: Artifact) extends Step(None, Some(parent)) {
  _xmlname = "input-edge"

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val node = graph.createInputNode(port)
    nodeMap.put(this, node)
  }
}
