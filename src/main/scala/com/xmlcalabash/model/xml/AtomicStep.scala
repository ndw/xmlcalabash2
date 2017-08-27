package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.model.exceptions.ModelException
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

class AtomicStep(override val config: ParserConfiguration,
                 override val parent: Option[Artifact],
                 val stepType: QName) extends PipelineStep(config, parent) {
  private var _name: Option[String] = None

  override def validate(): Boolean = {
    _name = properties.get(XProcConstants._name)
    var valid = true

    for (key <- List(XProcConstants._name)) {
      if (properties.contains(key)) {
        properties.remove(key)
      }
    }

    for (key <- properties.keySet) {
      if (key.getNamespaceURI == "") {
        throw new ModelException("badopt", s"Unexpected attribute: ${key.getLocalName}")
      }
    }

    val okChildren = List(classOf[Input], classOf[WithOption], classOf[Log])
    for (child <- children) {
      if (!okChildren.contains(child.getClass)) {
        throw new ModelException("badelem", s"Unexpected element: $child")
      }
      valid = valid && child.validate()
    }

    valid
  }

  override def makeInputPortsExplicit(): Boolean = {
    val sig = config.stepSignatures.step(stepType)

    for (port <- sig.inputPorts) {
      val siginput = sig.input(port)
      if (input(port).isEmpty) {
        val in = new Input(config, this, port, primary=siginput.primary, sequence=siginput.sequence)
        addChild(in)
      } else {
        val in = input(port).get
        in.primary = siginput.primary
        in.sequence = siginput.sequence
      }
    }

    for (port <- inputPorts) {
      if (!sig.inputPorts.contains(port)) {
        throw new ModelException("noport", s"Step has no port named $port")
      }
    }

    true
  }

  override def makeOutputPortsExplicit(): Boolean = {
    val sig = config.stepSignatures.step(stepType)

    for (port <- sig.outputPorts) {
      val sigoutput = sig.output(port)
      if (output(port).isEmpty) {
        val out = new Output(config, this, port, primary=sigoutput.primary, sequence=sigoutput.sequence)
        addChild(out)
      } else {
        val out = output(port).get
        out.primary = sigoutput.primary
        out.sequence = sigoutput.sequence
      }
    }

    for (port <- outputPorts) {
      if (!sig.outputPorts.contains(port)) {
        throw new ModelException("noport", s"Step has no port named $port")
      }
    }

    true
  }

  override def makeGraph(graph: Graph, parent: ContainerStart) {
    val node = parent.addAtomic(config.stepImplementation(stepType), name)
    graphNode = Some(node)
  }

  override def makeEdges(graph: Graph, parent: ContainerStart) {
    for (child <- children) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.makeEdges(graph, parent)
      }
    }
  }

  override def asXML: xml.Elem = {
    dumpAttr("name", _name.getOrElse(name))
    dumpAttr("id", id.toString)
    dumpAttr(properties.toMap)

    val nodes = ListBuffer.empty[xml.Node]
    if (children.nonEmpty) {
      nodes += xml.Text("\n")
    }
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem(stepType.getPrefix, stepType.getLocalName,
      dump_attr.getOrElse(xml.Null), namespaceScope, false, nodes:_*)
  }

}
