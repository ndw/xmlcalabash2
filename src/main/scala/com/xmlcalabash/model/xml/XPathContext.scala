package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.model.xml.bindings.{NamePipe, Pipe}
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class XPathContext(node: Option[XdmNode], parent: Option[Artifact]) extends Artifact(node, parent) {
  _xmlname = "xpath-context"

  override def fixBindingsOnIO(): Unit = {
    if (bindings().isEmpty) {
      parent.get match {
        case when: When =>
          val pipe = new Pipe(None, Some(this))
          _children += pipe
        case _ =>
          if (defaultReadablePort.isDefined) {
            val pipe = new Pipe(None, Some(this))
            pipe._drp = defaultReadablePort
            _children += pipe
          }
      }
   }

    for (child <- _children) { child.fixBindingsOnIO() }
  }

  override def findPipeBindings(): Unit = {
    parent.get match {
      case when: When =>
        // Special defaulting rules apply inside p:when/p:xpath-context
        for (pipe <- children.collect { case pipe: Pipe => pipe }) {
          if (pipe.synthetic) {
            val chooseCtx = parent.get.parent.get.children.head.asInstanceOf[XPathContext]
            pipe.connectAs(chooseCtx.children.head.asInstanceOf[Pipe])
          }
        }
      case _ => Unit
    }

    for (child <- _children) { child.findPipeBindings() }
  }

  override private[xml] def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    if (parent.isDefined && parent.get.isInstanceOf[When]) {
      // nop; handled by When
    } else {
      for (child <- children) {
        child match {
          case pipe: Pipe =>
            if (parent.isDefined && parent.get.isInstanceOf[OptionDecl]) {
              // nop; option decls don't get a context node
            } else {
              child.buildEdges(graph, nodeMap)
            }
          case npipe: NamePipe =>
            child.buildEdges(graph, nodeMap)
        }
      }
    }
  }
}
