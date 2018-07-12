package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.Pipe
import com.xmlcalabash.model.xml.{Artifact, AtomicStep, Documentation, Input, Output, PipeInfo, Variable}

import scala.collection.mutable

class Choose(override val config: XMLCalabash,
             override val parent: Option[Artifact]) extends Container(config, parent, XProcConstants.p_choose) {

  override def validate(): Boolean = {
    var valid = super.validate()

    if (attributes.nonEmpty) {
      val key = attributes.keySet.head
      throw new ModelException(ExceptionCode.BADATTR, key.toString, location)
    }

    var relChildren = relevantChildren
    var pos = 0
    while (pos < relChildren.length && relChildren(pos).isInstanceOf[Variable]) {
      valid = valid && relChildren(pos).validate()
      pos += 1
    }

    var hasWhen = false
    while (pos < relChildren.length && relChildren(pos).isInstanceOf[When]) {
      valid = valid && relChildren(pos).validate()
      hasWhen = true
      pos += 1
    }

    var hasOtherwise = false
    while (pos < relChildren.length && relChildren(pos).isInstanceOf[Otherwise]) {
      if (hasOtherwise) {
        throw new ModelException(ExceptionCode.DUPOTHERWISE, List.empty[String], relChildren(pos).location)
      }
      valid = valid && relChildren(pos).validate()
      hasOtherwise = true
      pos += 1
    }

    if (pos < relChildren.length) {
      throw new ModelException(ExceptionCode.BADCHOOSECHILD, relChildren(pos).name, relChildren(pos).location)
    }

    if (!hasWhen & !hasOtherwise) {
      throw new ModelException(ExceptionCode.MISSINGWHEN, List.empty[String], location)
    }

    var primaryInputPort = Option.empty[String]
    var primaryOutputPort = Option.empty[String]

    val impliedOutputs = mutable.HashSet.empty[String]
    for (child <- relevantChildren) {
      // We can do this in one pass because inputs and outputs are always first
      var process = false
      child match {
        case output: Output => impliedOutputs += output.port.get
        case when: When => process = true
        case other: Otherwise => process = true
        case _ => Unit
      }

      if (child.primaryInput.isDefined) {
        if (primaryInputPort.isDefined) {
          if (child.primaryInput.get.port.get != primaryInputPort.get) {
            throw new ModelException(ExceptionCode.DIFFPRIMARYINPUT,
              List(primaryInputPort.get, child.primaryInput.get.port.get), child.location)
          }
        } else {
          primaryInputPort = child.primaryInput.get.port
        }
      }

      if (child.primaryOutput.isDefined) {
        if (primaryOutputPort.isDefined) {
          if (child.primaryOutput.get.port.get != primaryOutputPort.get) {
            throw new ModelException(ExceptionCode.DIFFPRIMARYOUTPUT,
              List(primaryOutputPort.get, child.primaryOutput.get.port.get), child.location)
          }
        } else {
          primaryOutputPort = child.primaryOutput.get.port
        }
      }
    }

    valid
  }

  override def makeOutputPortsExplicit(): Boolean = {
    val ports = mutable.HashSet.empty[String]
    var primary = Option.empty[String]

    for (step <- children) {
      var process = false
      step match {
        case when: When => process = true
        case other: Otherwise => process = true
        case _ => Unit
      }

      if (process) {
        for (child <- step.children) {
          child match {
            case output: Output =>
              ports += output.port.get
              if (output.primary.getOrElse(false)) {
                if (primary.isDefined) {
                  if (primary.get != output.port.get) {
                    throw new ModelException(ExceptionCode.DIFFPRIMARYOUTPUT, List(primary.get, output.port.get), location)
                  }
                } else {
                  primary = Some(output.port.get)
                }
              }
            case _ => Unit
          }
        }
      }
    }

    for (port <- ports) {
      val isprimary = primary.isDefined && (primary.get == port)
      val output = new Output(config, this, port, primary=isprimary, sequence=true)
      addChild(output)
    }

    true
  }

  override def makeOutputBindingsExplicit(): Boolean = {
    // These are connected up by the when components
    true
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case start: ContainerStart =>
        start.addChoose(name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Choose parent isn't a container???", location)
    }
    _graphNode = Some(node)
    config.addNode(node.id, this)

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
          child.makeEdges(graph, _graphNode.get)
      }
    }
  }
}
