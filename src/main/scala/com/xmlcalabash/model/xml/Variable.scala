package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.runtime.XPathExpression
import com.xmlcalabash.util.UniqueId
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class Variable(node: Option[XdmNode], parent: Option[Artifact]) extends NameDecl(node, parent) {
  override def buildNodes(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val select = property(XProcConstants._select).get.value
    val node = graph.createVariableNode(declaredName.get, new XPathExpression(select, "var_" + UniqueId.nextId.toString))
    nodeMap.put(this, node)
  }
}
