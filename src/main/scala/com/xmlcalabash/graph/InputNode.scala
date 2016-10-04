package com.xmlcalabash.graph

import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage}

import scala.util.Random

/**
  * Created by ndw on 10/2/16.
  */
class InputNode(graph: Graph, name: Option[String]) extends Node(graph, name, None) {
  private var constructionOk = true
  private var seqNo: Long = 1

  override def addInput(port: String, edge: Option[Edge]): Unit = {
    graph.engine.staticError(None, "Cannot connect inputs to an InputNode")
    constructionOk = false
  }

  override def valid: Boolean = {
    super.valid && constructionOk
  }

  override def run(): Unit = {
    // do nothing
  }

  def write(item: GenericItem): Unit = {
    for (port <- outputs()) {
      val edge = output(port)
      val targetPort = edge.get.inputPort
      val targetNode = edge.get.destination

      val msg = new ItemMessage(targetPort, uid, seqNo, item)
      seqNo += 1

      println("R Sending msg to " + targetPort + " on " + targetNode + " (from " + this + ")")
      targetNode.actor ! msg
    }
  }

  def close(): Unit = {
    for (port <- outputs()) {
      val edge = output(port)
      val targetPort = edge.get.inputPort
      val targetNode = edge.get.destination

      val msg = new CloseMessage(targetPort)
      println("R Closing msg to " + targetPort + " on " + targetNode + " (from " + this + ")")
      targetNode.actor ! msg
    }
    actor ! new CloseMessage("result")
    actor ! new RanMessage(this)
  }
}
