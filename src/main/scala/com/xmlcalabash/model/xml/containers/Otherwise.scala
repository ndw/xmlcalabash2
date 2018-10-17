package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{ChooseStart, Graph, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.{Artifact, Documentation, PipeInfo}
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime, XProcXPathExpression}

class Otherwise(override val config: XMLCalabashRuntime,
                override val parent: Option[Artifact]) extends Container(config, parent, XProcConstants.p_otherwise) {
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
      case choose: ChooseStart =>
        val context = new ExpressionContext(baseURI, inScopeNS, location)
        choose.addWhen(new XProcXPathExpression(context, "true()"), name, Manifold.ALLOW_ANY)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "When parent isn't a choose???", location)
    }
    _graphNode = Some(node)
    config.addNode(node.id, this)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parentNode: Node) {
    for (output <- outputPorts) {
      graph.addEdge(_graphNode.get, output, parentNode, output)
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
