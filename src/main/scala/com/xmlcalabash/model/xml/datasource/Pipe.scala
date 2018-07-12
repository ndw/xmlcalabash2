package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.containers.Catch
import com.xmlcalabash.model.xml.{Artifact, IOPort, Variable, WithOption}

import scala.collection.mutable.ListBuffer

class Pipe(override val config: XMLCalabash,
           override val parent: Option[Artifact]) extends DataSource(config, parent) {
  private var _step = Option.empty[String]
  private var _port = Option.empty[String]
  protected[xml] var priority = false

  def this(config: XMLCalabash, parent: Artifact, step: String) = {
    this(config, Some(parent))
    this.step = step
  }

  def this(config: XMLCalabash, parent: Artifact, step: Option[String]) = {
    this(config, Some(parent))
    _step = step
  }

  def this(config: XMLCalabash, parent: Artifact, step: String, port: String) = {
    this(config, Some(parent))
    this.step = step
    this.port = port
  }

  def this(config: XMLCalabash, parent: Artifact, step: Option[String], port: Option[String]) = {
    this(config, Some(parent))
    _step = step
    _port = port
  }

  def this(config: XMLCalabash, parent: Artifact, pipe: Pipe) = {
    this(config, Some(parent))
    _step = pipe.step
    _port = pipe.port
  }

  override def toString: String = {
    s"Pipe: ${step.get}.${port.get} to ${parent.get}"
  }

  def step: Option[String] = _step
  protected[xml] def step_=(name: String): Unit = {
    _step = Some(name)
  }

  def port: Option[String] = _port
  protected[xml] def port_=(name: String): Unit = {
    _port = Some(name)
  }

  override def patchPipe(fromName: String, fromPort: List[String], patchName: String, patchPort: String): Unit = {
    if (_step.get == fromName && fromPort.contains(_port.get)) {
      _step = Some(patchName)
      _port = Some(patchPort)
    }
  }

  override def validate(): Boolean = {
    _step = attributes.get(XProcConstants._step)
    _port = attributes.get(XProcConstants._port)

    val ncname = """([\p{L}_][-\p{L}_\p{N}]*)""".r
    if (_step.isDefined) {
      _step.get match {
        case ncname(label) => Unit
        case _ => throw new ModelException(ExceptionCode.INVALIDNAME, _step.get, location)
      }
    }

    if (_port.isDefined) {
      _port.get match {
        case ncname(label) => Unit
        case _ => throw new ModelException(ExceptionCode.INVALIDNAME, _port.get, location)
      }
    }

    for (key <- List(XProcConstants._port, XProcConstants._step)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    true
  }

  override def makeEdges(graph: Graph, parNode: Node): Unit = {
    val fromStep = findStep(step.get)
    val fromPort = port.get

    if (fromStep.isEmpty) {
      throw new ModelException(ExceptionCode.NOSTEP, step.get, location)
    }

    var toNode = Option.empty[Node]
    var toPort = ""

    parent.get match {
      case opt: WithOption =>
        toNode = opt._graphNode
        toPort = "source"
      case port: IOPort =>
        toNode = parent.get.parent.get._graphNode
        toPort = port.port.get
      case variable: Variable =>
        toNode = variable._graphNode
        toPort = "source"
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "p:pipe points to " + parent.get, location)
    }

    if (isAncestor(step.get)) {
      if (fromStep.get.input(fromPort).isEmpty) {
        var ok = false
        fromStep.get match {
          case katch: Catch => ok = (fromPort == "errors")
          case _ => Unit
        }
        if (!ok) {
          throw new ModelException(ExceptionCode.NOPORT, List(fromStep.get.name, fromPort), location)
        }
      }

    } else {
      if (fromStep.get.output(fromPort).isEmpty) {
        throw new ModelException(ExceptionCode.NOPORT, List(fromStep.get.name, fromPort), location)
      }
    }

    if (priority) {
      graph.addPriorityEdge(fromStep.get._graphNode.get, fromPort, toNode.get, toPort)
    } else {
      graph.addOrderedEdge(fromStep.get._graphNode.get, fromPort, toNode.get, toPort)
    }
  }

  override def asXML: xml.Elem = {
    dumpAttr("step", _step)
    dumpAttr("port", _port)

    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "pipe", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }

}
