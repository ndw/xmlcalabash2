package com.xmlcalabash.model.xml.bindings

import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml._
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
    super.addDefaultReadablePort(port)
  }

  override def findPipeBindings(): Unit = {
    if (_port.isDefined) {
      // Already found (probably by p:xpath-context in p:when)
      return
    }

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
            var ancestor = false
            var p = parent
            while (!ancestor && p.isDefined) {
              ancestor = (p == _step)
              p = p.get.parent
            }

            if (ancestor) {
              for (child <- compound.children) {
                child match {
                  case i: Input =>
                    val name = i.property(XProcConstants._port)
                    if (name.isDefined && (name.get.value == portName)) {
                      _port = Some(i)
                    }
                  case is: IterationSource =>
                    if (portName == "current") {
                      _port = Some(is)
                    }
                  case _ => Unit
                }
              }
            } else {
                for (child <- compound.children) {
                  child match {
                    case o: Output =>
                      val name = o.property(XProcConstants._port)
                      if (name.isDefined && (name.get.value == portName)) {
                        _port = Some(o)
                      }
                    case _ => Unit
                  }
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
            var ancestor = false
            var p = parent
            while (!ancestor && p.isDefined) {
              ancestor = (p == _step)
              p = p.get.parent
            }

            if (ancestor) {
              for (child <- compound.children) {
                child match {
                  case i: Input =>
                    if (i.primary) {
                      _port = Some(i)
                    }
                  case is: IterationSource =>
                    _port = Some(is)
                  case _ => Unit
                }
              }
            } else {
              for (child <- compound.children) {
                child match {
                  case o: Output =>
                    if (o.primary) {
                      _port = Some(o)
                    }
                  case _ => Unit
                }
              }
            }
        }
      }
    }
  }

  override def adjustPortReference(node: InputOrOutput, replacement: InputOrOutput): Unit = {
    if (_port.isDefined && _port.get == node) {
      _port = Some(replacement)
    }
    for (child <- _children) { child.adjustPortReference(node, replacement) }
  }

  override def buildEdges(graph: Graph, nodeMap: mutable.HashMap[Artifact, Node]): Unit = {
    if (_port.isEmpty || _port.get.parent.isEmpty) {
      println("ERROR")
    }

    val srcArtifact = _port.get.parent.get
    val resArtifact = parent.get.parent.get
    var inPort      = "???" // _port.get.property(XProcConstants._port).get.value
    var outPort     = "???"
    var srcNode: Node = null
    var resNode: Node = null

    //println("Edges on " + this + ": " + "from " + srcArtifact + " to " + resArtifact)

    _port.get match {
      case x: IterationSource => outPort = "current"
      case _ =>
        val port = _port.get.property(XProcConstants._port)
        if (port.isDefined) {
          outPort = port.get.value
        } else {
          println("No source port on " + parent.get)
        }
    }

    parent.get match {
      case x: XPathContext =>
        inPort = "source"
      case x: IterationSource =>
        inPort = "source"
      case _ =>
        if (parent.isDefined) {
          val port = parent.get.property(XProcConstants._port)
          if (port.isDefined) {
            inPort = port.get.value
          } else {
            println("No result port on " + parent.get)
          }
        } else {
          println("No parent on " + this)
        }
    }

    srcArtifact match {
      case loop: ForEach =>
        var inside = false
        var p: Option[Artifact] = Some(resArtifact)
        while (!inside && p.isDefined) {
          inside = (p.get == loop)
          p = p.get.parent
        }
        if (inside) {
          srcNode = loop.loopStart
        } else {
          srcNode = loop.loopStart.compoundEnd
        }
      case choose: Choose =>
        srcNode = choose.chooseStart.compoundEnd
      case group: Group =>
        srcNode = group.compoundStart.compoundEnd
      case _ =>
        srcNode = nodeMap(srcArtifact)
    }

    resArtifact match {
      case loop: ForEach =>
        var inside = false
        var p: Option[Artifact] = Some(srcArtifact)
        while (!inside && p.isDefined) {
          inside = (p.get == loop)
          p = p.get.parent
        }
        if (inside) {
          resNode = loop.loopStart.compoundEnd
          inPort = "I_" + inPort
        } else {
          resNode = loop.loopStart
        }
      case when: When =>
        if (parent.isEmpty || !parent.get.isInstanceOf[XPathContext]) {
          resNode = when.whenStart.compoundEnd
          inPort = "I_" + outPort
        }  else {
          resNode = when.whenStart
        }
      case group: Group =>
        if (parent.isEmpty || !parent.get.isInstanceOf[XPathContext]) {
          resNode = group.compoundStart.compoundEnd
          inPort = "I_" + outPort
        }  else {
          resNode = group.compoundStart.asInstanceOf[Node]
        }
      case _ =>
        resNode = nodeMap(resArtifact)
    }

    //println("    " + srcNode + "/" + outPort + " -> " + resNode + "/" + inPort)

    graph.addEdge(srcNode, outPort, resNode, inPort)
  }

  def connectAs(pipe: Pipe): Unit = {
    _port = pipe._port
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
