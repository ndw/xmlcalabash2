package com.xmlcalabash.model.xml.datasource

import com.xmlcalabash.exceptions.ModelException
import com.xmlcalabash.model.util.ParserConfiguration
import com.xmlcalabash.model.xml.{Artifact, XProcConstants}

class Inline(override val config: ParserConfiguration,
             override val parent: Option[Artifact]) extends DataSource(config, parent) {
  private var _excludeInlinePrefixes: Set[String] = Set()

  override def validate(): Boolean = {
    _excludeInlinePrefixes = lexicalPrefixes(properties.get(XProcConstants._exclude_inline_prefixes))
    for (key <- List(XProcConstants._exclude_inline_prefixes)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    if (properties.nonEmpty) {
      throw new ModelException("badopt",
        s"Unexpected attribute: ${properties.keySet.head.getLocalName}", location)
    }

    valid
  }

}
