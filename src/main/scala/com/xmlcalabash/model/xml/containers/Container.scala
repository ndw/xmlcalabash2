package com.xmlcalabash.model.xml.containers

import com.xmlcalabash.model.xml.{Artifact, Step}

class Container(override val parent: Option[Artifact]) extends Step(parent) {
  /*
  override def makeBindingsExplicit(): Boolean = {
    true
  }
  */
}
