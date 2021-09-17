package com.xmlcalabash.model.xml

import com.jafpl.graph.{ChooseStart, ContainerStart, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class ForEach(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    val fc = firstChild
    if (firstWithInput.isEmpty) {
      val winput = new WithInput(config)
      winput.port = "#source"
      winput.primary = true
      addChild(winput, fc)
    } else {
      val wi = firstWithInput.get
      wi.primary = true
      if (wi.port == "") {
        wi.port = "#source"
      }
    }

    val input = new DeclareInput(config)
    input.port = "current"
    input.primary = true
    addChild(input, fc)

    makeContainerStructureExplicit()
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addForEach(stepName, Manifold.ALLOW_ANY)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, node)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    val winput = firstWithInput
    if (winput.isDefined) {
      for (pipe <- winput.get.children[Pipe]) {
        runtime.graph.addEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, "source")
      }
    }

    for (output <- children[DeclareOutput]) {
      for (pipe <- output.children[Pipe]) {
        runtime.graph.addEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, output.port)
      }
    }

    for (child <- children[Step]) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startForEach(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endForEach()
  }

  override def toString: String = {
    s"p:for-each $stepName"
  }
}