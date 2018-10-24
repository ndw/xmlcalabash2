package com.xmlcalabash.model.xml.datasource

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.model.xml.{Artifact, IOPort}
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime}
import com.xmlcalabash.steps.internal.EmptyLoader

class Empty(override val config: XMLCalabashRuntime,
            override val parent: Option[Artifact]) extends DataSource(config, parent) {

  def this(config: XMLCalabashRuntime, parent: Artifact, empty: Empty) {
    this(config, Some(parent))
  }

  override def validate(): Boolean = {
    super.validate()
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val container = this.parent.get.parent.get.parent.get
    val cnode = container._graphNode.get.asInstanceOf[ContainerStart]
    val context = new ExpressionContext(baseURI, inScopeNS, location)
    val step = new EmptyLoader()
    val emptyReader = cnode.addAtomic(step, "empty")

    _graphNode = Some(emptyReader)
    config.addNode(emptyReader.id, this)
  }

  override def makeEdges(graph: Graph, parent: Node): Unit = {
    val toStep = this.parent.get.parent
    val toPort = this.parent.get.asInstanceOf[IOPort].port.get
    graph.addOrderedEdge(_graphNode.get, "result", toStep.get._graphNode.get, toPort)
  }
}
