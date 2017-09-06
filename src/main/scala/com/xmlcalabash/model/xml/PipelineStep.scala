package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.xml.datasource.{DataSource, Pipe}

class PipelineStep(override val config: XMLCalabash,
                   override val parent: Option[Artifact]) extends Artifact(config, parent) {

  def makeInputPortsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makeInputPortsExplicit")
    false
  }

  def makeOutputPortsExplicit(): Boolean = {
    println(s"ERROR: $this doesn't override makeOutputPortsExplicit")
    false
  }

  def makeInputBindingsExplicit(): Boolean = {
    val drp = defaultReadablePort()
    if (drp.isDefined) {
      for (port <- inputPorts) {
        val in = input(port).get
        if (in.children.isEmpty) {
          val pipe = new Pipe(config, in, drp.get.parent.get.name, drp.get.port.get)
          in.addChild(pipe)
        }
      }
    } else {
      for (port <- inputPorts) {
        val in = input(port).get
        var binding = Option.empty[DataSource]
        for (child <- children) {
          child match {
            case data: DataSource =>
              binding = Some(data)
            case _ => Unit
          }
        }
        valid = valid && binding.isDefined
      }
    }

    valid
  }

  def makeOutputBindingsExplicit(): Boolean = {
    true
  }

  override def makePortsExplicit(): Boolean = {
    makeInputPortsExplicit() && makeOutputPortsExplicit()
  }

  override def makeBindingsExplicit(): Boolean = {
    makeInputBindingsExplicit() && makeOutputBindingsExplicit()
  }

  override def makeEdges(graph: Graph, parent: Node) {
    graphEdges(graph, graphNode.get.asInstanceOf[ContainerStart])
  }

}
