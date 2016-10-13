package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcConstants, XProcEngine, XProcException}
import com.jafpl.graph.{Graph, Node}
import com.xmlcalabash.model.xml.bindings.Pipe
import com.xmlcalabash.model.xml.decl.{XProc10Steps, XProc11Steps}
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/6/16.
  */
class PipelineDocument(node: Option[XdmNode], parent: Option[Artifact]) extends CompoundStep(node, parent) {
  override def fixup(): Unit = {
    if (parent.isEmpty) {
      findDeclarations(List(new XProc11Steps()))
      makeInputsOutputsExplicit()
      addDefaultReadablePort(None)
      fixUnwrappedInlines()
      fixBindingsOnIO()
      findPipeBindings()
      hoistOptions()
      refactorBoundaries()
    }
  }

  private def refactorBoundaries(): Unit = {
    val newch = collection.mutable.ListBuffer.empty[Artifact]

    for (child <- children) {
      child match {
        case input: Input =>
          val edge = new InputEdge(input.port, this)
          edge.addProperty(XProcConstants._port, input.port)
          val output = new Output(None, Some(edge))
          output.addProperty(XProcConstants._port, "result")
          if (input.sequence) {
            output.addProperty(XProcConstants._sequence, "true")
          }
          edge.addChild(output)
          newch += edge
          adjustPortReference(input, output)
        case output: Output =>
          val edge = new OutputEdge(output.port, this)
          edge.addProperty(XProcConstants._port, output.port)
          val input = new Input(None, Some(edge))
          input.addProperty(XProcConstants._port, "source")
          if (output.sequence) {
            input.addProperty(XProcConstants._sequence, "true")
          }
          edge.addChild(input)
          for (binding <- output.bindings()) {
            binding match {
              case pipe: Pipe =>
                val newPipe = new Pipe(None, Some(input))
                for (prop <- pipe.properties()) {
                  newPipe.addProperty(prop, pipe.property(prop).get.value)
                }
                newPipe._port = pipe._port
                input.addChild(newPipe)
            }
          }
          newch += edge
        case _ =>
          newch += child
      }
    }

    _children.clear()
    _children ++= newch
  }

  override def buildGraph(graph: Graph, engine: XProcEngine): Unit = {
    val nodeMap = mutable.HashMap.empty[Artifact, Node]
    buildNodes(graph, engine, nodeMap)
    buildEdges(graph, nodeMap)
  }
}

