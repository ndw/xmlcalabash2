package com.xmlcalabash.model.xml

import com.jafpl.config.Jafpl
import com.jafpl.graph.{Binding, Graph}
import com.jafpl.runtime.GraphRuntime
import com.xmlcalabash.config.{OptionSignature, PortSignature, StepSignature}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.messages.XdmValueItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.containers.{Container, DeclarationContainer}
import com.xmlcalabash.model.xml.datasource.{Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.{ExpressionContext, StaticContext, XMLCalabashRuntime, XProcMetadata, XProcXPathExpression}
import com.xmlcalabash.steps.internal.ContentTypeParams
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DeclareStep(override val config: XMLCalabashRuntime,
                  override val parent: Option[Artifact]) extends DeclarationContainer(config, parent, XProcConstants.p_declare_step) {
  private var _type: Option[QName] = None
  private var _psviRequired: Option[Boolean] = None
  private var _xpathVersion: Option[String] = None
  private var _excludeInlinePrefixes = Map.empty[String,String]
  private var _version: Option[String] = None
  private val _varOptList = ListBuffer.empty[Artifact]

  def declaredType: Option[QName] = _type
  def psviRequired: Boolean = _psviRequired.getOrElse(false)
  def xpathVersion: Option[String] = _xpathVersion
  def version: Option[String] = _version

  protected[model] def addVarOpt(art: Artifact): Unit = {
    art match {
      case variable: Variable => _varOptList += variable
      case option: OptionDecl => _varOptList += option
      case _ => throw new RuntimeException("Sent non opt to addVarOpt")
    }
  }

  protected[xmlcalabash] def _init(): Unit = {
    var valid = validate()

    if (!atomicStep) {
      valid = valid && makePortsExplicit()
      valid = valid && makePipesExplicit()
      valid = valid && makeBindingsExplicit()
    }

    if (!valid) {
      throw new RuntimeException("Invalid declare-step")
    }

    if (!atomicStep) {
      config.init()
    }
  }

  def pipelineGraph(): Graph = {
    val jafpl = Jafpl.newInstance()
    val graph = jafpl.newGraph()
    val pipeline = graph.addPipeline(name, manifold)

    for (port <- inputPorts) {
      graph.addInput(pipeline, port)
    }

    for (port <- outputPorts) {
      graph.addOutput(pipeline, port)
    }

    patchPipeline()

    _graphNode = Some(pipeline)
    graphChildren(graph, pipeline)
    graphEdges(graph, pipeline)

    for (step <- findInjectables()) {
      val stepName = step.name
      val stepType = step match {
        case atomic: AtomicStep =>
          atomic.stepType
        case container: Container =>
          container.stepType
        case _ =>
          XProcConstants.cx_unknown
      }

      for (inject <- step.inputInjectables) {
        var port: Option[String] = inject.declPort
        if (port.isEmpty) {
          for (iport <- step.inputPorts) {
            val input = step.input(iport)
            if (input.get.primary.get) {
              inject.declPort = iport
              port = Some(iport)
            }
          }
        }
        if (port.isEmpty) {
          logger.warn("Input injectable: no port specified and no primary output port")
        } else if (step.input(port.get).isEmpty) {
          logger.warn(s"Input injectable: no input port named ${port.get}")
        } else {
          logger.debug(s"Adding input injectable...${inject.id}")
          inject.name = stepName
          inject.stepType = stepType
          step._graphNode.get.addInputInjectable(inject)
        }
      }

      for (inject <- step.outputInjectables) {
        var port: Option[String] = inject.declPort
        if (port.isEmpty) {
          for (oport <- step.outputPorts) {
            val output = step.output(oport)
            if (output.get.primary.get) {
              inject.declPort = oport
              port = Some(oport)
            }
          }
        }
        if (port.isEmpty) {
          logger.warn("Input injectable: no port specified and no primary output port")
        } else if (step.output(port.get).isEmpty) {
          logger.warn(s"Input injectable: no output port named ${port.get}")
        } else {
          logger.debug(s"Adding output injectable...${inject.id}")
          inject.name = stepName
          inject.stepType = stepType
          step._graphNode.get.addOutputInjectable(inject)
        }
      }

      for (inject <- step.stepInjectables) {
        logger.debug(s"Adding step injectable...${inject.id}")
        inject.name = stepName
        inject.stepType = stepType
        step._graphNode.get.addStepInjectable(inject)
      }
    }

    graph
  }

  private def patchPipeline(): Unit = {
    for (input <- inputs) {
      if (input.defaultInputs.nonEmpty || input.contentTypes.nonEmpty) {
        patchDefaultInputs(input)
      }
    }
  }

  // If an input on a declare-step has default bindings, then we construct an identity step
  // and pipe both the external input binding and the default bindings through it. We arrange
  // for the (necessary) join step to be a priority join so that if documents appear on
  // the external binding, they will go through and none of the default bindings will.
  // If no documents are bound externally, all the defaults will flow through.
  //
  // In principle an explicit identity step shouldn't be necessary here, but it simplifies
  // the logic considerably and has very little performance impact.
  //
  private def patchDefaultInputs(input: Input): Unit = {
    // Insert the new step right before the input; this assures that the in-scope statics will be correct
    val params = new ContentTypeParams(input.port.get, input.contentTypes, input.sequence)
    val checker = new AtomicStep(config, Some(this), XProcConstants.cx_content_type_checker, Some(params))
    checker.staticContext.location = input.staticContext.location.get

    val idinput = new WithInput(config, checker, "source")
    if (input.select.isDefined) {
      idinput.select = input.select.get
    }
    checker.addChild(idinput)

    insertChildBefore(input, checker)
    checker.makePortsExplicit()

    val sinput = this.input(input.port.get)
    val cresult = checker.output("result")

    patchPipe(name, List(input.port.get), checker.name, "result")
    DrpRemap.remap(sinput.get, cresult.get)

    val pipe = new Pipe(config, idinput, name, input.port.get)
    pipe.gated = true
    idinput.addChild(pipe)

    for (source <- input.defaultInputs) {
      source match {
        case pipe: Pipe =>
          idinput.addChild(new Pipe(config, idinput, pipe))
        case doc: Document =>
          idinput.addChild(new Document(config, idinput, doc))
        case inline: Inline =>
          idinput.addChild(new Inline(config, idinput, inline))
        case empty: Empty =>
          idinput.addChild(new Empty(config, idinput, empty))
        case _ =>
          throw XProcException.xiBadPatchChild(source, staticContext.location)
      }
    }
  }

  override def atomicStep: Boolean = {
    for (child <- children) {
      child match {
        case _: Input => Unit
        case _: Output => Unit
        case _: OptionDecl => Unit
        case _ => return false
      }
    }
    true
  }

  override def makeInputBindingsExplicit(): Boolean = {
    true // Input bindings on a pipeline do not have to be bound
  }

  override protected[xml] def parse(node: XdmNode): Unit = {
    super.parse(node)
    _type = lexicalQName(attributes.get(XProcConstants._type))
  }

  override def validate(): Boolean = {
    var valid = super.validate()

    _psviRequired = lexicalBoolean(attributes.get(XProcConstants._psvi_required))
    _xpathVersion = attributes.get(XProcConstants._xpath_version)
    _excludeInlinePrefixes = lexicalPrefixes(attributes.get(XProcConstants._exclude_inline_prefixes))
    _version = attributes.get(XProcConstants._version)

    for (key <- List(XProcConstants._type, XProcConstants._psvi_required,
      XProcConstants._xpath_version, XProcConstants._exclude_inline_prefixes, XProcConstants._version)) {
      if (attributes.contains(key)) {
        attributes.remove(key)
      }
    }

    // N.B. The parser strips nested DeclareStep and Import elements out.
    for (child <- children) {
      valid = child.validate() && valid
    }

    if (valid) {
      // Clarify primary and sequence
      val inputs = children.filter(_.isInstanceOf[Input])
      val outputs = children.filter(_.isInstanceOf[Output])

      for (child <- inputs) {
        child match {
          case input: Input =>
            if (input.primary.isEmpty) {
              input.primary = inputs.size == 1
            }
        }
      }

      for (child <- outputs) {
        child match {
          case output: Output =>
            if (output.primary.isEmpty) {
              output.primary = outputs.size == 1
            }
        }
      }
    }

    valid
  }

  def signature: StepSignature = {
    val stepSig = new StepSignature(declaredType.get)
    if (config.config.atomicStepImplementation(declaredType.get).isDefined) {
      stepSig.implementation = config.config.atomicStepImplementation(declaredType.get).get
    }
    for (child <- children) {
      child match {
        case input: Input =>
          val portSig = new PortSignature(input.port.get, input.primary.get, input.sequence)
          stepSig.addInput(portSig, input.location.get)
        case output: Output =>
          val portSig = new PortSignature(output.port.get, output.primary.get, output.sequence)
          stepSig.addOutput(portSig, output.location.get)
        case option: OptionDecl =>
          val optSig = new OptionSignature(option.optionName, option.declaredType, option.required)
          if (option.allowedValues.isDefined) {
            optSig.tokenList = option.allowedValues.get
          }
          if (option.select.isDefined) {
            // Evaluate it; no reference to context is allowed.
            val context = new ExpressionContext(new StaticContext()) // FIXME: what about namespaces!?
            val expr = new XProcXPathExpression(context, option.select.get)
            val msg = config.expressionEvaluator.value(expr, List(), Map.empty[String,XdmValueItemMessage], None)
            optSig.defaultValue = msg.item.toString
          }
          stepSig.addOption(optSig, option.location.get)
        case _ =>
          Unit
      }
    }

    stepSig
  }

  override protected[xmlcalabash] def collectStatics(statics: Map[QName, Artifact]): Map[QName, Artifact] = {
    // It's the _varOptList that counts here because declared steps aren't in the child list

    // If the static is redefined several times, only the last one matters
    val staticHash = mutable.HashMap.empty[QName, Artifact]
    for (item <- _varOptList) {
      item match {
        case variable: Variable =>
          if (variable.static) {
            staticHash.put(variable.variableName, variable)
          }
        case option: OptionDecl =>
          if (option.static) {
            staticHash.put(option.optionName, option)
          }
        case _ =>
          throw new RuntimeException("This can't happen; static isn't variable or option")
      }
    }

    // If there are existing declarations at "lower levels", they're "last"
    for ((name,art) <- statics) {
      staticHash.put(name,art)
    }

    staticHash.toMap
  }

  protected[xmlcalabash] override def exposeStatics(): Boolean = {
    var valid = true

    // If the static is redefined several times, only the last one matters
    val staticHash = mutable.HashMap.empty[QName, Artifact]
    for (item <- _varOptList) {
      item match {
        case variable: Variable =>
          if (variable.static) {
            staticHash.put(variable.variableName, variable)
          }
        case option: OptionDecl =>
          if (option.static) {
            staticHash.put(option.optionName, option)
          }
        case _ =>
          throw new RuntimeException("This can't happen; static isn't variable or option")
      }
    }

    _varOptList.clear()
    for ((name,art) <- staticHash) {
      _varOptList += art
    }

    // Expose statics to the children, as appropriate
    for (child <- children) {
      child.exposeStatics()
    }

    // Also expose them to declared steps, which are no longer children
    for (art <- _declaredSteps) {
      valid = art.exposeStatics() && valid
    }
    for (art <- _declaredFunctions) {
      valid = art.exposeStatics() && valid
    }

    valid
  }

  protected[xmlcalabash] def evaluateStaticBindings(runtime: GraphRuntime): Unit = {
    val globalBindingsMap = mutable.HashMap.empty[String,XdmValueItemMessage]

    // If this is a user-defined step being called from a pipeline, there may already be
    // static variables in scope; make sure they get exposed.
    for (static <- _varOptList) {
      static match {
        case variable: Variable =>
          //runtime.setExternalStatic(variable.variableName.getClarkName, variable.staticValueMessage.get)
          globalBindingsMap.put(variable.variableName.getClarkName, variable.staticValueMessage.get)
          //config.globalContext.externalStatics.put(variable.variableName.getClarkName, variable.staticValueMessage.get)
        case option: OptionDecl =>
          //runtime.setExternalStatic(option.optionName.getClarkName, option.staticValueMessage.get)
          globalBindingsMap.put(option.optionName.getClarkName, option.staticValueMessage.get)
          //config.globalContext.externalStatics.put(option.optionName.getClarkName, option.staticValueMessage.get)
        case _ =>
          throw new RuntimeException("This can't happen; static isn't variable or option")
      }
    }

    val bindingsMap = mutable.HashMap.empty[String,XdmValueItemMessage] ++ globalBindingsMap
    for (child <- children) {
      child match {
        case variable: Variable =>
          if (variable.static) {
            val context = new ExpressionContext(new StaticContext()) // FIXME: what about namespaces!?
            val expr = new XProcXPathExpression(context, variable.select.get)
            val msg = config.expressionEvaluator.value(expr, List(), bindingsMap.toMap, None)
            variable.staticValueMessage = msg
            runtime.setStatic(variable._graphNode.get.asInstanceOf[Binding], msg)
            bindingsMap.put(variable.variableName.getClarkName, msg)
          }
        case option: OptionDecl =>
          if (option.static) {
            val msg = if (option.externalValue.isDefined) {
              new XdmValueItemMessage(option.externalValue.get, XProcMetadata.XML)
            } else {
              val context = new ExpressionContext(new StaticContext()) // FIXME: what about namespaces!?
              val expr = new XProcXPathExpression(context, option.select.get)
              config.expressionEvaluator.value(expr, List(), bindingsMap.toMap, None)
            }
            option.staticValueMessage = msg
            runtime.setStatic(option._graphNode.get.asInstanceOf[Binding], msg)
            bindingsMap.put(option.optionName.getClarkName, msg)
          }
        case _ => Unit
      }
    }
  }

  override def asXML: xml.Elem = {
    if (_excludeInlinePrefixes.nonEmpty) {
      var excludeIPs = ""
      for (prefix <- _excludeInlinePrefixes) {
        if (excludeIPs != "") {
          excludeIPs += " "
        }
        excludeIPs += prefix
      }
      dumpAttr("exclude-inline-prefixes", excludeIPs)
    }

    dumpAttr("version", _version)
    dumpAttr("psvi-required", _psviRequired)
    dumpAttr("xpath-version", _xpathVersion)

    if (_type.isDefined) {
      dumpAttr("type", _type.get.getClarkName)
    }

    dumpAttr("name", _name)
    dumpAttr("id", id.toString)

    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")
    for (child <- children) {
      nodes += child.asXML
      nodes += xml.Text("\n")
    }
    new xml.Elem("p", "declare-step", dump_attr.getOrElse(xml.Null), namespaceScope, false, nodes:_*)
  }

}
