package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{ChooseStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.xml.{Artifact, Documentation, PipeInfo, XProcConstants}
import com.xmlcalabash.runtime.XProcXPathExpression

class Otherwise(override val config: XMLCalabash,
                override val parent: Option[Artifact]) extends Container(config, parent) {
  override def validate(): Boolean = {
    var valid = true

    _name = attributes.get(XProcConstants._name)
    if (_name.isDefined) {
      label = _name.get
    } else {
      label = "otherwise"
    }

    for (key <- List(XProcConstants._name)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    for (child <- relevantChildren()) {
      valid = valid && child.validate()
    }

    valid = valid && makePortsExplicit()
    valid = valid && makeBindingsExplicit()

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case choose: ChooseStart =>
        choose.addWhen(new XProcXPathExpression(Map.empty[String,String], "true()"), name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "When parent isn't a choose???", location)
    }
    graphNode = Some(node)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parent: Node) {
    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, parent)
      }
    }
  }
}
