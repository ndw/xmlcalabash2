package com.xmlcalabash.model.xml

import com.jafpl.graph.{ChooseStart, Graph, Node}
import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.util.WhenOrOtherwise
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/4/16.
  */
class Choose(node: Option[XdmNode], parent: Option[Artifact]) extends CompoundStep(node, parent) {
  private[xml] var chooseStart: ChooseStart = _

  override def makeInputsOutputsExplicit(): Unit = {
    for (child <- children) {
      child.makeInputsOutputsExplicit()
    }

    // A p:choose has no inputs or outputs.
    // However, in this model, we manufacture p:output elements that are the union of all
    // the p:output elements of all the p:when children.
    val portSet = mutable.HashSet.empty[String]
    var primary: Option[String] = None
    for (when <- children.collect { case when: WhenOrOtherwise => when }) {
      var wprimary: Option[Output] = None
      for (output <- when.children.collect { case out: Output => out }) {
        if (!portSet.contains(output.port)) {
          portSet += output.port
        }
        if (output.primary) {
          wprimary = Some(output)
        }
      }

      // Is the same primary output port defined by all when/otherwise clauses?
      // If this when/otherwise defines a primary
      //   Then yes as long as it's the first one or has the same name as the last one
      // Else
      //   No
      if (wprimary.isDefined) {
        if (primary.isEmpty) {
          primary = Some(wprimary.get.port)
        } else {
          if (wprimary.get.port != primary.get) {
            primary = Some("")
          }
        }
      } else {
        primary = Some("")
      }
    }

    val newChildren = ListBuffer.empty[Artifact]
    for (port <- portSet) {
      val output = new Output(None, Some(this))
      output.setProperty(XProcConstants._port, port)
      if (primary.isDefined && primary.get == port) {
        output.setProperty(XProcConstants._primary, "true")
      }
      newChildren += output
    }
    newChildren ++= _children
    _children.clear()
    _children ++= newChildren

    val context = children.collect { case ctx: XPathContext => ctx }
    if (context.isEmpty) {
      val newChildren = ListBuffer.empty[Artifact]
      val ctx = new XPathContext(None, Some(this))
      newChildren += ctx
      newChildren ++= _children
      _children.clear()
      _children ++= newChildren
    }

  }

  override def buildNodes(graph: Graph, engine: XProcEngine, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val subpipeline = ListBuffer.empty[Node]

    for (child <- children) {
      child.buildNodes(graph, engine, nodeMap)
      child match {
        case when: When =>
          subpipeline += when.whenStart
        case _ => Unit
      }
    }

    chooseStart = graph.createChooseNode(subpipeline.toList)
  }

  override private[xml] def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    for (child <- children) {
      child match {
        case ctx: XPathContext => Unit // No edge necessary for the default XPathContext
        case _ =>
          child.buildEdges(graph, nodeMap)
      }
    }
  }

}
