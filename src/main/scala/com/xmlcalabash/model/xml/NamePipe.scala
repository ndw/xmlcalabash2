package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

class NamePipe(override val config: XMLCalabashConfig, val name: QName, val step: String, val link: NameBinding) extends Artifact(config) {
  override def parse(node: XdmNode): Unit = {
    throw new RuntimeException("This is a purely synthetic element")
  }

  override protected[model] def validateStructure(): Unit = {
    if (allChildren.nonEmpty) {
      throw new RuntimeException(s"Invalid content in $this")
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
    val toNode = parNode
    val toPort = "#bindings"
    val fromNode = link._graphNode.get
    val fromPort = "result"
    runtime.graph.addEdge(fromNode, fromPort, toNode, toPort)
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startNamePipe(tumble_id, step)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endNamePipe()
  }

  override def toString: String = {
    s"p:name-pipe $name $step"
  }
}
