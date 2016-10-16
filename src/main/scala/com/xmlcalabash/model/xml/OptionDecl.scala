package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.runtime.XPathExpression
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

// Yes, it should be called Option but that's an inconvenient name in a Scala program :-)
//
class OptionDecl(node: Option[XdmNode], parent: Option[Artifact]) extends NameDecl(node, parent) {
  var input: Option[Node] = None
  var expr: Option[Node] = None
  var optName: String = ""

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val name = property(XProcConstants._name).get.value
    optName = declaredName.get.getClarkName
    if (!optName.startsWith("{")) {
      optName = "{}" + optName
    }
    input = Some(graph.createInputNode(optName))
    nodeMap.put(this, input.get)

    val select = property(XProcConstants._select).get.value
    val step = new XPathExpression(engine, inScopeNamespaces, select, Some(declaredName.get))
    step.label = declaredName.get.getClarkName

    expr = Some(graph.createVariableNode(step))
    nodeMap.put(this, expr.get)
  }

  override private[xml] def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    graph.addEdge(input.get, "result", expr.get, optName)
  }
}
