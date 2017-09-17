package com.xmlcalabash.model.xml

import com.jafpl.graph.{ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, StepProxy, XProcAvtExpression, XProcExpression}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AtomicStep(override val config: XMLCalabash,
                 override val parent: Option[Artifact],
                 val stepType: QName) extends PipelineStep(config, parent) {
  private val options = mutable.HashMap.empty[QName, XProcExpression]

  override def validate(): Boolean = {
    val sig = config.signatures.step(stepType)
    var valid = super.validate()

    for (key <- attributes.keySet) {
      if (key.getNamespaceURI == "") {
        if (sig.options.contains(key)) {
          val avt = ValueParser.parseAvt(attributes(key))
          if (avt.isDefined) {
            val context = new ExpressionContext(baseURI, inScopeNS, location)
            options.put(key, new XProcAvtExpression(context, avt.get))
          } else {
            throw new ModelException(ExceptionCode.BADAVT, List(key.toString, attributes(key)), location)
          }
        } else {
          throw new ModelException(ExceptionCode.BADATOMICATTR, key.getLocalName, location)
        }
      } else {
        val avt = ValueParser.parseAvt(attributes(key))
        if (avt.isDefined) {
          val context = new ExpressionContext(_baseURI, inScopeNS, _location)
          options.put(key, new XProcAvtExpression(context, avt.get))
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
    for (opt <- options.keySet) {
      val withOpt = new WithOption(config, this, opt, options(opt))
      addChild(withOpt)
    }

    var proxy: StepProxy = null
    val node = parent match {
      case start: ContainerStart =>
        val impl = config.stepImplementation(stepType, location.get)
        proxy = new StepProxy(config, impl)
        start.addAtomic(proxy, name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Atomic step parent isn't a container???", location)
    }
    graphNode = Some(node)
    proxy.nodeId = node.id
    config.addNode(node.id, this)

    for (child <- children) {
      child.makeGraph(graph, node)
    }
  }

  override def makeEdges(graph: Graph, parent: Node) {
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
