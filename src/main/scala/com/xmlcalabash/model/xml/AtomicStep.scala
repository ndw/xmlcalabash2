package com.xmlcalabash.model.xml

import com.jafpl.graph.{Binding, ContainerStart, Graph, Node}
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import com.xmlcalabash.runtime.{ExpressionContext, ImplParams, StaticContext, StepExecutable, StepProxy, StepRunner, StepWrapper, XMLCalabashRuntime, XProcExpression, XProcVtExpression, XProcXPathExpression, XmlStep}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AtomicStep(override val config: XMLCalabashRuntime,
                 override val parent: Option[Artifact],
                 override val stepType: QName,
                 params: Option[ImplParams]) extends PipelineStep(config, parent) {
  protected[xml] val options = mutable.HashMap.empty[QName, XProcExpression]
  staticContext.stepType = stepType

  def this(config: XMLCalabashRuntime, parent: Option[Artifact], stepType: QName) = {
    this(config, parent, stepType, None)
  }

  override def validate(): Boolean = {
    var valid = super.validate()
    val stepType = staticContext.stepType

    val sig = if (parent.isDefined) {
      parent.get.stepSignature(stepType).getOrElse(config.signatures.step(stepType))
    } else {
      config.signatures.step(stepType)
    }

    val seenOptions = mutable.HashSet.empty[QName]
    for (key <- attributes.keySet) {
      if ((stepType.getNamespaceURI == XProcConstants.ns_p && key == XProcConstants._message)
        || (stepType.getNamespaceURI != XProcConstants.ns_p && key == XProcConstants.p_message)) {
        // This is the message pseudo-option
        val avt = ValueParser.parseAvt(attributes(key))
        val context = new ExpressionContext(staticContext)
        options.put(key, new XProcVtExpression(context, avt.get, true))
      } else if (key.getNamespaceURI == "") {
        if (sig.options.contains(key)) {
          val opt = sig.option(key, staticContext.location.get)
          val context = new ExpressionContext(staticContext)
          seenOptions += key

          if (opt.declaredType.getOrElse("") == "map(*)") {
            options.put(key, new XProcXPathExpression(context, attributes(key)))
          } else {
            val avt = ValueParser.parseAvt(attributes(key))
            if (avt.isDefined) {
              options.put(key, new XProcVtExpression(context, avt.get, true))
            } else {
              throw new ModelException(ExceptionCode.BADAVT, List(key.toString, attributes(key)), staticContext.location)
            }
          }
        } else {
          throw XProcException.xsUndeclaredOption(stepType, key, staticContext.location)
        }
      } else {
        val avt = ValueParser.parseAvt(attributes(key))
        if (avt.isDefined) {
          val context = new ExpressionContext(staticContext)
          options.put(key, new XProcVtExpression(context, avt.get, true))
        } else {
          throw new ModelException(ExceptionCode.BADAVT, List(key.toString, attributes(key)), staticContext.location)
        }
      }
    }

    val okChildren = List(classOf[WithInput], classOf[WithOption])
    for (child <- relevantChildren) {
      if (!okChildren.contains(child.getClass)) {
        throw XProcException.xsElementNotAllowed(staticContext.location, child.nodeName)
      }
      valid = valid && child.validate()

      child match {
        case wo: WithOption =>
          if (seenOptions.contains(wo.optionName)) {
            throw XProcException.xsDupWithOptionName(wo.optionName, wo.staticContext.location)
          } else {
            seenOptions += wo.optionName
          }

          if (!sig.options.contains(wo.optionName)) {
            throw XProcException.xsUndeclaredOption(sig.stepType, wo.optionName, wo.staticContext.location)
          }
        case _ => Unit
      }
    }

    for (optName <- sig.options) {
      val opt = sig.option(optName, staticContext.location.get)
      if (opt.required) {
        if (!seenOptions.contains(optName)) {
          throw XProcException.xsMissingRequiredOption(optName, staticContext.location)
        }
      }

    }

    valid
  }

  override def makeInputPortsExplicit(): Boolean = {
    val sig = stepSignature(staticContext.stepType).get

    // Work out which input port is primary
    var primaryInput = Option.empty[String]
    for (port <- sig.inputPorts) {
      val siginput = sig.input(port, staticContext.location.get)
      if (siginput.primary) {
        primaryInput = Some(siginput.name)
      }
    }

    // If the port has been omitted, it refers to the primary port
    for (input <- inputs) {
      if (input.port.isEmpty) {
        if (primaryInput.isEmpty) {
          throw new ModelException(ExceptionCode.NOPRIMARYINPUTPORT, List(staticContext.stepType.toString), staticContext.location)
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
        throw new ModelException(ExceptionCode.DUPINPUTPORT, List(port), staticContext.location)
      }
      seenPorts += port
    }

    for (port <- sig.inputPorts) {
      val siginput = sig.input(port, staticContext.location.get)
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
        throw new ModelException(ExceptionCode.BADATOMICINPUTPORT, List(staticContext.stepType.toString, port), staticContext.location)
      }
    }

    true
  }

  override def makeOutputPortsExplicit(): Boolean = {
    val sig = stepSignature(staticContext.stepType).get

    for (port <- sig.outputPorts) {
      val sigoutput = sig.output(port, staticContext.location.get)
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
        throw new ModelException(ExceptionCode.BADATOMICINPUTPORT, List(staticContext.stepType.toString, port), staticContext.location)
      }
    }

    true
  }

  def stepImplementation(staticContext: StaticContext): StepExecutable = {
    stepImplementation(staticContext, None)
  }

  def stepImplementation(staticContext: StaticContext, implParams: Option[ImplParams]): StepExecutable = {
    val stepType = staticContext.stepType
    val location = staticContext.location

    val sig = stepSignature(stepType)
    if (sig.isEmpty) {
      config.stepImplementation(staticContext, implParams)
    } else {
      val implClass = sig.get.implementation
      if (implClass.isEmpty) {
        val declStep = parentDeclareStep()
        if (declStep.isDefined) {
          val decl = declStep.get.stepDeclaration(stepType)
          new StepRunner(config, decl.get, sig.get)
        } else {
          throw new ModelException(ExceptionCode.NOIMPL, stepType.toString, location)
        }
      } else {
        val klass = Class.forName(implClass.head).newInstance()
        klass match {
          case step: XmlStep =>
            new StepWrapper(step, sig.get)
          case _ =>
            throw new ModelException(ExceptionCode.IMPLNOTSTEP, stepType.toString, location)
        }
      }
    }
  }

  override def makeGraph(graph: Graph, parent: Node) {
    for (opt <- options.keySet) {
      val withOpt = new WithOption(config, this, opt, options(opt))
      addChild(withOpt)
    }

    var proxy: StepProxy = null
    val node = parent match {
      case start: ContainerStart =>
        val impl = stepImplementation(staticContext)
        proxy = new StepProxy(config, staticContext.stepType, impl, params, staticContext)

        for (port <- inputPorts) {
          val in = input(port)
          if (in.get.select.isDefined) {
            proxy.setDefaultSelect(port, in.get.selectExpression)
          }
        }

        start.addAtomic(proxy, name)
      case _ =>
        throw new ModelException(ExceptionCode.INTERNAL, "Atomic step parent isn't a container???", staticContext.location)
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
        throw new ModelException(ExceptionCode.NOBINDING, ref.toString, staticContext.location)
      }

      bind.get match {
        case optDecl: OptionDecl =>
          graph.addBindingEdge(optDecl._graphNode.get.asInstanceOf[Binding], graphNode)
        case varDecl: Variable =>
          graph.addBindingEdge(varDecl._graphNode.get.asInstanceOf[Binding], graphNode)
        case _ =>
          throw new ModelException(ExceptionCode.INTERNAL, s"Unexpected $ref binding: ${bind.get}", staticContext.location)
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

    val stepType = staticContext.stepType
    new xml.Elem(stepType.getPrefix, stepType.getLocalName,
      dump_attr.getOrElse(xml.Null), namespaceScope, false, nodes:_*)
  }

}
