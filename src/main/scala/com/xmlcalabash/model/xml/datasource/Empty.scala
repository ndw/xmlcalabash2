package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.model.xml.Artifact

class Empty(override val config: XMLCalabashConfig,
            override val parent: Option[Artifact]) extends DataSource(config, parent) {
  def this(config: XMLCalabashConfig, parent: Artifact, empty: Empty) {
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
