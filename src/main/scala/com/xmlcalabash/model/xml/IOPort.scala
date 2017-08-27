package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.model.exceptions.ModelException
import com.xmlcalabash.model.xml.datasource.{DataSource, Pipe}

import scala.collection.mutable.ListBuffer

class IOPort(override val config: ParserConfiguration,
             override val parent: Option[Artifact]) extends Artifact(config, parent) {
  protected var _port: Option[String] = None
  protected var _sequence: Option[Boolean] = None
  protected var _primary: Option[Boolean] = None

  protected[xml] def this(config: ParserConfiguration, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  def port: Option[String] = _port

  def primary: Boolean = _primary.getOrElse(false)
  protected[xml] def primary_=(setPrimary: Boolean): Unit = {
    _primary = Some(setPrimary)
  }

  def sequence: Boolean = _sequence.getOrElse(false)
  protected[xml] def sequence_=(setSequence: Boolean): Unit = {
    _sequence = Some(setSequence)
  }

  def dataSources: List[DataSource] = {
    val list = ListBuffer.empty[DataSource]
    for (child <- children) {
      child match {
        case data: DataSource =>
          list += data
        case _ => Unit
      }
    }
    list.toList
  }

  override protected[xml] def addChild(child: Artifact): Unit = {
    child match {
      case data: DataSource =>
        super.addChild(data)
      case doc: Documentation =>
        super.addChild(doc)
      case info: PipeInfo =>
        super.addChild(info)
      case _ =>
        throw new ModelException("invalid", s"Invalid child of input: $child")
    }
  }

  override def validate(): Boolean = {
    _port = properties.get(XProcConstants._port)
    _sequence = lexicalBoolean(properties.get(XProcConstants._sequence))
    _primary = lexicalBoolean(properties.get(XProcConstants._primary))

    if (_port.isEmpty) {
      throw new ModelException("portreq", "Port is required")
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: ContainerStart) {
    // no direct contribution
  }

  override def makeEdges(graph: Graph, parent: ContainerStart) {
    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, parent)
      }
    }
  }
}
