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
  private var _matchAvt = List.empty[String]

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (attributes.contains(XProcConstants._match)) {
      _match = attr(XProcConstants._match).get
      _matchAvt = staticContext.parseAvt(_match)
    } else {
      throw new RuntimeException("Viewport must have match")
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    val first = firstChild

    if (firstWithInput.isEmpty) {
      val input = new WithInput(config)
      input.port = "source"
      addChild(input, first)
    }

    val current = new DeclareInput(config)
    current.port = "current"
    current.primary = true
    addChild(current, first)

    makeContainerStructureExplicit(environment)
  }

  override protected[model] def makeBindingsExplicit(env: Environment, drp: Option[Port]): Unit = {
    super.makeBindingsExplicit(env, drp)

    val bindings = mutable.HashSet.empty[QName]
    bindings ++= staticContext.findVariableRefsInAvt(_matchAvt)

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

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node) {
    val start = parent.asInstanceOf[ContainerStart]
    val context = staticContext.withStatics(inScopeStatics)
    val composer = new XMLViewportComposer(config, context, _match)
    val node = start.addViewport(composer, stepName)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, node)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node) {
    val winput = firstWithInput
    if (winput.isDefined) {
      for (child <- winput.get.allChildren) {
        child match {
          case pipe: Pipe =>
            pipe.graphEdges(runtime, _graphNode.get)
          //runtime.graph.addEdge(pipe.link.get.parent.get._graphNode.get, pipe.port, _graphNode.get, "source")
          case pipe: NamePipe =>
            pipe.graphEdges(runtime, _graphNode.get)
          case _ => Unit
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