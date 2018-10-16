package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.jafpl.steps.{Manifold, PortCardinality, PortSpecification}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.Pipe
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class PipelineStep(override val config: XMLCalabashConfig,
                   override val parent: Option[Artifact],
                   val stepType: QName) extends Artifact(config, parent) {
  protected var _name: Option[String] = None
  // Can be used to create additional variable bindings, e.g., for injectables
  protected[xml] val variableRefs = mutable.HashSet.empty[QName]

  protected[xml] def name_=(name: String): Unit = {
    _name = Some(name)
  }

  /* this breaks implicit pipeline connections.
  override def name: String = {
    if (label.isDefined && !label.get.startsWith("!")) {
      label.get
    } else {
      stepType.toString
    }
  }
  */

  def graphNode: Node = _graphNode.get // Steps always have graphNodes

  def addVariableRef(ref: QName): Unit = {
    variableRefs += ref
  }

  override def validate(): Boolean = {
    _name = attributes.get(XProcConstants._name)
    if (_name.isDefined) {
      val regex = """([\p{L}_][-\p{L}_\p{N}]*)""".r
      _name.get match {
        case regex(name) => label = name
        case _ => throw new ModelException(ExceptionCode.INVALIDNAME, _name.get, location)
      }
    } else {
      label = defaultLabel
    }

    for (key <- List(XProcConstants._name)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    true
  }

  def makeInputPortsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makeInputPortsExplicit")
    false
  }

  def makeOutputPortsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makeOutputPortsExplicit")
    false
  }

  def makeInputBindingsExplicit(): Boolean = {
    var valid = true
    val drp = defaultReadablePort

    for (in <- inputs) {
      val port = in.port.get

      if (relevantChildren(in.children.toList).isEmpty) {
        if (in.primary.get) {
          if (drp.isDefined) {
            val pipe = new Pipe(config, in, drp.get.parent.get.name, drp.get.port.get)
            in.addChild(pipe)
          } else {
            valid = false
            throw XProcException.xsUnconnectedPrimaryInputPort(name, port, location)
          }
        } else {
          valid = false
          throw XProcException.xsUnconnectedInputPort(name, port, location)
        }
      }
    }

    valid
  }

  def makeOutputBindingsExplicit(): Boolean = {
    true
  }

  override def makePortsExplicit(): Boolean = {
    makeInputPortsExplicit() && makeOutputPortsExplicit()
  }

  override def makeBindingsExplicit(): Boolean = {
    makeInputBindingsExplicit() && makeOutputBindingsExplicit()
  }

  override def makeEdges(graph: Graph, parent: Node) {
    graphEdges(graph, _graphNode.get.asInstanceOf[ContainerStart])
  }

  def manifold: Manifold = {
    val inputMap = mutable.HashMap.empty[String,PortCardinality]
    for (input <- inputs) {
      if (input.sequence) {
        inputMap.put(input.port.get, PortCardinality.ZERO_OR_MORE)
      } else {
        inputMap.put(input.port.get, PortCardinality.EXACTLY_ONE)
      }
    }

    val outputMap = mutable.HashMap.empty[String,PortCardinality]
    for (output <- outputs) {
      if (output.sequence) {
        outputMap.put(output.port.get, PortCardinality.ZERO_OR_MORE)
      } else {
        outputMap.put(output.port.get, PortCardinality.EXACTLY_ONE)
      }
    }

    new Manifold(new PortSpecification(inputMap.toMap), new PortSpecification(outputMap.toMap))
  }
}
