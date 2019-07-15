package com.xmlcalabash.model.xml

import com.jafpl.graph.{ChooseStart, ContainerStart, Node, WhenStart}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{XMLCalabashRuntime, XProcXPathExpression}
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.XdmNode

import scala.collection.mutable

class Choose(override val config: XMLCalabashConfig) extends Container(config) {
  private var hasWhen = false
  private var hasOtherwise = false
  protected var ifexpr: Option[String] = None
  protected var ifcoll: Option[Boolean] = None

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    if (node.getNodeName == XProcConstants.p_if) {
      if (attributes.contains(XProcConstants._test)) {
        ifexpr = attr(XProcConstants._test)
      } else {
        throw new RuntimeException("p:if must have a test")
      }
      if (attributes.contains(XProcConstants._collection)) {
        ifcoll = staticContext.parseBoolean(attr(XProcConstants._collection))
      }
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    var firstChild = Option.empty[Artifact]
    var input = Option.empty[WithInput]
    for (child <- allChildren) {
      if (firstChild.isEmpty) {
        firstChild = Some(child)
      }
      child match {
        case winput: WithInput =>
          if (input.isDefined) {
            throw new RuntimeException("Only one with-input is allowed")
          }
          input = Some(winput)
        case when: When =>
          hasWhen = true
          when.makeStructureExplicit(environment)
        case otherwise: Otherwise =>
          hasOtherwise = true
          otherwise.makeStructureExplicit(environment)
      }
    }

    if (!hasWhen && !hasOtherwise) {
      throw XProcException.xsMissingWhen(location)
    }

    if (input.isEmpty) {
      val winput = new WithInput(config)
      winput.port = "#source"
      winput.primary = true
      if (firstChild.isDefined) {
        addChild(winput, firstChild.get)
      } else {
        addChild(winput)
      }
    }

    val outputSet = mutable.HashSet.empty[String]
    var primaryOutput = Option.empty[String]

    for (branch <- children[Container]) {
      for (child <- branch.children[DeclareOutput]) {
        outputSet += child.port
        if (child.primary) {
          primaryOutput = Some(child.port)
        }
      }
    }

    val first = firstChild
    for (port <- outputSet) {
      val woutput = new WithOutput(config)
      woutput.port = port
      woutput.primary = (primaryOutput.isDefined && primaryOutput.get == port)
      addChild(woutput, first)
    }

    if (!hasOtherwise) {
      val other = new Otherwise(config)
      other.test = "true()"

      val winput = new WithInput(config)
      winput.port = "source"
      other.addChild(winput)

      if (primaryOutput.isDefined) {
        val output = new DeclareOutput(config)
        output.port = primaryOutput.get
        output.primary = true
        other.addChild(output)
      }

      val identity = new AtomicStep(config)
      identity.stepType = XProcConstants.p_identity
      other.addChild(identity)

      addChild(other)
      other.makeStructureExplicit(environment)
    }
  }

  override protected[model] def makeBindingsExplicit(env: Environment, drp: Option[Port]): Unit = {
    for (child <- allChildren) {
      val cenv = new Environment(env)
      child.makeBindingsExplicit(cenv, drp)
    }
  }

  override protected[model] def validateStructure(): Unit = {
    super.validateStructure()

    var first = true
    var primaryOutput = Option.empty[DeclareOutput]
    for (child <- allChildren) {
      child match {
        case _: WithInput => Unit
        case _: WithOutput => Unit
        case when: When =>
          if (first) {
            for (child <- when.children[DeclareOutput]) {
              if (child.primary) {
                primaryOutput = Some(child)
              }
            }
            first = false
          }

          var foundPrimary = false
          for (child <- when.children[DeclareOutput]) {
            if (child.primary) {
              foundPrimary = true
              if (primaryOutput.isEmpty) {
                throw XProcException.xsBadChooseOutputs("#NONE", child.port, location)
              }
              if (primaryOutput.isDefined && primaryOutput.get.port != child.port) {
                throw XProcException.xsBadChooseOutputs(primaryOutput.get.port, child.port, location)
              }
            }
            if (!foundPrimary && primaryOutput.isDefined) {
              throw XProcException.xsBadChooseOutputs(primaryOutput.get.port, "#NONE", location)
            }
          }

        case _: Otherwise => Unit
        case _ =>
          throw new RuntimeException(s"Invalid content in $this")
      }
    }
  }

  override protected[model] def normalizeToPipes(): Unit = {
    super.normalizeToPipes()

    // Now that we've distributed the input into the p:when's, we can remove this
    val winput = firstWithInput
    if (winput.isDefined) {
      removeChild(winput.get)
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node) {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addChoose(stepName)
    _graphNode = Some(node)

    for (child <- children[Step]) {
      child.graphNodes(runtime, node)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node) {
    for (child <- children[Container]) {
      child.graphEdges(runtime, _graphNode.get)
    }

    val chooseNode = _graphNode.get.asInstanceOf[ChooseStart]
    for (branch <- children[ChooseBranch]) {
      val branchNode = branch._graphNode.get
      for (output <- branch.children[DeclareOutput]) {
        runtime.graph.addEdge(branchNode, output.port, chooseNode, output.port)
      }
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startChoose(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endChoose()
  }

  override def toString: String = {
    s"p:choose $stepName"
  }
}