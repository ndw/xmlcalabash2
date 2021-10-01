package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.steps.Manifold
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable

class Viewport(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  private var _match: String = _

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._match)) {
      _match = attr(XProcConstants._match).get
    } else {
      throw new RuntimeException("Viewport must have match")
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    val first = firstChild
    if (firstWithInput.isDefined) {
      val fwi = firstWithInput.get
      fwi.port match {
        case "" =>
          // It may be anonymous in XProc, but it mustn't be anonymous in the graph
          fwi.port = "source"
        case "source" => ()
        case _ => throw XProcException.xiThisCantHappen(s"Viewport withinput is named '${fwi.port}''", location)
      }
    } else {
      val input = new WithInput(config)
      input.port = "source"
      input.primary = true
      addChild(input, first)
    }

    val current = new DeclareInput(config)
    current.port = "current"
    current.primary = true
    addChild(current, first)

    makeContainerStructureExplicit()
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    super.makeBindingsExplicit()

    val env = environment()

    val bindings = mutable.HashSet.empty[QName]
    bindings ++= staticContext.findVariableRefsInString(_match)

    if (bindings.nonEmpty) {
      var winput = firstWithInput
      if (winput.isEmpty) {
        val input = new WithInput(config)
        input.port = "source"
        addChild(input, firstChild)
        winput = Some(input)
      }
      for (ref <- bindings) {
        val binding = env.variable(ref)
        if (binding.isEmpty) {
          throw new RuntimeException("Reference to undefined variable")
        }
        if (!binding.get.static) {
          val pipe = new NamePipe(config, ref, binding.get.tumble_id, binding.get)
          winput.get.addChild(pipe)
        }
      }
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    val start = parent.asInstanceOf[ContainerStart]
    val context = staticContext.withStatics(inScopeStatics)
    val composer = new XMLViewportComposer(config, context, _match)
    val node = start.addViewport(composer, stepName, containerManifold)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, node)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node): Unit = {
    super.graphEdges(runtime, parent)

    val winput = firstWithInput
    if (winput.isDefined) {
      for (child <- winput.get.allChildren) {
        child match {
          case pipe: Pipe =>
            pipe.graphEdges(runtime, _graphNode.get)
          case pipe: NamePipe =>
            pipe.graphEdges(runtime, _graphNode.get)
          case _ => ()
        }
      }
    }

    for (output <- children[DeclareOutput]) {
      for (pipe <- output.children[Pipe]) {
        runtime.graph.addEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, output.port)
      }
    }

    for (child <- children[Step]) {
      child.graphEdges(runtime, _graphNode.get)
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startViewport(tumble_id, stepName, _match)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endViewport()
  }

  override def toString: String = {
    s"p:viewport $stepName"
  }
}