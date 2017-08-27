package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.model.exceptions.ModelException
import com.xmlcalabash.model.xml.{Artifact, ParserConfiguration, XProcConstants}

import scala.collection.mutable.ListBuffer

class Document(override val config: ParserConfiguration,
               override val parent: Option[Artifact]) extends DataSource(config, parent) {
  private var _href: Option[String] = None

  override def validate(): Boolean = {
    _href = properties.get(XProcConstants._href)

    for (key <- List(XProcConstants._href)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (_href.isEmpty) {
      throw new ModelException("hrefreq", "Href is required")
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new ModelException("badopt", s"Unexpected attribute: ${key.getLocalName}")
    }

    if (children.nonEmpty) {
      throw new ModelException("badelem", s"Unexpected element: ${children.head}")
    }

    valid
  }

  override def asXML: xml.Elem = {
    dumpAttr("href", _href)

    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "document", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }

}
