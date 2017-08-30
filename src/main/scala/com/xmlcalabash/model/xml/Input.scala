package com.xmlcalabash.model.xml

import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.containers.Container
import com.xmlcalabash.model.xml.datasource.{DataSource, Pipe}

import scala.collection.mutable.ListBuffer

class Input(override val config: ParserConfiguration,
            override val parent: Option[Artifact]) extends IOPort(config, parent) {
  protected var _select: Option[String] = None

  protected[xml] def this(config: ParserConfiguration, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  override def validate(): Boolean = {
    super.validate()
    _select = properties.get(XProcConstants._select)

    for (key <- List(XProcConstants._port, XProcConstants._sequence,
      XProcConstants._primary, XProcConstants._select)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new ModelException("badopt", s"Unexpected attribute: ${key.getLocalName}")
    }

    if (parent.isDefined && parent.get.isInstanceOf[Container]) {
      for (child <- children) {
        child match {
          case ds: DataSource =>
            if (child.isInstanceOf[Pipe]) {
              throw new ModelException("nopipe", "Pipe not allowed here")
            }
            valid = valid && child.validate()
          case doc: Documentation => Unit
          case info: PipeInfo => Unit
          case _ =>
            throw new ModelException("badelem", s"Unexpected element: $child")
        }
      }
    } else {
      if (_sequence.isDefined) {
        throw new ModelException("noseq", "Sequence not allowed here")
      }
      if (_primary.isDefined) {
        throw new ModelException("noprim", "Primary not allowed here")
      }
      for (child <- children) {
        if (dataSourceClasses.contains(child.getClass)) {
          valid = valid && child.validate()
        } else {
          throw new ModelException("badelem", s"Unexpected element: $child")
        }
      }
    }

    valid
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
