package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ParserConfiguration, XProcConstants}
import com.xmlcalabash.model.xml.datasource.DataSource

import scala.collection.mutable.ListBuffer

class IOPort(override val config: XMLCalabash,
             override val parent: Option[Artifact]) extends Artifact(config, parent) {
  protected var _port: Option[String] = None
  protected var _sequence: Option[Boolean] = None
  protected var _primary: Option[Boolean] = None

  protected[xml] def this(config: XMLCalabash, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  def port: Option[String] = _port
  def port_=(port: String): Unit = {
    if (_port.isEmpty) {
      _port = Some(port)
    } else {
      throw new RuntimeException("Attempt to reset port name")
    }
  }

  def primary: Option[Boolean] = _primary
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
        throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
    }
  }

  override def validate(): Boolean = {
    _port = attributes.get(XProcConstants._port)
    _sequence = lexicalBoolean(attributes.get(XProcConstants._sequence))
    _primary = lexicalBoolean(attributes.get(XProcConstants._primary))

    if (_port.isEmpty) {
      throw new ModelException(ExceptionCode.PORTATTRREQ, this.toString, location)
    }

    true
  }

  override def makeEdges(graph: Graph, parent: Node) {
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
