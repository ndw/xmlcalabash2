package com.xmlcalabash.model.xml

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.AvtParser
import com.xmlcalabash.model.xml.util.WithOptionData
import com.xmlcalabash.runtime.{StepProxy, XProcAvtExpression, XProcExpression}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AtomicStep(override val config: XMLCalabash,
                 override val parent: Option[Artifact],
                 val stepType: QName) extends PipelineStep(config, parent) {
  private var _name: Option[String] = None
  private val options = mutable.HashMap.empty[QName, XProcExpression]

  override def validate(): Boolean = {
    val sig = config.signatures.step(stepType)
    var valid = true

    _name = attributes.get(XProcConstants._name)
    if (_name.isDefined) {
      label = _name.get
    } else {
      label = stepType.getLocalName
    }

    for (key <- List(XProcConstants._name)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    for (key <- attributes.keySet) {
      if (key.getNamespaceURI == "") {
        if (sig.options.contains(key)) {
          val avt = AvtParser.parse(attributes(key))
          if (avt.isDefined) {
            options.put(key, new XProcAvtExpression(inScopeNS, avt.get))
          } else {
            throw new ModelException(ExceptionCode.BADAVT, List(key.toString, attributes(key)), location)
          }
        } else {
          throw new ModelException(ExceptionCode.BADATOMICATTR, key.getLocalName, location)
        }
      } else {
        val avt = AvtParser.parse(attributes(key))
        if (avt.isDefined) {
          options.put(key, new XProcAvtExpression(inScopeNS, avt.get))
        } else {
          throw new ModelException(ExceptionCode.BADAVT, List(key.toString, attributes(key)), location)
        }
      }
    }

    val okChildren = List(classOf[Input], classOf[WithOption], classOf[Log])
    for (child <- relevantChildren()) {
      if (!okChildren.contains(child.getClass)) {
        throw new ModelException(ExceptionCode.BADCHILD, child.toString, location)
      }
      valid = valid && child.validate()
    }

    valid
  }

  override def makeInputPortsExplicit(): Boolean = {
    val sig = config.signatures.step(stepType)

    for (port <- sig.inputPorts) {
      val siginput = sig.input(port, location.get)
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
        throw new ModelException(ExceptionCode.BADATOMICINPUTPORT, List(stepType.toString, port), location)
      }
    }

    true
  }

  override def makeOutputPortsExplicit(): Boolean = {
    val sig = config.signatures.step(stepType)

    for (port <- sig.outputPorts) {
      val sigoutput = sig.output(port, location.get)
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
        throw new ModelException(ExceptionCode.BADATOMICINPUTPORT, List(stepType.toString, port), location)
      }
    }

    true
  }

  override def makeGraph(graph: Graph, parent: Node) {
    val node = parent match {
      case start: ContainerStart =>
        val impl = config.stepImplementation(stepType, location.get)
        val withOptions = ListBuffer.empty[WithOptionData]
        for (child <- children) {
          child match {
            case opt: WithOption =>
              withOptions += new WithOptionData(opt.optionName, opt.dataPort, opt.select, inScopeNS)
            case _ => Unit
          }
        }
        val proxy = new StepProxy(impl, options.toMap, withOptions.toList, inScopeNS)
        start.addAtomic(proxy, name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Atomic step parent isn't a container???", location)
    }
    graphNode = Some(node)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parent: Node) {
    for (opt <- options.keySet) {
      val drp = defaultReadablePort()
      if (drp.isDefined) {
        val port = "#" + opt.toString
        drp.get match {
          case out: Output =>
            graph.addEdge(out.parent.get.graphNode.get, out.port.get, graphNode.get, port)
          case in: Input =>
            graph.addEdge(in.parent.get.graphNode.get, in.port.get, graphNode.get, port)
          case _ =>
            throw new PipelineException("notimpl", "not implemented reading from: " + drp.get, location)
        }
      }
    }

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
    dumpAttr(attributes.toMap)

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
