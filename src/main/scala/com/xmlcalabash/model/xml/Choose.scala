package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, LoopStart, Node}
import com.xmlcalabash.core.XProcEngine
import com.xmlcalabash.runtime.{ChooseStep, ForEachStep}
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/4/16.
  */
class Choose(node: Option[XdmNode], parent: Option[Artifact]) extends CompoundStep(node, parent) {
  private[xml] var chooseStart: LoopStart = _

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

    val choose = new ChooseStep()
    chooseStart = graph.createIteratorNode(choose, subpipeline.toList)
  }
}
