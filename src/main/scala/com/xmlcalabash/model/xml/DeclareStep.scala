package com.xmlcalabash.model.xml

import com.jafpl.steps.{Manifold, PortCardinality, PortSpecification}
import com.xmlcalabash.config.{OptionSignature, PortSignature, StepSignature, XMLCalabashConfig}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.runtime.params.ContentTypeCheckerParams
import com.xmlcalabash.util.xc.ElaboratedPipeline
import com.xmlcalabash.util.{S9Api, TypeUtils}
import net.sf.saxon.s9api.{ItemType, QName, SaxonApiException, XdmAtomicValue, XdmNode, XdmNodeKind}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DeclareStep(override val config: XMLCalabashConfig) extends Container(config) with DeclContainer {
  private var _type = Option.empty[QName]
  private var _psvi_required = Option.empty[Boolean]
  private var _xpath_version = Option.empty[Double]
  private var _exclude_inline_prefixes = Option.empty[String]
  private var _version = Option.empty[Double]
  private var _visibility = Option.empty[String]
  private var _inScopeDeclarations = ListBuffer.empty[StepSignature]
  private var _signature = Option.empty[StepSignature]
  private var _inputs = mutable.HashMap.empty[String, DeclareInput]
  private var _bindings = mutable.HashMap.empty[QName, DeclareOption]
  private var _excludeUriBindings = Set.empty[String]

  def stepType: Option[QName] = _type
  def psvi_required: Option[Boolean] = _psvi_required
  def xpath_version: Option[Double] = _xpath_version
  def exclude_inline_prefixes: Option[String] = _exclude_inline_prefixes
  def exclude_uri_bindings: Set[String] = _excludeUriBindings
  def version: Double = _version.getOrElse(3.0)
  def visiblity: String = _visibility.getOrElse("public")
  def inScopeDeclarations: List[StepSignature] = _inScopeDeclarations.toList
  def signature: StepSignature = _signature.get

  def inputPorts: List[String] = _inputs.keySet.toList
  def outputPorts: List[String] = _outputs.keySet.toList

  def input(port: String): DeclareInput = _inputs(port)
  def output(port: String): DeclareOutput = _outputs(port)

  def inputs: List[DeclareInput] = _inputs.values.toList
  def outputs: List[DeclareOutput] = _outputs.values.toList

  def bindings: Map[QName,DeclareOption] = _bindings.toMap

  override def addDeclaration(decl: StepSignature): Unit = {
    _inScopeDeclarations += decl
  }

  override def parse(node: XdmNode): Unit = {
    super.parse(node)

    val aname = attr(XProcConstants._name)
    if (aname.isDefined) {
      try {
        val typeUtils = new TypeUtils(config)
        val ncname = typeUtils.castAtomicAs(XdmAtomicValue.makeAtomicValue(aname.get), ItemType.NCNAME, staticContext)
        _name = Some(ncname.getStringValue)
      } catch {
        case sae: SaxonApiException =>
          throw XProcException.xsBadTypeValue(aname.get, "NCName", location)
      }
    }

    _type = staticContext.parseQName(attr(XProcConstants._type))
    _psvi_required = staticContext.parseBoolean(attr(XProcConstants._psvi_required))
    if (attributes.contains(XProcConstants._xpath_version)) {
      val vstr = attr(XProcConstants._xpath_version).get
      _xpath_version = Some(vstr.toDouble)
    }
    if (attributes.contains(XProcConstants._version)) {
      val vstr = attr(XProcConstants._version).get
      try {
        _version = Some(vstr.toDouble)
      } catch {
        case ex: NumberFormatException =>
          throw XProcException.xsBadVersion(vstr, location)
      }
      if (_version.get != 3.0) {
        throw XProcException.xsInvalidVersion(_version.get, location)
      }
    }
    if (_version.isEmpty) {
      if (node.getParent.getNodeKind == XdmNodeKind.DOCUMENT) {
        throw XProcException.xsVersionRequired(location)
      }
    }

    _exclude_inline_prefixes = attr(XProcConstants._exclude_inline_prefixes)
    if (_exclude_inline_prefixes.isDefined) {
      val prefixes = _exclude_inline_prefixes.get.split("\\s+").toList
      _excludeUriBindings = S9Api.urisForPrefixes(node, prefixes)
    }

    _visibility = attr(XProcConstants._visibility)
    if (_visibility.isDefined) {
      if (_visibility.get != "public" && _visibility.get != "private") {
        throw XProcException.xdBadVisibility(_visibility.get, location)
      }
    }

    if (attributes.nonEmpty) {
      val badattr = attributes.keySet.head
      throw XProcException.xsBadAttribute(badattr, location)
    }
  }

  private def parseDeclarationSignature(env: Environment): Unit = {
    // If there's only one input and it doesn't have a declared primary status, make it primary
    var count = 0
    var pinput = Option.empty[DeclareInput]
    for (child <- allChildren) {
      child match {
        case input: DeclareInput =>
          count += 1
          if (pinput.isEmpty) {
            pinput = Some(input)
          }
        case _ => Unit
      }
    }
    if (count == 1 && pinput.get._primary.isEmpty) {
      pinput.get.primary = true
    }

    // If there's only one output and it doesn't have a declared primary status, make it primary
    count = 0
    var poutput = Option.empty[DeclareOutput]
    for (child <- allChildren) {
      child match {
        case output: DeclareOutput =>
          count += 1
          if (poutput.isEmpty) {
            poutput = Some(output)
          }
        case _ => Unit
      }
    }
    if (count == 1 && poutput.get._primary.isEmpty) {
      poutput.get.primary = true
    }

    var lastInput = Option.empty[DeclareInput]
    var lastOutput = Option.empty[DeclareOutput]
    var primaryInput = Option.empty[DeclareInput]
    var primaryOutput = Option.empty[DeclareOutput]

    for (child <- allChildren) {
      child match {
        case input: DeclareInput =>
          if (_inputs.contains(input.port) || _outputs.contains(input.port)) {
            throw XProcException.xsDupPortName(input.port, location)
          }
          _inputs.put(input.port, input)

          lastInput = Some(input)
          if (input.primary) {
            if (primaryInput.isDefined) {
              throw XProcException.xsDupPrimaryPort(input.port, primaryInput.get.port, staticContext.location)
            }
            primaryInput = Some(input)
          }
        case output: DeclareOutput =>
          if (_outputs.contains(output.port) || _inputs.contains(output.port)) {
            throw XProcException.xsDupPortName(output.port, location)
          }
          _outputs.put(output.port, output)

          lastOutput = Some(output)
          if (output.primary) {
            if (primaryOutput.isDefined) {
              throw XProcException.xsDupPrimaryPort(output.port, primaryOutput.get.port, staticContext.location)
            }
            primaryOutput = Some(output)
          }
        case option: DeclareOption =>
          if (_bindings.contains(option.name)) {
            throw new RuntimeException("duplicate option name")
          }
          _bindings.put(option.name, option)

        case _ => Unit
      }
    }

    if (_inputs.size == 1 && lastInput.get._primary.isEmpty) {
      lastInput.get.primary = true
    }

    if (_outputs.size == 1 && lastOutput.get._primary.isEmpty) {
      lastOutput.get.primary = true
    }

    val stepSig = new StepSignature(stepType)
    if (stepType.isDefined) {
      if (config.atomicStepImplementation(stepType.get).isDefined) {
        stepSig.implementation = config.atomicStepImplementation(stepType.get).get
      } else {
        stepSig.declaration = this
      }
    }

    for (child <- allChildren) {
      child match {
        case input: DeclareInput =>
          val portSig = new PortSignature(input.port, input.primary, input.sequence)
          stepSig.addInput(portSig, input.location.get)
        case output: DeclareOutput =>
          val portSig = new PortSignature(output.port, output.primary, output.sequence)
          stepSig.addOutput(portSig, output.location.get)
        case option: DeclareOption =>
          val optSig = new OptionSignature(option.name, option.declaredType, option.required)
          if (option.allowedValues.isDefined) {
            optSig.tokenList = option.allowedValues.get
          }
          if (option.select.isDefined) {
            optSig.defaultSelect = option.select.get
          }
          stepSig.addOption(optSig, option.location.get)
        case _ =>
          Unit
      }
    }

    _signature = Some(stepSig)
    env.addStep(this)

    if (stepType.isDefined && declaration(stepType.get).isDefined) {
      throw new RuntimeException(s"Attempt to redefine ${stepType.get}")
    }
    _inScopeDeclarations += stepSig

    val buf = ListBuffer.empty[DeclareStep]
    for (decl <- children[DeclareStep]) {
      //decl.parseDeclarationSignature()
      if (decl.stepType.isDefined && declaration(decl.stepType.get).isDefined) {
        throw new RuntimeException(s"Attempt to redefine ${stepType.get}")
      }
      _inScopeDeclarations += decl.signature
      buf += decl
    }
    for (decl <- buf) {
      removeChild(decl)
    }
  }

  override protected[model] def makeStructureExplicit(environment: Environment): Unit = {
    if (_signature.isEmpty) {
      parseDeclarationSignature(environment)
    }

    for (child <- allChildren) {
      child match {
        case atomic: AtomicStep =>
          atomic.makeStructureExplicit(environment)
        case decl: DeclareStep =>
          val newenvironment = environment.declareStep()
          decl.makeStructureExplicit(newenvironment)
        case compound: Container =>
          compound.makeStructureExplicit(environment)
        case variable: Variable =>
          variable.makeStructureExplicit(environment)
          environment.addVariable(variable)
        case option: DeclareOption =>
          option.makeStructureExplicit(environment)
          environment.addVariable(option)
        case _ =>
          child.makeStructureExplicit(environment)
      }
    }
  }

  override protected[model] def validateStructure(): Unit = {
    for (child <- allChildren) {
      child.validateStructure()
    }
  }

  protected[model] def makeBindingsExplicit(env: Environment): Unit = {
    makeBindingsExplicit(env, None)
  }

  def runtime(): XMLCalabashRuntime = {
    val runtime = new XMLCalabashRuntime(this)
    val pipeline = runtime.graph.addPipeline(stepName, manifold)

    for (port <- inputPorts) {
      runtime.graph.addInput(pipeline, port)
    }

    for (port <- outputPorts) {
      runtime.graph.addOutput(pipeline, port)
    }

    _graphNode = Some(pipeline)

    graphNodes(runtime, pipeline)

    for (child <- allChildren) {
      child match {
        case doc: Documentation => Unit
        case pipe: PipeInfo => Unit
        case _ =>
          child.graphEdges(runtime, pipeline)
      }
    }

    runtime.init()
    runtime
  }

  private def manifold: Manifold = {
    val inputMap = mutable.HashMap.empty[String,PortCardinality]
    for (input <- inputs) {
      if (input.sequence) {
        inputMap.put(input.port, PortCardinality.ZERO_OR_MORE)
      } else {
        inputMap.put(input.port, PortCardinality.EXACTLY_ONE)
      }
    }
    val outputMap = mutable.HashMap.empty[String,PortCardinality]
    for (output <- outputs) {
      if (output.sequence) {
        outputMap.put(output.port, PortCardinality.ZERO_OR_MORE)
      } else {
        outputMap.put(output.port, PortCardinality.EXACTLY_ONE)
      }
    }
    new Manifold(new PortSpecification(inputMap.toMap), new PortSpecification(outputMap.toMap))
  }

  override def declaration(stepType: QName): Option[StepSignature] = {
    var found = Option.empty[StepSignature]

    for (sig <- _inScopeDeclarations) {
      if (sig.stepType.isDefined && sig.stepType.get == stepType) {
        found = Some(sig)
      }
    }

    if (found.isEmpty && parent.isDefined) {
      found = parent.get.declaration(stepType)
    }

    found
  }

  def addContentTypeCheckers(): Unit = {
    // I don't know if it's safe to manipulate a list while I'm traversing it or not.
    // So don't.
    val inputs = ListBuffer.empty[DeclareInput] ++ children[DeclareInput]

    for (input <- inputs) {
      val params = new ContentTypeCheckerParams(input.port, input.content_types, staticContext, input.select, input.sequence)
      val atomic = new AtomicStep(config, Some(params))
      atomic.stepType = XProcConstants.cx_content_type_checker
      addChild(atomic, firstStepChild)

      // Save the pipes
      val pipes = ListBuffer.empty[Pipe]

      val winput = new WithInput(config)
      winput.port = "source"
      atomic.addChild(winput)

      val pipe = new Pipe(config)
      pipe.step = stepName
      pipe.port = input.port
      pipe.link = input
      pipes += pipe

      for (child <- input.allChildren) {
        child match {
          case pipe: Pipe =>
            pipes += pipe
            val step = pipe.link.get.parent.get
            step match {
              case atomic: AtomicStep =>
                input.defaultInputs += atomic
            }
          case doc: Document =>
            Unit
          case inline: Inline =>
            Unit
          case _ =>
            throw new RuntimeException("children of input not pipe?")
        }
      }

      input.removeChildren()
      val woutput = new WithOutput(config)
      woutput.port = "result"
      atomic.addChild(woutput)
      replumb(input, woutput)

      // Put in the pipes after we replumb, so we don't replumb ourselves!
      for (pipe <- pipes) {
        winput.addChild(pipe)
      }
    }
  }

  def xdump(): XdmNode = {
    val xml = new ElaboratedPipeline(config)
    xdump(xml)
    val doc = xml.endPipeline()
    doc.get
  }

  override def xdump(xml: ElaboratedPipeline): Unit = {
    xml.startPipeline(tumble_id, stepName, stepType, version, psvi_required, xpath_version, exclude_inline_prefixes, _visibility)
    for (child <- rawChildren) {
      child.xdump(xml)
    }
  }

  override def toString: String = {
    if (stepType.isDefined) {
      s"p:declare-step ${stepType.get} $stepName $uid"
    } else {
      s"p:declare-step {anon} $stepName $uid"
    }
  }

}
