package com.xmlcalabash.graph

import akka.event.Logging
import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage}
import org.slf4j.LoggerFactory

import scala.util.Random

/**
  * Created by ndw on 10/2/16.
  */
class InputNode(graph: Graph, name: Option[String]) extends Node(graph, name, None) {
  private var constructionOk = true
  private var seqNo: Long = 1

  private[graph] override def addInput(port: String, edge: Option[Edge]): Unit = {
    constructionOk = false
    throw new GraphException("Cannot connect inputs to an InputNode")
  }

  private[graph] override def valid: Boolean = {
    super.valid && constructionOk
  }

  override private[graph] def run(): Unit = {
    // do nothing
  }

  def write(item: GenericItem): Unit = {
    for (port <- outputs()) {
      val edge = output(port)
      val targetPort = edge.get.inputPort
      val targetNode = edge.get.destination

      val msg = new ItemMessage(targetPort, uid, seqNo, item)
      seqNo += 1

      logger.debug("Input edge {} sends to {} on {}", this, targetPort, targetNode)
      targetNode.actor ! msg
    }
  }

  def close(): Unit = {
    for (port <- outputs()) {
      val edge = output(port)
      val targetPort = edge.get.inputPort
      val targetNode = edge.get.destination

      val msg = new CloseMessage(targetPort)

      logger.debug("Input edge {} closes {} on {}", this, targetPort, targetNode)
      targetNode.actor ! msg
    }
    actor ! new CloseMessage("result")
    actor ! new RanMessage(this)
  }
}
