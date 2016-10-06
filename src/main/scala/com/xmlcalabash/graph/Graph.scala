package com.xmlcalabash.graph

import akka.actor.{ActorRef, ActorSystem, Props}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.messages.{CloseMessage, StartMessage}
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.runtime.Step
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable
import scala.collection.immutable

/**
  * Created by ndw on 10/2/16.
  */
class Graph(private[graph] val engine: XProcEngine) {
  private val nodes = mutable.HashSet.empty[Node]
  private val edges = mutable.HashSet.empty[Edge]
  private var _finished = false
  private var _system: ActorSystem = _
  private var _reaper: ActorRef = _

  def finished = _finished
  private[graph] def system = _system
  private[graph] def reaper = _reaper

  def createNode(name: String, step: Step): Node = {
    val node = new Node(this, Some(name), Some(step))
    nodes.add(node)
    node
  }

  def createInputNode(name: String): InputNode = {
    val node = new InputNode(this, Some(name))
    nodes.add(node)
    node
  }

  def createOutputNode(name: String): OutputNode = {
    val node = new OutputNode(this, Some(name))
    nodes.add(node)
    node
  }

  def addEdge(from: Port, to: Port): Unit = {
    addEdge(from.node, from.name, to.node, to.name)
  }

  def addEdge(source: Node, outputPort: String, destination: Node, inputPort: String): Unit = {
    val from =
      if (source.output(outputPort).isDefined) {
        val edge = source.output(outputPort).get
        edge.source match {
        case n: FanOut => n.nextPort
        case n: Node =>
          val fanOut = new FanOut(this)
          nodes.add(fanOut)
          edge.source.removeOutput(edge.outputPort)
          edge.destination.removeInput(edge.inputPort)
          edges.remove(edge)
          addEdge(source, outputPort, fanOut, "source")
          val targetPort = new Port(edge.destination, edge.inputPort)
          addEdge(fanOut.nextPort, targetPort)
          fanOut.nextPort
      }
    } else {
        new Port(source, outputPort)
      }
    val to =
      if (destination.input(inputPort).isDefined) {
        val edge = destination.input(inputPort).get
        edge.destination match {
          case n: FanIn => n.nextPort
          case n: Node =>
            val fanIn = new FanIn(this)
            nodes.add(fanIn)
            edge.source.removeOutput(edge.outputPort)
            edge.destination.removeInput(edge.inputPort)
            edges.remove(edge)
            addEdge(fanIn, "result", destination, inputPort)
            val sourcePort = new Port(edge.source, edge.outputPort)
            addEdge(sourcePort, fanIn.nextPort)
            fanIn.nextPort
        }
      } else {
        new Port(destination, inputPort)
      }
    val edge = new Edge(this, from, to)
    edges.add(edge)
  }

  def addDependency(node: Node, dependsOn: Node): Unit = {
    node.addDependancy(dependsOn)
  }

  private[graph] def finish(): Unit = {
    _finished = true
  }

  def valid(): Boolean = {
    var valid = true
    for (node <- nodes) {
      valid = valid && node.valid
      valid = valid && node.noCycles(immutable.HashSet.empty[Node])
      valid = valid && node.connected()
    }
    valid
  }

  private def roots(): Set[Node] = {
    val roots = mutable.HashSet.empty[Node]
    for (node <- nodes) {
      if (node.inputs().isEmpty) {
        roots.add(node)
      }
    }
    roots.toSet
  }

  def inputs(): Set[InputNode] = {
    val inodes = mutable.HashSet.empty[InputNode]
    for (node <- nodes) {
      node match {
        case n: InputNode => inodes += n
        case _ => Unit
      }
    }
    inodes.toSet
  }

  def outputs(): Set[OutputNode] = {
    val onodes = mutable.HashSet.empty[OutputNode]
    for (node <- nodes) {
      node match {
        case n: OutputNode => onodes += n
        case _ => Unit
      }
    }
    onodes.toSet
  }

  def makeActors(): Unit = {
    _system = ActorSystem("XMLCalabashPipeline")
    _reaper = _system.actorOf(Props(new ProductionReaper(this)), name="reaper")

    for (node <- roots()) {
      node.makeActors()
    }

    // Run all the roots that aren't InputNodes or OutputNodes
    for (root <- roots()) {
      root match {
        case node: InputNode => Unit
        case node: OutputNode => Unit
        case node: Node =>
          node.actor ! new StartMessage()
      }
    }
  }

  def dump(): XdmNode = {
    val tree = new TreeWriter(engine)
    tree.startDocument(null)
    tree.addStartElement(XProcConstants.pg("graph"))
    for (node <- nodes) {
      node.dump(tree)
    }
    tree.addEndElement()
    tree.endDocument()
    tree.getResult
  }
}
