package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{Graph, Node, TryCatchStart}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.model.xml.{Artifact, Documentation, PipeInfo}
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class Finally(override val config: XMLCalabash,
              override val parent: Option[Artifact]) extends Container(config, parent) {
  override def validate(): Boolean = {
    var valid = super.validate()

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    for (child <- relevantChildren()) {
      valid = valid && child.validate()
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case trycatch: TryCatchStart =>
        trycatch.addFinally(name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Finally parent isn't a try/catch???", location)
    }
    graphNode = Some(node)
    config.addNode(node.id, this)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parentNode: Node) {
    for (output <- outputPorts) {
      graph.addEdge(graphNode.get, output, parentNode, output)
    }

    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, graphNode.get)
      }
    }
  }
}
