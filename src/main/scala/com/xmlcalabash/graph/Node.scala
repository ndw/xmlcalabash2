package com.xmlcalabash.graph

import akka.actor.{ActorRef, Props}
import com.xmlcalabash.messages.{CloseMessage, ItemMessage, RanMessage}
import com.xmlcalabash.runtime.{Identity, Step, StepController}
import Reaper.WatchMe
import com.xmlcalabash.core.XProcException
import com.xmlcalabash.items.GenericItem
import com.xmlcalabash.util.UniqueId

import scala.collection.{Set, immutable, mutable}

/**
  * Created by ndw on 10/2/16.
  */
class Node(val graph: Graph, val name: Option[String] = None, step: Option[Step]) extends StepController {
  protected val uid = UniqueId.nextId
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

  def addInput(port: String, edge: Option[Edge]): Unit = {
    if (inputPort.getOrElse(port, None).isDefined) {
      graph.engine.staticError(None, "Input port '" + port + "' already in use")
      constructionOk = false
    } else {
      inputPort.put(port, edge)
    }
  }

  def addOutput(port: String, edge: Option[Edge]): Unit = {
    if (outputPort.getOrElse(port, None).isDefined) {
      graph.engine.staticError(None, "Output port '" + port + "' already in use")
      constructionOk = false
    } else {
      outputPort.put(port, edge)
    }
  }

  def removeInput(port: String): Unit = {
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

  def valid = constructionOk
  def finished = _finished

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
      println("Sending msg to " + targetPort + " on " + targetNode + " (from " + this + ")")
      targetNode.actor ! msg
    } else {
      println("no downstream for " + port)
    }
  }

  def run(): Unit = {
    worker.get.run()

    for (port <- outputPort.keySet) {
      val edge = outputPort(port).get
      val targetPort = edge.inputPort
      val targetNode = edge.destination
      val msg = new CloseMessage(targetPort)
      println("Sending close to " + targetPort + " on " + targetNode + " (from " + this + ")")
      targetNode.actor ! msg
    }

    for (rest <- dependents) {
      if (!rest.finished) {
        rest.actor ! new RanMessage(this)
      }
    }

    println(name.get + " finished")
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

      _actor = graph.system.actorOf(Props(new NodeActor(this)), name.get)
      graph.reaper ! WatchMe(_actor)
    }
  }
}
