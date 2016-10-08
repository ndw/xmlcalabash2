package com.xmlcalabash.graph

import akka.actor.{ActorRef, ActorSystem, Props}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.messages.{CloseMessage, StartMessage}
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.runtime.Step
import com.xmlcalabash.util.UniqueId
import net.sf.saxon.s9api.{QName, XdmNode}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.immutable

/**
  * Created by ndw on 10/2/16.
  */
class Graph(private[graph] val engine: XProcEngine) {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  private val nodes = mutable.HashSet.empty[Node]
  private val fans = mutable.HashMap.empty[Node, Node]
  private val edges = mutable.HashSet.empty[Edge]
  private var _validated = false
  private var _valid = true
  private var _finished = false
  private var _system: ActorSystem = _
  private var _reaper: ActorRef = _

  private[graph] def finished = _finished
  private[graph] def system = _system
  private[graph] def reaper = _reaper

  def chkValid() = {
    if (_validated) {
      throw new GraphException("Attempt to change validated graph")
    }
  }

  def createNode(name: String): Node = {
    chkValid()
    val node = new Node(this, Some(name), None)
    nodes.add(node)
    node
  }

  def createNode(name: String, step: Step): Node = {
    chkValid()
    val node = new Node(this, Some(name), Some(step))
    nodes.add(node)
    node
  }

  def createInputOption(name: QName): InputOption = {
    chkValid()
    val node = new InputOption(this, name)
    nodes.add(node)
    node
  }

  def createInputNode(name: String): InputNode = {
    chkValid()
    val node = new InputNode(this, Some(name))
    nodes.add(node)
    node
  }

  def createOutputNode(name: String): OutputNode = {
    chkValid()
    val node = new OutputNode(this, Some(name))
    nodes.add(node)
    node
  }

  def createVariableNode(name: QName, step: Step): Node = {
    chkValid()
    val node = new Node(this, Some("var_" + UniqueId.nextId), Some(step))
    nodes.add(node)
    node
  }

  def createLoopNode(): LoopStart = {
    chkValid()
    val loopEnd = new LoopEnd(this, Some("loop_end_" + UniqueId.nextId))
    val loopStart = new LoopStart(this, Some("loop_start_" + UniqueId.nextId), loopEnd)
    nodes.add(loopStart)
    nodes.add(loopEnd)
    loopStart
  }

  private[graph] def createIterationCacheNode(): IterationCache = {
    val node = new IterationCache(this, Some("i_cache_" + UniqueId.nextId))
    nodes.add(node)
    node
  }

  def addEdge(from: Port, to: Port): Unit = {
    chkValid()
    addEdge(from.node, from.name, to.node, to.name)
  }

  def addEdge(source: Node, outputPort: String, destination: Node, inputPort: String): Unit = {
    chkValid()

    val from =
      if (source.output(outputPort).isDefined) {
        val edge = source.output(outputPort).get

        if (fans.contains(edge.source)) {
          val fanOut = fans(edge.source).asInstanceOf[FanOut]
          fanOut.nextPort
        } else {
          logger.debug("Fanout: " + source + "." + outputPort)
          val fanOut = new FanOut(this)
          nodes.add(fanOut)
          fans.put(edge.source, fanOut)

          removeEdge(edge)
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

        if (fans.contains(edge.destination)) {
          val fanIn = fans(edge.destination).asInstanceOf[FanIn]
          fanIn.nextPort
        } else {
          logger.debug("Fanin: " + destination + "." + inputPort)
          val fanIn = new FanIn(this)
          nodes.add(fanIn)
          fans.put(edge.destination, fanIn)
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

  private[graph] def removeEdge(edge: Edge): Unit = {
    edge.source.removeOutput(edge.outputPort)
    edge.destination.removeInput(edge.inputPort)
    edges.remove(edge)
  }

  def addDependency(node: Node, dependsOn: Node): Unit = {
    chkValid()
    node.addDependancy(dependsOn)
  }

  private[graph] def finish(): Unit = {
    _finished = true
  }

  def valid(): Boolean = {
    if (_validated) {
      return _valid
    }

    val srcPorts = mutable.HashSet.empty[Port]
    val dstPorts = mutable.HashSet.empty[Port]
    for (edge <- edges) {
      val src = new Port(edge.source, edge.outputPort)
      val dst = new Port(edge.destination, edge.inputPort)

      srcPorts += src
      dstPorts += dst

      if (srcPorts.contains(dst)) {
        _valid = false
        throw new GraphException("Attempt to write to an input port: " + dst)
      }

      if (dstPorts.contains(src)) {
        _valid = false
        throw new GraphException("Attempt to read to an output port: " + dst)
      }
    }

    for (node <- nodes) {
      _valid = _valid && node.valid
      _valid = _valid && node.noCycles(immutable.HashSet.empty[Node])
      _valid = _valid && node.connected()
    }

    if (_valid) {
      for (node <- nodes) {
        node.addIterationCaches()
      }
    }

    _validated = true
    _valid
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

  private[graph] def inputs(): List[InputNode] = {
    val inodes = mutable.ListBuffer.empty[InputNode]
    for (node <- nodes) {
      node match {
        case n: InputNode => inodes += n
        case _ => Unit
      }
    }
    inodes.toList
  }

  private [graph] def options(): List[InputOption] = {
    val onodes = mutable.ListBuffer.empty[InputOption]
    for (node <- nodes) {
      node match {
        case n: InputOption => onodes += n
        case _ => Unit
      }
    }
    onodes.toList
  }

  private[graph] def outputs(): List[OutputNode] = {
    val onodes = mutable.ListBuffer.empty[OutputNode]
    for (node <- nodes) {
      node match {
        case n: OutputNode => onodes += n
        case _ => Unit
      }
    }
    onodes.toList
  }

  private[graph] def makeActors(): Unit = {
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
