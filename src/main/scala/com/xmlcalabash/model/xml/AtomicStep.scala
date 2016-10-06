package com.xmlcalabash.model.xml

import java.io.PrintWriter

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.graph.{Graph, Node}
import com.xmlcalabash.model.xml.decl.{StepDecl, StepLibrary}
import com.xmlcalabash.model.xml.util.{RelevantNodes, TreeWriter}
import com.xmlcalabash.runtime.Identity
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.immutable.Set
import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class AtomicStep(node: Option[XdmNode], parent: Option[Artifact]) extends Step(node, parent) {
  private val _stepType = if (node.isDefined) {
    node.get.getNodeName
  } else {
    XProcConstants.px_anonymous_step_type
  }
  private var _decl: Option[StepDecl] = None

  _xmlname = "atomic-step"

  def stepType = _stepType
  def decl = _decl

  override private[model] def parseAttributes(node: XdmNode): Unit = {
    val propnames = Set(XProcConstants._name)
    for (childitem <- RelevantNodes.filter(node, Axis.ATTRIBUTE)) {
      val child = childitem.asInstanceOf[XdmNode]
      if (propnames.contains(child.getNodeName)) {
        _prop += new Attribute(child)
      } else {
        _attr += new Attribute(child)
      }
    }
  }

  override def findDeclarations(decls: List[StepLibrary]): Unit = {
    var decl: Option[StepDecl] = None
    for (lib <- decls) {
      if (decl.isEmpty) {
        decl = lib.steps.get(_stepType)
      }
    }
    _decl = decl
  }

  override def makeInputsOutputsExplicit(): Unit = {
    if (decl.isEmpty) {
      return
    }

    val ihash = mutable.HashMap.empty[String, Input]
    val ohash = mutable.HashMap.empty[String, Output]
    for (child <- _children) {
      child match {
        case inp: Input =>
          val port = inp.property(XProcConstants._port)
          if (port.isDefined) {
            ihash.put(port.get.value, inp)
          }
        case out: Output =>
          val port = out.property(XProcConstants._port)
          if (port.isDefined) {
            ohash.put(port.get.value, out)
          }
        case opt: DeclOption => Unit
      }
    }

    for (port <- decl.get.inputs.keySet) {
      val idecl = decl.get.inputs(port)
      val input = ihash.getOrElse(port, new Input(None, Some(this)))
      input.addProperty(XProcConstants._port, idecl.port)
      input.addProperty(XProcConstants._sequence, idecl.sequence.toString)
      input.addProperty(XProcConstants._primary, idecl.primary.toString)
      if (idecl.kind == "parameter") {
        input.addProperty(XProcConstants._kind, idecl.kind)
      }
      if (ihash.get(port).isEmpty) {
        _children += input
      }
    }

    for (port <- decl.get.outputs.keySet) {
      val odecl = decl.get.outputs(port)
      val output = ihash.getOrElse(port, new Output(None, Some(this)))
      output.addProperty(XProcConstants._port, odecl.port)
      output.addProperty(XProcConstants._sequence, odecl.sequence.toString)
      output.addProperty(XProcConstants._primary, odecl.primary.toString)
      if (ohash.get(port).isEmpty) {
        _children += output
      }
    }
  }

  private[xml] def extractOptions(): List[DeclOption] = {
    val newch = collection.mutable.ListBuffer.empty[Artifact]
    val opts  = collection.mutable.ListBuffer.empty[DeclOption]
    for (child <- children) {
      child match {
        case opt: DeclOption =>
          opts += opt
        case _ =>
          newch += child
      }
    }

    _children.clear()
    _children ++= newch

    opts.toList
  }

  override def buildNodes(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val node = graph.createNode(this.toString, new Identity(this.toString))
    nodeMap.put(this, node)
  }

  override def dumpAdditionalAttributes(tree: TreeWriter): Unit = {
    tree.addAttribute(XProcConstants.px("type"), _stepType.toString)
  }
}
