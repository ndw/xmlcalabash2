package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.config.XMLCalabashConfig
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable.ListBuffer

class PipeInfo(override val config: XMLCalabashConfig,
               override val parent: Option[Artifact],
               val content: List[XdmNode]) extends Artifact(config, parent) {
  override def validate(): Boolean = {
    true
  }

  override def makeGraph(graph: Graph, parent: Node) {
    // no direct contribution
  }

  override def makeEdges(graph: Graph, parent: Node) {
    // no direct contribution
  }

  override def asXML: xml.Elem = {
    dumpAttr(attributes.toMap)
    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "pipeinfo", dump_attr.getOrElse(xml.Null),
      namespaceScope, false, nodes:_*)
  }
}
