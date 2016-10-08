package com.xmlcalabash.graph

import scala.collection.mutable

/**
  * Created by ndw on 10/2/16.
  */
class LoopStart(graph: Graph, name: Option[String], val loopEnd: LoopEnd) extends Node(graph, name, None) {
  val nodes = mutable.ListBuffer.empty[Node]

  def addNode(node: Node): Unit = {
    nodes += node
  }

  override def addIterationCaches(): Unit = {
    for (child <- nodes) {
      child.addIterationCaches()
    }

    for (child <- nodes) {
      for (input <- child.inputs()) {
        val edge = child.input(input).get
        val node = edge.source
        var found = (node == this)
        for (cnode <- nodes) {
          found = found || node == cnode
        }
        if (!found) {
          // Cache me Amadeus
          logger.debug("Add cache  : " + edge)
          val cache = graph.createIterationCacheNode()
          graph.removeEdge(edge)
          graph.addEdge(edge.source, edge.outputPort, cache, "source")
          graph.addEdge(cache, "result", edge.destination, edge.inputPort)
        }
      }
    }
  }
}
