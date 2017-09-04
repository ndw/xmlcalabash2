package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.{Artifact, Documentation, IOPort, PipeInfo, WithOption, XProcConstants}

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
    _step = attributes.get(XProcConstants._step)
    _port = attributes.get(XProcConstants._port)

    for (key <- List(XProcConstants._port, XProcConstants._step)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    valid
  }

  override def makeEdges(graph: Graph, parNode: Node): Unit = {
    val fromStep = findStep(step.get)
    val fromPort = port.get
    val toStep = parent.get.parent
    val toPort = parent.get match {
      case port: IOPort =>
        port.port.get
      case wopt: WithOption =>
        wopt.dataPort
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "p:pipe points to " + parent.get, location)
    }

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
