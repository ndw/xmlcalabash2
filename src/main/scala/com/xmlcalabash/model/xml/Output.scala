package com.xmlcalabash.model.xml

import com.xmlcalabash.model.exceptions.ModelException
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.containers.Container

import scala.collection.mutable.ListBuffer

class Output(override val config: ParserConfiguration,
             override val parent: Option[Artifact]) extends IOPort(config, parent) {
  protected[xml] def this(config: ParserConfiguration, parent: Artifact, port: String, primary: Boolean, sequence: Boolean) {
    this(config, Some(parent))
    _port = Some(port)
    _primary = Some(primary)
    _sequence = Some(sequence)
  }

  override def validate(): Boolean = {
    super.validate()

    for (key <- List(XProcConstants._port, XProcConstants._sequence, XProcConstants._primary)) {
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
        if (dataSourceClasses.contains(child.getClass)) {
          valid = valid && child.validate()
        } else {
          throw new ModelException("badelem", s"Unexpected element: ${child}")
        }
      }
    } else {
      if (children.nonEmpty) {
        throw new ModelException("badelem", s"Unexpected element: ${children.head}")
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
    new xml.Elem("p", "output", dump_attr.get, namespaceScope, false, nodes:_*)
  }

}
