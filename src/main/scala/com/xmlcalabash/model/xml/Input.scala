package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ParserConfiguration, XProcConstants}
import com.xmlcalabash.model.xml.containers.Container
import com.xmlcalabash.model.xml.datasource.{DataSource, Pipe}

import scala.collection.mutable.ListBuffer

class Input(override val config: XMLCalabash,
            override val parent: Option[Artifact]) extends IOPort(config, parent) {
  protected var _select: Option[String] = None

  protected[xml] def this(config: XMLCalabash, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  override def validate(): Boolean = {
    super.validate()
    _select = attributes.get(XProcConstants._select)

    for (key <- List(XProcConstants._port, XProcConstants._sequence,
      XProcConstants._primary, XProcConstants._select)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    if (parent.isDefined && parent.get.isInstanceOf[Container]) {
      for (child <- children) {
        child match {
          case ds: DataSource =>
            if (child.isInstanceOf[Pipe]) {
              throw new ModelException(ExceptionCode.BADPIPE, this.toString, location)
            }
            valid = valid && child.validate()
          case doc: Documentation => Unit
          case info: PipeInfo => Unit
          case _ =>
            throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
        }
      }
    } else {
      if (_sequence.isDefined) {
        throw new ModelException(ExceptionCode.BADSEQ, "sequence", location)
      }
      if (_primary.isDefined) {
        throw new ModelException(ExceptionCode.BADPRIMARY, "primary", location)
      }
      for (child <- children) {
        if (dataSourceClasses.contains(child.getClass)) {
          valid = valid && child.validate()
        } else {
          throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
        }
      }
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    // Process the children in the context of our parent
    for (child <- children) {
      child.makeGraph(graph, parent)
    }
  }

  override def asXML: xml.Elem = {
    dumpAttr("port", _port)
    dumpAttr("sequence", _sequence)
    dumpAttr("primary", _primary)
    dumpAttr("id", id.toString)

    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "input", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }

}
