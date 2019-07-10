package com.xmlcalabash.model.xml

import com.jafpl.graph.Node
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

class Pipe(override val config: XMLCalabashConfig, shortcut: Option[String]) extends DataSource(config) {
  def this(config: XMLCalabashConfig) = {
    this(config, None)
  }
  def this(config: XMLCalabashConfig, shortcut: String) = {
    this(config, Some(shortcut))
  }

  def this(pipe: Pipe) {
    this(pipe.config)
    _step = pipe._step
    _port = pipe._port
    _link = pipe._link
  }

  private var _step = Option.empty[String]
  private var _port = Option.empty[String]
  private var _link = Option.empty[Port]

  def step: String = _step.get
  protected[model] def step_=(step: String): Unit = {
    _step = Some(step)
  }
  def port: String = _port.get

  protected[model] def port_=(port: String): Unit = {
    _port = Some(port)
  }
  def link: Option[Port] = _link
  def link_=(port: Port): Unit = {
    _link = Some(port)
  }

  override protected[model] def parse(node: XdmNode): Unit = {
    super.parse(node)

    _step = attr(XProcConstants._step)
    _port = attr(XProcConstants._port)

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    // nop
  }

  override protected[model] def makeBindingsExplicit(env: Environment, drp: Option[Port]): Unit = {
    for (child <- allChildren) {
      child.makeBindingsExplicit(env, drp)
    }

    if (_link.isEmpty) {
      if (_step.isEmpty) {
        if (drp.isEmpty) {
          throw XProcException.xsPipeWithoutStepOrDrp(location)
        }
        _step = Some(drp.get.step.stepName)
      }

      val step = env.step(_step.get)
      val thisStep = containingStep.get
      if (_port.isEmpty) {
        if (step.isEmpty) {
          throw XProcException.xsPortNotReadableNoStep(_step.get, location)
        }

        if (thisStep.ancestor(step.get)) {
          if (step.get.primaryInput.isEmpty) {
            throw XProcException.xsPortNotReadableNoPrimaryInput(_step.get, location)
          } else {
            _port = Some(step.get.primaryInput.get.port)
          }
        } else {
          if (step.get.primaryOutput.isEmpty) {
            throw XProcException.xsPortNotReadableNoPrimaryOutput(_step.get, location)
          } else {
            _port = Some(step.get.primaryOutput.get.port)
          }
        }
      }

      if (thisStep == step.get) {
        // Special case of a loop
        throw XProcException.xsLoop(_step.get, _port.get, location)
      }

      for (child <- step.get.allChildren) {
        child match {
          case input: DeclareInput =>
            if (input.port == _port.get) {
              _link = Some(input)
            }
          case output: DeclareOutput =>
            if (output.port == _port.get) {
              _link = Some(output)
            }
          case output: WithOutput =>
            if (output.port == _port.get) {
              _link = Some(output)
            }
          case _ => Unit
        }
      }

      if (_link.isEmpty) {
        throw XProcException.xsPortNotReadable(_step.get, _port.get, location)
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    if (allChildren.nonEmpty) {
      throw new RuntimeException(s"Invalid content in $this")
    }
  }

  override protected[model] def normalizeToPipes(): Unit = {
    // nop
  }

  override protected[model] def insertPipe(source: Port, pipe: Pipe): Unit = {
    if (link.get == source) {
      val newPipe = new Pipe(config)
      newPipe.step = pipe.step
      newPipe.port = pipe.port
      newPipe.link = pipe.link.get
      parent.get.addChild(newPipe, this)
    }
  }

  override protected[model] def replumb(oldSource: Port, newSource: Port): Unit = {
    if (link.get == oldSource) {
      link = newSource
      step = newSource.parent.get.asInstanceOf[Step].stepName
      port = newSource.port
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parNode: Node): Unit = {
    val toNode = parNode
    val toPort = parent.get.asInstanceOf[Port].port
    val fromNode = link.get.parent.get._graphNode.get
    val fromPort = link.get.port
    //println(s"  EDGE: $fromNode/$fromPort => $toNode/$toPort")
    runtime.graph.addOrderedEdge(fromNode, fromPort, toNode, toPort)
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startPipe(tumble_id, step, port)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endPipe()
  }

  override def toString: String = {
    val sstep = _step.getOrElse("???")
    val sport = _port.getOrElse("???")

    if (tumble_id.startsWith("!syn")) {
      s"p:pipe from $sstep/$sport"
    } else {
      s"p:pipe from $sstep/$sport $tumble_id"
    }
  }
}
