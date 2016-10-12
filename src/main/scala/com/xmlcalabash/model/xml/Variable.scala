package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcConstants, XProcEngine}
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
  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val select = property(XProcConstants._select).get.value
    val step = new XPathExpression(engine, inScopeNamespaces, select)
    step.label = declaredName.get.getClarkName

    val node = graph.createVariableNode(step)
    nodeMap.put(this, node)
  }
}
