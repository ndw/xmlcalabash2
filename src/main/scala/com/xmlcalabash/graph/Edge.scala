package com.xmlcalabash.graph

/**
  * Created by ndw on 10/2/16.
  */
private[graph] class Edge(val graph: Graph, val source: Node, val outputPort: String, val destination: Node, val inputPort: String) {
  source.addOutput(outputPort, Some(this))
  destination.addInput(inputPort, Some(this))

  def this(graph: Graph, from: Port, to: Port) {
    this(graph, from.node, from.name, to.node, to.name)
  }
}
