package com.xmlcalabash.graph

import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/2/16.
  */
private[graph] class Edge(val graph: Graph, val source: Node, val outputPort: String, val destination: Node, val inputPort: String) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  source.addOutput(outputPort, Some(this))
  destination.addInput(inputPort, Some(this))
  logger.debug("Create edge: " + this)

  def this(graph: Graph, from: Port, to: Port) {
    this(graph, from.node, from.name, to.node, to.name)
  }

  override def toString: String = {
    source + "." + outputPort + " → " + destination + "." + inputPort
  }
}
