package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.model.xml.{Artifact, DeclareStep, Import, Input, Log, OptionDecl, Output, Serialization, XProcConstants, XmlPipelineException}

class Inline(override val parent: Option[Artifact]) extends DataSource(parent) {
  private var _excludeInlinePrefixes: Set[String] = Set()
  private var valid = true

  override def validate(): Boolean = {
    _excludeInlinePrefixes = lexicalPrefixes(properties.get(XProcConstants._exclude_inline_prefixes))
    for (key <- List(XProcConstants._exclude_inline_prefixes)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (properties.nonEmpty) {
      throw new XmlPipelineException("badopt",
        s"Unexpected attribute: ${properties.keySet.head.getLocalName}")
    }

    valid
  }

}
