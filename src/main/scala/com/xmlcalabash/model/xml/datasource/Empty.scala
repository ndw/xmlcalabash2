package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.model.xml.Artifact
import com.xmlcalabash.runtime.XMLCalabashRuntime

class Empty(override val config: XMLCalabashRuntime,
            override val parent: Option[Artifact]) extends DataSource(config, parent) {

  def this(config: XMLCalabashRuntime, parent: Artifact, empty: Empty) {
    this(config, Some(parent))
  }

  override def validate(): Boolean = {
    // nop?
    true
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    // nop?
  }
}
