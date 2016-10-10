package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, LoopStart, Node}
import com.xmlcalabash.runtime.ForEachStep
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/4/16.
  */
class ForEach(node: Option[XdmNode], parent: Option[Artifact]) extends CompoundStep(node, parent) {
  private[xml] var loopStart: LoopStart = _

  override def primaryInputPort: Option[InputOrOutput] = {
    var primary: Option[InputOrOutput] = None

    for (child <- _children) {
      child match {
        case is: IterationSource =>
          primary = Some(is)
        case _ => Unit
      }
    }

    if (primary.isEmpty) {
      logger.info("Missing iteration-source on for-each?")
    }

    primary
  }

  override def buildNodes(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val subpipeline = ListBuffer.empty[Node]

    val childMap = mutable.HashMap.empty[Artifact, Node]
    for (child <- children) {
      child.buildNodes(graph, childMap)
    }
    for (art <- childMap.keySet) {
      val node = childMap(art)
      subpipeline += node
      nodeMap.put(art, node)
    }

    val forEach = new ForEachStep(this.toString)
    loopStart = graph.createIteratorNode(forEach, subpipeline.toList)

  }
}
