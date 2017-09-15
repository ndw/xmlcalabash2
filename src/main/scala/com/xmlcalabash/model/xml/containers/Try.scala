package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.{Artifact, Documentation, Output, PipeInfo, Variable}

import scala.collection.mutable

class Try(override val config: XMLCalabash,
          override val parent: Option[Artifact]) extends Container(config, parent) {
  override def validate(): Boolean = {
    var valid = true

    _name = attributes.get(XProcConstants._name)
    if (_name.isDefined) {
      label = _name.get
    } else {
      label = "try"
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

    var relChildren = relevantChildren()
    var pos = 0
    while (pos < relChildren.length && relChildren(pos).isInstanceOf[Variable]) {
      valid = valid && relChildren(pos).validate()
      pos += 1
    }

    var hasGroup = false
    while (pos < relChildren.length && relChildren(pos).isInstanceOf[Group]) {
      if (hasGroup) {
        throw new ModelException(ExceptionCode.DUPGROUP, List.empty[String], relChildren(pos).location)
      }
      valid = valid && relChildren(pos).validate()
      hasGroup = true
      pos += 1
    }

    var hasCatch = false
    while (pos < relChildren.length && relChildren(pos).isInstanceOf[Catch]) {
      valid = valid && relChildren(pos).validate()
      hasCatch = true
      pos += 1
    }

    var hasFinally = false
    while (pos < relChildren.length && relChildren(pos).isInstanceOf[Finally]) {
      if (hasFinally) {
        throw new ModelException(ExceptionCode.DUPFINALLY, List.empty[String], relChildren(pos).location)
      }
      valid = valid && relChildren(pos).validate()
      hasFinally = true
      pos += 1
    }

    if (pos < relChildren.length) {
      throw new ModelException(ExceptionCode.BADTRYCHILD, relChildren(pos).name, relChildren(pos).location)
    }

    if (!hasGroup) {
      throw new ModelException(ExceptionCode.MISSINGGROUP, List.empty[String], location)
    }

    if (!hasCatch) {
      throw new ModelException(ExceptionCode.MISSINGCATCH, List.empty[String], location)
    }

    var primaryInputPort = Option.empty[String]
    var primaryOutputPort = Option.empty[String]

    val impliedOutputs = mutable.HashSet.empty[String]
    for (child <- relevantChildren()) {
      // We can do this in one pass because inputs and outputs are always first
      child match {
        case output: Output =>
          impliedOutputs += output.port.get
        case group: Group =>
          if (group.primaryInput.isDefined) {
            if (primaryInputPort.isDefined) {
              if (group.primaryInput.get.port.get != primaryInputPort.get) {
                throw new ModelException(ExceptionCode.DIFFPRIMARYINPUT,
                  List(primaryInputPort.get, group.primaryInput.get.port.get), group.location)
              }
            } else {
              primaryInputPort = group.primaryInput.get.port
            }
          }

          if (group.primaryOutput.isDefined) {
            if (primaryOutputPort.isDefined) {
              if (group.primaryOutput.get.port.get != primaryOutputPort.get) {
                throw new ModelException(ExceptionCode.DIFFPRIMARYOUTPUT,
                  List(primaryOutputPort.get, group.primaryOutput.get.port.get), group.location)
              }
            } else {
              primaryOutputPort = group.primaryOutput.get.port
            }
          }

          for (port <- group.outputPorts) {
            if (!impliedOutputs.contains(port)) {
              impliedOutputs += port
              val output = group.output(port).get
              val out = new Output(config, this, port, primary=output.primary, sequence=output.sequence)
              addChild(out)
            }
          }
        case katch: Catch =>
          if (katch.primaryInput.isDefined) {
            if (primaryInputPort.isDefined) {
              if (katch.primaryInput.get.port.get != primaryInputPort.get) {
                throw new ModelException(ExceptionCode.DIFFPRIMARYINPUT,
                  List(primaryInputPort.get, katch.primaryInput.get.port.get), katch.location)
              }
            } else {
              primaryInputPort = katch.primaryInput.get.port
            }
          }

          if (katch.primaryOutput.isDefined) {
            if (primaryOutputPort.isDefined) {
              if (katch.primaryOutput.get.port.get != primaryOutputPort.get) {
                throw new ModelException(ExceptionCode.DIFFPRIMARYOUTPUT,
                  List(primaryOutputPort.get, katch.primaryOutput.get.port.get), katch.location)
              }
            } else {
              primaryOutputPort = katch.primaryOutput.get.port
            }
          }

          for (port <- katch.outputPorts) {
            if (!impliedOutputs.contains(port)) {
              impliedOutputs += port
              val output = katch.output(port).get
              val out = new Output(config, this, port, primary=output.primary, sequence=output.sequence)
              addChild(out)
            }
          }
        case _ => Unit
      }
    }

    valid
  }

  override def makeOutputBindingsExplicit(): Boolean = {
    // These are conneced up by the when components
    true
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case start: ContainerStart =>
        start.addTryCatch(name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Try parent isn't a container???", location)
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
          child.makeEdges(graph, graphNode.get)
      }
    }
  }
}
