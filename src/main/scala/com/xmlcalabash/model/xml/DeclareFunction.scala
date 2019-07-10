package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabashConfig

class DeclareFunction(override val config: XMLCalabashConfig) extends Artifact(config) {
  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    for (child <- allChildren) {
      child.makeStructureExplicit(environment)
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
    }
  }
}
