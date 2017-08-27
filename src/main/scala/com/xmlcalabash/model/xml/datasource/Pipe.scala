package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.model.exceptions.ModelException
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.{Artifact, Documentation, IOPort, PipeInfo, XProcConstants}

import scala.collection.mutable.ListBuffer

class Pipe(override val config: ParserConfiguration,
           override val parent: Option[Artifact]) extends DataSource(config, parent) {
  private var _step = Option.empty[String]
  private var _port = Option.empty[String]

  def this(config: ParserConfiguration, parent: Artifact, step: String, port: String) = {
    this(config, Some(parent))
    _step = Some(step)
    _port = Some(port)
  }

  def step: Option[String] = _step
  protected[xml] def step_=(name: String): Unit = {
    _step = Some(name)
  }

  def port: Option[String] = _port
  protected[xml] def port_=(name: String): Unit = {
    _port = Some(name)
  }

  override def validate(): Boolean = {
    _step = properties.get(XProcConstants._step)
    _port = properties.get(XProcConstants._port)

    for (key <- List(XProcConstants._port, XProcConstants._step)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new ModelException("badopt", s"Unexpected attribute: ${key.getLocalName}")
    }

    valid
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    val fromStep = findStep(step.get)
    val fromPort = port.get
    val toStep = this.parent.get.parent
    val toPort = this.parent.get.asInstanceOf[IOPort].port.get

    graph.addEdge(fromStep.get.graphNode.get, fromPort,
      toStep.get.graphNode.get, toPort)
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
