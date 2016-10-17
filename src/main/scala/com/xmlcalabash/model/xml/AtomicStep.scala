package com.xmlcalabash.model.xml

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.decl.{StepDecl, StepLibrary}
import com.xmlcalabash.model.xml.util.{RelevantNodes, TreeWriter}
import net.sf.saxon.s9api.{Axis, QName, XdmNode}

import scala.collection.immutable.Set
import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class AtomicStep(node: Option[XdmNode], parent: Option[Artifact]) extends Step(node, parent) {
  protected val _stepType = if (node.isDefined) {
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

  override def valid(listener: XMLErrorListener): Boolean = {
    var valid = true

    if (decl.isEmpty) {
      valid = false
      listener.error(node, s"No declaration for $stepType")
    } else {
      val optSet = mutable.HashSet.empty[QName]
      for (opt <- attributes()) {
        if (opt.getNamespaceURI == "") {
          if (decl.get.options.contains(opt)) {
            optSet += opt
          } else {
            valid = false
            listener.error(node, s"There is no '$opt' option")
          }
        } else {
          // Extension attributes are ok
        }
      }

      for (opt <- children.collect { case opt: WithOption => opt }) {
        if (opt.declaredName.isDefined) {
          val name = opt.declaredName.get
          if (decl.get.options.contains(name)) {
            if (optSet.contains(name)) {
              valid = false
              listener.error(opt.node, s"Duplicatated assignments to the '$name' option")
            } else {
              optSet += name
            }
          } else {
            listener.error(node, s"There is no '$name' option")
          }
        } else {
          valid = false
          listener.error(opt.node, "The 'name' attribute is required on p:with-option")
        }
      }

    }

    valid
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
        case opt: OptionDecl => Unit
        case opt: WithOption => Unit
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

  override def promoteShortcutOptions(): Unit = {
    for (qname <- attributes()) {
      val attr = attribute(qname)
      val opt = new WithOption(None, Some(this))
      opt.setProperty(XProcConstants._name, attr.get.name.toString)
      var value = attr.get.value.replace("'", "&apos;")
      opt.setProperty(XProcConstants._select, "'" + value + "'")
      opt.parseExpression(Some(attr.get.name), node.get)
      addChild(opt)
      removeAttribute(qname)
    }
  }

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    super.buildNodes(graph, engine, nodeMap)

    val impl = engine.implementation(stepType)

    if (node.isDefined && Option(node.get.getAttributeValue(XProcConstants._name)).isDefined) {
      impl.label = impl.label + "_" + node.get.getAttributeValue(XProcConstants._name)
    }

    val gnode = graph.createNode(impl)
    nodeMap.put(this, gnode)
  }

  override def dumpAdditionalAttributes(tree: TreeWriter): Unit = {
    tree.addAttribute(XProcConstants.px("type"), _stepType.toString)
  }
}
