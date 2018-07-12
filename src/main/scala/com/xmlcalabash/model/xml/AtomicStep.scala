package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.ValueParser
import com.xmlcalabash.runtime.{ExpressionContext, ImplParams, StaticContext, StepProxy, XProcAvtExpression, XProcExpression}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AtomicStep(override val config: XMLCalabash,
                 override val parent: Option[Artifact],
                 override val stepType: QName,
                 params: Option[ImplParams]) extends PipelineStep(config, parent, stepType) {
  protected[xml] val options = mutable.HashMap.empty[QName, XProcExpression]

  def this(config: XMLCalabash, parent: Option[Artifact], stepType: QName) = {
    this(config, parent, stepType, None)
  }

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

    val okChildren = List(classOf[WithInput], classOf[WithOption])
    for (child <- relevantChildren) {
      if (!okChildren.contains(child.getClass)) {
        throw XProcException.xsElementNotAllowed(location, child.nodeName)
      }
      valid = valid && child.validate()
    }

    valid
  }

  override def makeInputPortsExplicit(): Boolean = {
    val sig = config.signatures.step(stepType)

    // Work out which input port is primary
    var primaryInput = Option.empty[String]
    for (port <- sig.inputPorts) {
      val siginput = sig.input(port, location.get)
      if (siginput.primary) {
        primaryInput = Some(siginput.name)
      }
    }

    // If the port has been omitted, it refers to the primary port
    for (input <- inputs) {
      if (input.port.isEmpty) {
        if (primaryInput.isEmpty) {
          throw new ModelException(ExceptionCode.NOPRIMARYINPUTPORT, List(stepType.toString), location)
        } else {
          input.port = primaryInput.get
        }
      }
    }

    // It's an error to have two bindings for the same port name
    val seenPorts = mutable.HashSet.empty[String]
    for (input <- inputs) {
      val port = input.port.get
      if (seenPorts.contains(port)) {
        throw new ModelException(ExceptionCode.DUPINPUTPORT, List(port), location)
      }
      seenPorts += port
    }

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

    val context = new StaticContext()
    context.baseURI = baseURI

    var proxy: StepProxy = null
    val node = parent match {
      case start: ContainerStart =>
        val impl = config.stepImplementation(stepType, location.get)
        proxy = new StepProxy(config, stepType, impl, params, context)

        for (port <- inputPorts) {
          val in = input(port)
          if (in.get.select.isDefined) {
            proxy.setDefaultSelect(port, in.get.selectExpression)
          }
        }

        proxy.setLocation(location.get)
        start.addAtomic(proxy, name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Atomic step parent isn't a container???", location)
    }
    _graphNode = Some(node)
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

    for (ref <- variableRefs) {
      val bind = findBinding(ref)
      if (bind.isEmpty) {
        throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
      }

      bind.get match {
          /*
        case declStep: DeclareStep =>
          // FIXME: Does this case ever happen anymore?
          var optDecl = Option.empty[OptionDecl]
          for (child <- declStep.children) {
            child match {
              case opt: OptionDecl =>
                if (opt.optionName == ref) {
                  optDecl = Some(opt)
                }
              case _ => Unit
            }
          }
          if (optDecl.isEmpty) {
            throw new ModelException(ExceptionCode.NOBINDING, ref.toString, location)
          }
          graph.addBindingEdge(optDecl.get._graphNode.get.asInstanceOf[Binding], graphNode)
          */
        case optDecl: OptionDecl =>
          graph.addBindingEdge(optDecl._graphNode.get.asInstanceOf[Binding], graphNode)
        case varDecl: Variable =>
          graph.addBindingEdge(varDecl._graphNode.get.asInstanceOf[Binding], graphNode)
        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", location)
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
