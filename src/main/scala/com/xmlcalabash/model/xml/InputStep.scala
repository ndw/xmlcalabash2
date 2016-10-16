package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.bindings.{Binding, Document, Inline, InputBinding}
import com.xmlcalabash.model.xml.util.TreeWriter
import com.xmlcalabash.runtime.io.{DocumentReader, InlineReader}

import scala.collection.mutable

/**
  * Created by ndw on 10/5/16.
  */
class InputStep(val binding: InputBinding, parent: Artifact) extends Step(None, Some(parent)) {
  _xmlname = "input-step"

  val result = new Output(None, Some(this))
  result.setProperty(XProcConstants._port, "result")
  result.setProperty(XProcConstants._primary, "true")
  _children += result

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    var jstep: Option[com.jafpl.runtime.Step] = None

    binding match {
      case doc: Document =>
        jstep = Some(new DocumentReader(engine, doc.href))
      case inl: Inline =>
        // Can only happen once, right?
        for (lit <- inl.children.collect { case lit: XMLLiteral => lit }) {
          val tree = new TreeWriter(engine)
          tree.startDocument(lit.node.get.getBaseURI)
          tree.addSubtree(lit.node.get)
          tree.endDocument()
          jstep = Some(new InlineReader(engine, tree.getResult))
        }
    }

    val node = graph.createNode(jstep.get)
    nodeMap.put(this, node)
  }
}
