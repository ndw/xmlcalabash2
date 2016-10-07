package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcConstants, XProcEngine, XProcException}
import com.xmlcalabash.graph.{Graph, Node, XProcRuntime}
import com.xmlcalabash.model.xml.bindings.Pipe
import com.xmlcalabash.model.xml.decl.XProc10Steps
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/6/16.
  */
class PipelineDocument(node: Option[XdmNode], parent: Option[Artifact]) extends CompoundStep(node, parent) {
  override def fixup(): Unit = {
    if (parent.isEmpty) {
      findDeclarations(List(new XProc10Steps()))
      makeInputsOutputsExplicit()
      addDefaultReadablePort(None)
      fixUnwrappedInlines()
      fixBindingsOnIO()
      findPipeBindings()
      hoistOptions()
      refactorBoundaries()
      refactorForEach()
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

  private def refactorForEach(): Unit = {
    val newch = mutable.ListBuffer.empty[Artifact]

    for (child <- children) {
      child match {
        case forEach: ForEach =>
          val feBegin = new ForEachBegin(parent)
          val feEnd = new ForEachEnd(parent)
          var innerPortMap = mutable.HashMap.empty[InputOrOutput, InputOrOutput]
          var outerPortMap = mutable.HashMap.empty[InputOrOutput, InputOrOutput]

          var iterationSource: Option[IterationSource] = None
          var feOutputs = mutable.ListBuffer.empty[Output]
          var feChildren = mutable.ListBuffer.empty[Artifact]

          for (child <- forEach.children) {
            child match {
              case is: IterationSource => iterationSource = Some(is)
              case o: Output => feOutputs += o
              case a: Artifact => feChildren += a
            }
          }

          val input = new Input(None, Some(feBegin))
          iterationSource.get.children foreach { input.addChild }
          feBegin.addChild(input)

          val output = new Output(None, Some(feBegin))
          output.addProperty(XProcConstants._port, "current")
          feBegin.addChild(output)

          innerPortMap.put(iterationSource.get, output)

          for (output <- feOutputs) {
            val endInput = new Input(None, Some(feEnd))
            output.children foreach { endInput.addChild }
            feEnd.addChild(endInput)
            innerPortMap.put(output, endInput)

            val endOutput = new Output(None, Some(feEnd))
            output.addProperty(XProcConstants._port, "out_" + output.property(XProcConstants._port).get.value)
            feEnd.addChild(endOutput)
            outerPortMap.put(output, endOutput)
          }

          for (child <- feChildren) {
            for (remap <- innerPortMap.keySet) {
              child.adjustPortReference(remap, innerPortMap(remap))
            }
          }

          for (remap <- outerPortMap.keySet) {
            root.adjustPortReference(remap, outerPortMap(remap))
          }

          newch += feBegin
          newch ++= feChildren
          newch += feEnd
        case _ =>
          newch += child
      }
    }

    _children.clear()
    _children ++= newch
  }


  override def buildGraph(graph: Graph): Unit = {
    val nodeMap = mutable.HashMap.empty[Artifact, Node]
    buildNodes(graph, nodeMap)
    buildEdges(graph, nodeMap)
  }
}

