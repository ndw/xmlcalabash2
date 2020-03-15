package com.xmlcalabash.model.xml

import com.jafpl.graph.{ChooseStart, ContainerStart, Node, TryCatchStart}
import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.util.xc.ElaboratedPipeline
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class Try(override val config: XMLCalabashConfig) extends Container(config) with NamedArtifact {
  private var hasWhen = false
  private var hasOtherwise = false

  override def parse(node: XdmNode): Unit = {
    super.parse(node)
    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  override protected[model] def makeStructureExplicit(): Unit = {
    var hasSubpipeline = false
    var hasCatch = false
    var hasFinally = false
    val subpipeline = ListBuffer.empty[Artifact]
    val catches = ListBuffer.empty[Artifact]

    for (child <- allChildren) {
      child match {
        case output: DeclareOutput =>
          if (hasSubpipeline || hasCatch || hasFinally) {
            throw new RuntimeException("invalid children in p:try")
          }
          subpipeline += output
        case cat: Catch =>
          if (hasFinally) {
            throw new RuntimeException("catch cannot follow finally")
          }
          catches += cat
          hasCatch = true
        case fin: Finally =>
          if (hasFinally) {
            throw new RuntimeException("at most one p:finally is allowed")
          }
          catches += fin
          hasFinally = true
        case step: Step =>
          if (hasCatch || hasFinally) {
            throw new RuntimeException("steps cannot follow catch/finally")
          }
          subpipeline += step
          hasSubpipeline = true
      }
    }

    if (!hasSubpipeline || !(hasCatch || hasFinally)) {
      throw XProcException.xsMissingTryCatch(location)
    }

    val codes = mutable.HashSet.empty[QName]
    var noCode = false
    for (child <- children[Catch]) {
      if (child.codes.isEmpty) {
        if (noCode) {
          throw new RuntimeException("Only the last catch can omit the code")
        }
        noCode = true
      }
      for (code <- child.codes) {
        if (codes.contains(code)) {
          throw new RuntimeException("Only a single catch may specify a particular code")
        }
        codes += code
      }
    }

    if (subpipeline.size > 1 || !subpipeline.head.isInstanceOf[Group]) {
      val group = new Group(config)
      for (child <- subpipeline) {
        group.addChild(child)
      }
      removeChildren()
      addChild(group)
      for (child <- catches) {
        addChild(child)
      }
    }

    for (child <- allChildren) {
      child.makeStructureExplicit()
    }

    val outputSet = mutable.HashSet.empty[String]
    var primaryOutput = Option.empty[String]

    for (branch <- allChildren) {
      branch match {
        case group: Group =>
          for (child <- group.children[DeclareOutput]) {
            outputSet += child.port
            if (child.primary) {
              if (primaryOutput.isDefined) {
                if (primaryOutput.get != child.port) {
                  throw XProcException.xsBadTryOutputs(primaryOutput.get, child.port, location)
                }
              } else {
                primaryOutput = Some(child.port)
              }
            }
          }
        case cat: Catch =>
          for (child <- cat.children[DeclareOutput]) {
            outputSet += child.port
            if (child.primary) {
              if (primaryOutput.isDefined) {
                if (primaryOutput.get != child.port) {
                  throw XProcException.xsBadTryOutputs(primaryOutput.get, child.port, location)
                }
              } else {
                primaryOutput = Some(child.port)
              }
            }
          }
        case fin: Finally =>
          for (child <- fin.children[DeclareOutput]) {
            if (outputSet.contains(child.port)) {
              throw new RuntimeException("Bad output in p:finally")
            }
            outputSet += child.port
          }
        case _ => Unit
      }
    }

    val first = firstChild
    for (port <- outputSet) {
      val woutput = new WithOutput(config)
      woutput.port = port
      woutput.primary = (primaryOutput.isDefined && primaryOutput.get == port)
      addChild(woutput, first)
    }
  }

  override protected[model] def makeBindingsExplicit(): Unit = {
    for (child <- allChildren) {
      child.makeBindingsExplicit()
    }
  }

  override def graphNodes(runtime: XMLCalabashRuntime, parent: Node) {
    val start = parent.asInstanceOf[ContainerStart]
    val node = start.addTryCatch(stepName)
    _graphNode = Some(node)

    for (child <- children[Container]) {
      child.graphNodes(runtime, node)
    }
  }

  override def graphEdges(runtime: XMLCalabashRuntime, parent: Node) {
    super.graphEdges(runtime, parent)

    for (child <- children[Container]) {
      child.graphEdges(runtime, _graphNode.get)
    }

    val tryNode = _graphNode.get.asInstanceOf[TryCatchStart]
    for (child <- allChildren) {
      child match {
        case group: Group =>
          for (output <- group.children[DeclareOutput]) {
            runtime.graph.addEdge(group._graphNode.get, output.port, tryNode, output.port)
          }
        case cat: Catch =>
          for (output <- cat.children[DeclareOutput]) {
            runtime.graph.addEdge(cat._graphNode.get, output.port, tryNode, output.port)
          }
        case fin: Finally =>
          for (output <- fin.children[DeclareOutput]) {
            runtime.graph.addEdge(fin._graphNode.get, output.port, tryNode, output.port)
          }
        case _ => Unit
      }
    }
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startTry(tumble_id, stepName)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
    xml.endTry()
  }

  override def toString: String = {
    s"p:try $stepName"
  }
}