package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.model.xml.containers.Container
import com.xmlcalabash.model.xml.{Artifact, XProcConstants, XmlPipelineException}

class Document(override val parent: Option[Artifact]) extends DataSource(parent) {
  private var _href: Option[String] = None
  private var valid = true

  override def validate(): Boolean = {
    _href = properties.get(XProcConstants._href)

    for (key <- List(XProcConstants._href)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (_href.isEmpty) {
      throw new XmlPipelineException("hrefreq", "Href is required")
    }

    if (properties.nonEmpty) {
      val key = properties.keySet.head
      throw new XmlPipelineException("badopt", s"Unexpected attribute: ${key.getLocalName}")
    }

    if (children.nonEmpty) {
      throw new XmlPipelineException("badelem", s"Unexpected element: ${children.head}")
    }

    valid
  }

}
