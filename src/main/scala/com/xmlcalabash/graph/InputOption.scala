package com.xmlcalabash.graph

import com.xmlcalabash.core.XProcException
import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage}
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/2/16.
  */
private[graph] class InputOption(graph: Graph, val optName: QName) extends Node(graph, None, None) {
  private var constructionOk = true
  private var seqNo: Long = 1
  private var initialized = false

  private[graph] override def addInput(port: String, edge: Option[Edge]): Unit = {
    constructionOk = false
    throw new GraphException("Cannot connect inputs to an InputOption")
  }

  private[graph] override def valid: Boolean = {
    super.valid && constructionOk
  }

  override private[graph] def run(): Unit = {
    // do nothing
  }

  def set(item: GenericItem): Unit = {
    if (initialized) {
      throw new XProcException("You cannot reinitialize an option")
    }

    for (port <- outputs()) {
      val edge = output(port)
      val targetPort = edge.get.inputPort
      val targetNode = edge.get.destination

      val msg = new ItemMessage(targetPort, uid, seqNo, item)
      seqNo += 1

      logger.debug("Input edge {} sends to {} on {}", this, targetPort, targetNode)
      targetNode.actor ! msg
    }

    initialized = true
    close()
  }

  private def close(): Unit = {
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
