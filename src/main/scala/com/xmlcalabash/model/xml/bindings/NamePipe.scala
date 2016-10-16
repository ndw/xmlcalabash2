package com.xmlcalabash.model.xml.bindings

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.model.xml.{Artifact, NameDecl, XPathContext}
import net.sf.saxon.s9api.QName

import scala.collection.mutable

/**
  * Created by ndw on 10/7/16.
  */
class NamePipe(name: QName, parent: XPathContext) extends Binding(None, Some(parent)) {
  _xmlname = "name-pipe"
  var _decl: Option[NameDecl] = None

  def decl = _decl
  def decl_=(decl: NameDecl): Unit = {
    _decl = Some(decl)
  }

  override def dumpAdditionalAttributes(tree: TreeWriter): Unit = {
    if (_decl.isDefined) {
      tree.addAttribute(XProcConstants.px("decl"), _decl.get.toString)
    }
  }

  override private[xml] def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val srcStep = _decl.get
    val dstStep = parent.parent.get

    var dstPort = ""
    srcStep match {
      case ndecl: NameDecl =>
        dstPort = ndecl.declaredName.get.getClarkName
        if (!dstPort.startsWith("{")) {
          dstPort = "{}" + dstPort
        }
    }

    graph.addEdge(nodeMap(srcStep), "result", nodeMap(dstStep), dstPort)
  }

}
