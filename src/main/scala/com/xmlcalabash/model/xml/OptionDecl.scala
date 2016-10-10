package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.jafpl.graph.{Graph, Node}
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

// Yes, it should be called Option but that's an inconvenient name in a Scala program :-)
//
class OptionDecl(node: Option[XdmNode], parent: Option[Artifact]) extends NameDecl(node, parent) {
  override def buildNodes(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val name = property(XProcConstants._name).get.value
    val optName = declaredName.get
    val node = graph.createInputOption(optName)
    nodeMap.put(this, node)
  }
}
