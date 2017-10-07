package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.xml.Artifact

class Empty(override val config: XMLCalabash,
            override val parent: Option[Artifact]) extends DataSource(config, parent) {
  def this(config: XMLCalabash, parent: Artifact, empty: Empty) {
    this(config, Some(parent))
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    // nop?
  }
}
