package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{ContainerStart, Graph, Node, TryCatchStart}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.{Artifact, Documentation, PipeInfo}
import com.xmlcalabash.runtime.XMLCalabashRuntime

class Group(override val config: XMLCalabashRuntime,
            override val parent: Option[Artifact]) extends Container(config, parent, XProcConstants.p_group) {

  override def validate(): Boolean = {
    var valid = super.validate()

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    for (child <- relevantChildren) {
      valid = valid && child.validate()
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case trycatch: TryCatchStart =>
        trycatch.addTry(name)
      case cont: ContainerStart =>
        cont.addGroup(name, manifold)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Group parent isn't a container???", location)
    }
    _graphNode = Some(node)
    config.addNode(node.id, this)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parentNode: Node) {
    for (output <- outputs) {
      graph.addEdge(_graphNode.get, output.port.get, parentNode, output.port.get)
    }

    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, _graphNode.get)
      }
    }
  }
}
