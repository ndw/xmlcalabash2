package com.xmlcalabash.model.xml.containers

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.Pipe
import com.xmlcalabash.model.xml.{Artifact, AtomicStep, Documentation, Input, Output, PipeInfo, Variable}

import scala.collection.mutable

class Choose(override val config: XMLCalabash,
             override val parent: Option[Artifact]) extends Container(config, parent) {

  override def validate(): Boolean = {
    var valid = true

    _name = attributes.get(XProcConstants._name)
    if (_name.isDefined) {
      label = _name.get
    } else {
      label = "choose"
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

    valid = valid && makePortsExplicit()
    valid = valid && makeBindingsExplicit()

    var primaryInputPort = Option.empty[String]
    var primaryOutputPort = Option.empty[String]

    val impliedOutputs = mutable.HashSet.empty[String]
    for (child <- relevantChildren()) {
      // We can do this in one pass because inputs and outputs are always first
      child match {
        case output: Output =>
          impliedOutputs += output.port.get
        case when: When =>
          if (when.primaryInput.isDefined) {
            if (primaryInputPort.isDefined) {
              if (when.primaryInput.get.port.get != primaryInputPort.get) {
                throw new ModelException(ExceptionCode.DIFFPRIMARYINPUT,
                  List(primaryInputPort.get, when.primaryInput.get.port.get), when.location)
              }
            } else {
              primaryInputPort = when.primaryInput.get.port
            }
          }

          if (when.primaryOutput.isDefined) {
            if (primaryOutputPort.isDefined) {
              if (when.primaryOutput.get.port.get != primaryOutputPort.get) {
                throw new ModelException(ExceptionCode.DIFFPRIMARYOUTPUT,
                  List(primaryOutputPort.get, when.primaryOutput.get.port.get), when.location)
              }
            } else {
              primaryOutputPort = when.primaryOutput.get.port
            }
          }

          for (port <- when.outputPorts) {
            if (!impliedOutputs.contains(port)) {
              impliedOutputs += port
              val output = when.output(port).get
              val out = new Output(config, this, port, primary=output.primary, sequence=output.sequence)
              addChild(out)
            }
          }
        case when: Otherwise =>
          if (when.primaryInput.isDefined) {
            if (primaryInputPort.isDefined) {
              if (when.primaryInput.get.port.get != primaryInputPort.get) {
                throw new ModelException(ExceptionCode.DIFFPRIMARYINPUT,
                  List(primaryInputPort.get, when.primaryInput.get.port.get), when.location)
              }
            } else {
              primaryInputPort = when.primaryInput.get.port
            }
          }

          if (when.primaryOutput.isDefined) {
            if (primaryOutputPort.isDefined) {
              if (when.primaryOutput.get.port.get != primaryOutputPort.get) {
                throw new ModelException(ExceptionCode.DIFFPRIMARYOUTPUT,
                  List(primaryOutputPort.get, when.primaryOutput.get.port.get), when.location)
              }
            } else {
              primaryOutputPort = when.primaryOutput.get.port
            }
          }

          for (port <- when.outputPorts) {
            if (!impliedOutputs.contains(port)) {
              impliedOutputs += port
              val output = when.output(port).get
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
        start.addChoose(name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Choose parent isn't a container???", location)
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
