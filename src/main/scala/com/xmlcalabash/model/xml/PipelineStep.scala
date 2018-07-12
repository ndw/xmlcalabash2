package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.datasource.{DataSource, Pipe}
import net.sf.saxon.s9api.QName

import scala.collection.mutable

class PipelineStep(override val config: XMLCalabash,
                   override val parent: Option[Artifact]) extends Artifact(config, parent) {
  protected var _name: Option[String] = None
  // Can be used to create additional variable bindings, e.g., for injectables
  protected[xml] val variableRefs = mutable.HashSet.empty[QName]

  protected[xml] def name_=(name: String): Unit = {
    _name = Some(name)
  }

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
    if (drp.isDefined) {
      for (port <- inputPorts) {
        val in = input(port).get
        if (relevantChildren(in.children.toList).isEmpty) {
          val pipe = new Pipe(config, in, drp.get.parent.get.name, drp.get.port.get)
          in.addChild(pipe)
        }
      }
    } else {
      for (port <- inputPorts) {
        val in = input(port).get
        var binding = Option.empty[DataSource]
        for (child <- relevantChildren()) {
          child match {
            case data: DataSource =>
              println("sdata $data")
              binding = Some(data)
            case _ => Unit
          }
        }

        if (binding.isEmpty) {
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
}
