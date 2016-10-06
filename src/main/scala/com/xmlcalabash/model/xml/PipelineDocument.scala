package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcEngine, XProcException}
import com.xmlcalabash.graph.{Graph, Node, XProcRuntime}
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/6/16.
  */
class PipelineDocument(node: Option[XdmNode], parent: Option[Artifact]) extends CompoundStep(node, parent) {
  override def buildGraph(graph: Graph): Unit = {
    val nodeMap = mutable.HashMap.empty[Artifact, Node]
    buildNodes(graph, nodeMap)
    buildEdges(graph, nodeMap)
  }
}

