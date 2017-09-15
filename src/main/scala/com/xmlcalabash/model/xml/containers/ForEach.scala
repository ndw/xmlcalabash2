package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{ContainerStart, Graph, Node, TryCatchStart}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.{DataSource, Pipe}
import com.xmlcalabash.model.xml.{Artifact, Documentation, Input, PipeInfo}

class ForEach(override val config: XMLCalabash,
              override val parent: Option[Artifact]) extends Container(config, parent) {

  override def validate(): Boolean = {
    var valid = true

    _name = attributes.get(XProcConstants._name)
    if (_name.isDefined) {
      label = _name.get
    } else {
      label = "for-each"
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

    valid
  }

  override def makeInputPortsExplicit(): Boolean = {
    var source = Option.empty[Input]
    var current = Option.empty[Input]

    for (child <- children) {
      child match {
        case input: Input =>
          input.port.get match {
            case "source" => source = Some(input)
            case "current" => current = Some(input)
            case _ => throw new RuntimeException("BANG1")
          }
        case _ => Unit
      }
    }

    if (source.isEmpty) {
      val input = new Input(config, this, "source", primary=false, sequence=true)
      addChild(input)
    }

    if (current.isEmpty) {
      val input = new Input(config, this, "current", primary=true, sequence=true)
      addChild(input)
    }

    valid
  }

  override def makeInputBindingsExplicit(): Boolean = {
    val drp = defaultReadablePort
    if (drp.isDefined) {
      for (port <- inputPorts) {
        val in = input(port).get
        if (in.children.isEmpty && (port != "current")) {
          val pipe = new Pipe(config, in, drp.get.parent.get.name, drp.get.port.get)
          in.addChild(pipe)
        }
      }
    } else {
      for (port <- inputPorts) {
        val in = input(port).get
        var binding = Option.empty[DataSource]
        for (child <- children) {
          child match {
            case data: DataSource =>
              binding = Some(data)
            case _ => Unit
          }
        }
        valid = valid && binding.isDefined
      }
    }

    valid
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case cont: ContainerStart =>
        cont.addForEach(name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "ForEach parent isn't a container???", location)
    }
    graphNode = Some(node)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parentNode: Node) {
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
