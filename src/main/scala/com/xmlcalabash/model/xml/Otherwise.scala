package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node, WhenStart}
import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.model.xml.util.WhenOrOtherwise
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/4/16.
  */
class Otherwise(node: Option[XdmNode], parent: Option[Artifact]) extends WhenOrOtherwise(node, parent) {
  private[xml] var otherwiseStart: WhenStart = _

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val subpipeline = ListBuffer.empty[Node]

    val childMap = mutable.HashMap.empty[Artifact, Node]
    for (child <- children) {
      child.buildNodes(graph, engine, childMap)
    }
    for (art <- childMap.keySet) {
      val node = childMap(art)
      subpipeline += node
      nodeMap.put(art, node)
    }

    otherwiseStart = graph.createWhenNode(subpipeline.toList)
  }

}
