package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ParserConfiguration, XProcConstants}
import com.xmlcalabash.model.xml.containers.Container

import scala.collection.mutable.ListBuffer

class Output(override val config: XMLCalabash,
             override val parent: Option[Artifact]) extends IOPort(config, parent) {
  protected[xml] def this(config: XMLCalabash, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    _port = attributes.get(XProcConstants._port)

    var attr = attributes.get(XProcConstants._primary)
    if (attr.isDefined) {
      attr.get match {
        case "true" => _primary = Some(true)
        case "false" => _primary = Some(false)
        case _ => throw new RuntimeException("primary must be true or false")
      }
    } else {
      _primary = None
    }

    attr = attributes.get(XProcConstants._sequence)
    if (attr.isDefined) {
      attr.get match {
        case "true" => _sequence = Some(true)
        case "false" => _sequence = Some(false)
        case _ => throw new RuntimeException("sequence must be true or false")
      }
    } else {
      _sequence = None
    }

    for (key <- List(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary)) {
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
        if (dataSourceClasses.contains(child.getClass)) {
          valid = valid && child.validate()
        } else {
          throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
        }
      }
    } else {
      if (children.nonEmpty) {
        throw new ModelException(ExceptionCode.BADCHILD, children.head.toString, location)
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
    new xml.Elem("p", "output", dump_attr.get, namespaceScope, false, nodes:_*)
  }

}
