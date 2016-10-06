package com.xmlcalabash.model.xml

import java.io.PrintWriter

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.graph.{Graph, Node}
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

/**
  * Created by ndw on 10/4/16.
  */
class Pipe(node: Option[XdmNode], parent: Option[Artifact]) extends Binding(node, parent) {
  var _step: Option[Step] = None
  var _port: Option[InputOrOutput] = None
  _xmlname = "pipe"

  override def addDefaultReadablePort(port: Option[InputOrOutput]): Unit = {
    _drp = port
    for (child <- _children) { child.addDefaultReadablePort(port) }
  }

  override def findPipeBindings(): Unit = {
    if (property(XProcConstants._step).isDefined) {
      val stepName = property(XProcConstants._step).get.value
      _step = findInScopeStep(stepName)
    } else {
      if (_drp.isDefined) {
        _step = Some(_drp.get.parent.get.asInstanceOf[Step])
      }
    }

    if (_step.isDefined) {
      if (property(XProcConstants._port).isDefined) {
        val portName = property(XProcConstants._port).get.value

        _step.get match {
          case atomic: AtomicStep =>
            for (child <- atomic.children) {
              child match {
                case o: Output =>
                  val name = o.property(XProcConstants._port)
                  if (name.isDefined && (name.get.value == portName)) {
                    _port = Some(o)
                  }
                case _ => Unit
              }
            }
          case compound: CompoundStep =>
            for (child <- compound.children) {
              child match {
                case i: Input =>
                  val name = i.property(XProcConstants._port)
                  if (name.isDefined && (name.get.value == portName)) {
                    _port = Some(i)
                  }
                case _ => Unit
              }
            }
        }
      } else {
        _step.get match {
          case atomic: AtomicStep =>
            for (child <- atomic.children) {
              child match {
                case o: Output =>
                  if (o.primary) {
                    _port = Some(o)
                  }
                case _ => Unit
              }
            }
          case compound: CompoundStep =>
            for (child <- compound.children) {
              child match {
                case i: Input =>
                  if (i.primary) {
                    _port = Some(i)
                  }
                case _ => Unit
              }
            }
        }
      }
    }
  }

  override def replaceNode(node: InputOrOutput, replacement: InputOrOutput): Unit = {
    if (_port.isDefined && _port.get == node) {
      _port = Some(replacement)
    }
    for (child <- _children) { child.replaceNode(node, replacement) }
  }

  override def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    val srcArtifact = parent.get.parent.get
    val srcPort     = _port.get.property(XProcConstants._port).get.value
    val resArtifact = _port.get.parent.get
    val resPort     = parent.get.property(XProcConstants._port).get.value
    //println(srcArtifact, srcPort, resArtifact, resPort)

    graph.addEdge(nodeMap(resArtifact), resPort, nodeMap(srcArtifact), srcPort)
  }


  override def dumpAdditionalAttributes(tree: TreeWriter): Unit = {
    if (_port.isDefined) {
      tree.addAttribute(XProcConstants.px("port"), _port.get.toString)
    } else {
      if (_step.isDefined) {
        tree.addAttribute(XProcConstants.px("step"), _step.get.toString)
      }
    }
  }
}
