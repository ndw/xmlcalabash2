package com.xmlcalabash.model.xml

import com.jafpl.graph.{Node, TryCatchStart}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class Finally(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    makeContainerStructureExplicit()

    for (output <- children[DeclareOutput]) {
      if (output.primary) {
        throw new RuntimeException("No output on p:finally may be primary")
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node) {
    val start = parent.asInstanceOf[TryCatchStart]
    val node = start.addFinally(stepName)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, _graphNode.get)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node) {
    for (child <- allChildren) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startFinally(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endFinally()
  }

  override def toString: String = {
    s"p:finally $stepName"
  }
}