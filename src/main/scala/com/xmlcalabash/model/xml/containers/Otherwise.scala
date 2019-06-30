package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{ChooseStart, Graph, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.{Artifact, Documentation, Output, PipeInfo}
import com.xmlcalabash.runtime.{ExpressionContext, XMLCalabashRuntime, XProcXPathExpression}

class Otherwise(override val config: XMLCalabashRuntime,
                override val parent: Option[Artifact],
                val synthetic: Boolean) extends Container(config, parent, XProcConstants.p_otherwise) {

  def this(config: XMLCalabashRuntime, parent: Option[Artifact]) {
    this(config, parent, false)
  }

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

  override def makeOutputPortsExplicit(): Boolean = {
    if (!synthetic) {
      return super.makeOutputPortsExplicit()
    }

    // Null sucks, but there has to be a when...
    var when: When = null
    val x = parent.get.children
    for (child <- parent.get.children) {
      child match {
        case w: When => when = w
        case _ => Unit
      }
    }

    if (when.primaryOutput.isEmpty) {
      val choose = parent.get.asInstanceOf[Choose]
      if (choose.p_if) {
        throw XProcException.xsPrimaryOutputRequired(when.location)
      }
    }

    // lastChildStep is always p:identity
    val step = lastChildStep
    if (when.primaryOutput.isDefined) {
      val output = new Output(config, this, when.primaryOutput.get.port.get, primary=true, sequence=step.get.primaryOutput.get.sequence)
      addChild(output)
    }

    true
  }


  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case choose: ChooseStart =>
        val context = new ExpressionContext(staticContext)
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
