package com.xmlcalabash.graph

import akka.actor.{ActorRef, Props}
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage}
import com.xmlcalabash.runtime.{Identity, Step, StepController}
import Reaper.WatchMe
import com.xmlcalabash.core.{XProcConstants, XProcException}
import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.util.UniqueId
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

import scala.collection.{Set, immutable, mutable}

/**
  * Created by ndw on 10/2/16.
  */
class Node(val graph: Graph, val name: Option[String] = None, step: Option[Step]) extends StepController {
  protected val uid = UniqueId.nextId
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val inputPort = mutable.HashMap.empty[String, Option[Edge]]
  private val outputPort = mutable.HashMap.empty[String, Option[Edge]]
  private val sequenceNos = mutable.HashMap.empty[String, Long]
  private val dependents = mutable.HashSet.empty[Node]
  private var constructionOk = true
  private val actors = mutable.HashMap.empty[String, ActorRef]
  private var _actor: ActorRef = _
  private var madeActors = false
  private var _finished = false
  private val worker = step

  private[graph] val dependsOn = mutable.HashSet.empty[Node]
  private[graph] def actor = _actor

  def inputs(): Set[String] = {
    inputPort.keySet
  }

  def input(port: String): Option[Edge] = {
    inputPort.getOrElse(port, None)
  }

  def outputs(): Set[String] = {
    outputPort.keySet
  }

  def output(port: String): Option[Edge] = {
    outputPort.getOrElse(port, None)
  }

  private[graph] def addInput(port: String, edge: Option[Edge]): Unit = {
    if (inputPort.getOrElse(port, None).isDefined) {
      graph.engine.staticError(None, "Input port '" + port + "' already in use")
      constructionOk = false
    } else {
      inputPort.put(port, edge)
    }
  }

  private[graph] def addOutput(port: String, edge: Option[Edge]): Unit = {
    if (outputPort.getOrElse(port, None).isDefined) {
      graph.engine.staticError(None, "Output port '" + port + "' already in use")
      constructionOk = false
    } else {
      outputPort.put(port, edge)
    }
  }

  private[graph] def removeInput(port: String): Unit = {
    if (inputPort.getOrElse(port, None).isDefined) {
      inputPort.remove(port)
    } else {
      graph.engine.staticError(None, "Attempt to remove unconnected input")
      constructionOk = false
    }
  }

  def removeOutput(port: String): Unit = {
    if (outputPort.getOrElse(port, None).isDefined) {
      outputPort.remove(port)
    } else {
      graph.engine.staticError(None, "Attempt to remove unconnected output")
      constructionOk = false
    }
  }

  def addDependancy(node: Node): Unit = {
    dependsOn.add(node)
    node.addDependent(this)
  }

  private def addDependent(node: Node): Unit = {
    dependents.add(node)
  }

  private[graph] def valid = constructionOk
  private[graph] def finished = _finished

  def noCycles(seen: immutable.HashSet[Node]): Boolean = {
    if (seen.contains(this)) {
      graph.engine.staticError(None, "Graph contains a cycle!")
      false
    } else {
      var valid = true
      for (edge <- outputPort.values) {
        if (valid && edge.isDefined) {
          valid = valid && edge.get.destination.noCycles(seen + this)
        }
      }
      for (depends <- dependsOn) {
        valid = valid && depends.noCycles(seen + this)
      }
      valid
    }
  }

  def connected(): Boolean = {
    var valid = true
    for (port <- inputPort.keySet) {
      if (inputPort.get(port).isEmpty) {
        graph.engine.staticError(None, "Unconnected input port")
        valid = false
      }
    }
    for (port <- outputPort.keySet) {
      if (outputPort.get(port).isEmpty) {
        graph.engine.staticError(None, "Unconnected output port")
        valid = false
      }
    }
    valid
  }

  override def toString: String = {
    if (name.isDefined) {
      "[Node: " + name.get + " " + uid.toString + "]"
    } else {
      "[Node: " + uid.toString + "]"
    }
  }

  def receive(port: String, msg: ItemMessage): Unit = {
    worker.get.receive(port, msg)
  }

  def send(port: String, item: GenericItem): Unit = {
    if (outputPort.get(port).isDefined) {
      val edge = output(port).get
      val targetPort = edge.inputPort
      val targetNode = edge.destination

      var seqNo: Long = 1
      if (sequenceNos.get(port).isDefined) {
        seqNo = sequenceNos(port) + 1
      }
      sequenceNos.put(port, seqNo)

      val msg = new ItemMessage(targetPort, uid, seqNo, item)
      logger.debug("Node {} sends to {} on {}", this, targetPort, targetNode)
      targetNode.actor ! msg
    } else {
      throw new XProcException("no downstream for " + port)
    }
  }

  private[graph] def run(): Unit = {
    worker.get.run()

    for (port <- outputPort.keySet) {
      val edge = outputPort(port).get
      val targetPort = edge.inputPort
      val targetNode = edge.destination
      val msg = new CloseMessage(targetPort)
      logger.debug("Node {} closes {} on {}", this, targetPort, targetNode)
      targetNode.actor ! msg
    }

    for (rest <- dependents) {
      if (!rest.finished) {
        rest.actor ! new RanMessage(this)
      }
    }

    logger.debug("Node {} finishes", this)
    _finished = true

  }

  private[graph] def makeActors(): Unit = {
    for (port <- outputPort.keySet) {
      val edge = outputPort.getOrElse(port, None)
      if (edge.isDefined) {
        edge.get.destination.makeActors()
      }
    }

    if (!madeActors) {
      madeActors = true
      //println("Making actors for " + this)

      if (worker.isDefined) {
        if (!worker.get.init(this, inputs(), outputs(), Set()))
          throw new XProcException("Failed to initialize worker step.")
      }

      var actorName = name
      if (actorName.isEmpty) {
        actorName = Some("anon" + UniqueId.nextId)
      }

      _actor = graph.system.actorOf(Props(new NodeActor(this)), actorName.get)
      graph.reaper ! WatchMe(_actor)
    }
  }

  def dump(tree: TreeWriter): Unit = {
    tree.addStartElement(XProcConstants.pg("node"))
    if (name.isDefined) {
      tree.addAttribute(XProcConstants._name, name.get)
    }
    tree.addAttribute(XProcConstants._uid, uid.toString)

    if (inputs().nonEmpty) {
      tree.addStartElement(XProcConstants.pg("inputs"))
      for (portName <- inputs()) {
        val port = inputPort(portName)
        if (port.isDefined) {
          tree.addStartElement(XProcConstants.pg("in-edge"))
          tree.addAttribute(new QName("", "source"), port.get.source.uid.toString)
          tree.addAttribute(new QName("", "input-port"), port.get.inputPort)
          tree.addAttribute(new QName("", "output-port"), port.get.outputPort)
          tree.addEndElement()
        }
      }
      tree.addEndElement()
    }

    if (outputs().nonEmpty) {
      tree.addStartElement(XProcConstants.pg("outputs"))
      for (portName <- outputs()) {
        val port = outputPort(portName)
        if (port.isDefined) {
          tree.addStartElement(XProcConstants.pg("out-edge"))
          tree.addAttribute(new QName("", "input-port"), port.get.inputPort)
          tree.addAttribute(new QName("", "destination"), port.get.destination.uid.toString)
          tree.addAttribute(new QName("", "output-port"), port.get.outputPort)
          tree.addEndElement()
        }
      }
      tree.addEndElement()
    }

    tree.addEndElement()
  }
}
