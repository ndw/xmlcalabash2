package com.xmlcalabash.graph

import com.xmlcalabash.runtime.Step
import com.xmlcalabash.util.UniqueId

/**
  * Created by ndw on 10/3/16.
  */
private[graph] class FanOut(graph: Graph, name: Option[String], step: Step) extends Node(graph, name, Some(step)) {
  var portCount = 0

  def this(graph: Graph) {
    this(graph, Some("!fanout_" + UniqueId.nextId.toString), new Fan("fanout"))
  }

  def nextPort: Port = {
    portCount += 1
    new Port(this, "result_" + portCount.toString)
  }
}
