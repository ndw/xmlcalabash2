package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.runtime.XPathExpression
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class WithOption(node: Option[XdmNode], parent: Option[Artifact]) extends NameDecl(node, parent) {
  var select: Option[String] = None

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    var expr = property(XProcConstants._select).get.value
    select = Some(expr)

    val step = new XPathExpression(engine, inScopeNamespaces, expr)
    step.label = declaredName.get.getClarkName

    val node = graph.createVariableNode(step)
    nodeMap.put(this, node)
  }

  override def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    super.buildEdges(graph, nodeMap)

    /* handled by name-binding in xpath-context
    val xpp = new XPathParser(select.get)
    for (cname <- xpp.variableRefs()) {
      val name = new QName("", cname)
      val decl = findNameDecl(name, this)

      if (decl.isDefined) {
        var destPort = decl.get.declaredName.get.getClarkName
        if (!destPort.startsWith("{")) {
          destPort = "{}" + destPort
        }
        graph.addEdge(nodeMap(decl.get), "result", nodeMap(this), destPort)
      }
    }
    */

    val srcNode = nodeMap(this)
    val srcPort = "result"
    val destNode = nodeMap(parent.get)

    var destPort = declaredName.get.getClarkName
    if (!destPort.startsWith("{")) {
      destPort = "{}" + destPort
    }

    graph.addEdge(srcNode, srcPort, destNode, destPort)
  }
}
