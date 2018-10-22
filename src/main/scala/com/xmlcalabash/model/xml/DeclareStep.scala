package com.xmlcalabash.model.xml

import com.jafpl.config.Jafpl
import com.jafpl.graph.Graph
import com.xmlcalabash.config.{OptionSignature, PortSignature, StepSignature}
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.model.xml.containers.{Container, DeclarationContainer, WithDocument, WithProperties}
import com.xmlcalabash.model.xml.datasource.{Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.XMLCalabashRuntime
import com.xmlcalabash.steps.internal.ContentTypeParams
import net.sf.saxon.s9api.{QName, XdmNode}

import scala.collection.mutable.ListBuffer

class DeclareStep(override val config: XMLCalabashRuntime,
                  override val parent: Option[Artifact]) extends DeclarationContainer(config, parent, XProcConstants.p_declare_step) {
  private var _type: Option[QName] = None
  private var _psviRequired: Option[Boolean] = None
  private var _xpathVersion: Option[String] = None
  private var _excludeInlinePrefixes = Map.empty[String,String]
  private var _version: Option[String] = None
  //private val options = mutable.HashMap.empty[QName, XProcExpression]

  def declaredType: Option[QName] = _type
  def psviRequired: Boolean = _psviRequired.getOrElse(false)
  def xpathVersion: Option[String] = _xpathVersion
  def version: Option[String] = _version

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
    for (patch <- findPatchable()) {
      patch match {
        case art: WithProperties => patchWithProperties(art, processProperties = true)
        case art: WithDocument => patchWithProperties(art, processProperties = false)
        case _ => throw XProcException.xiBadPatch(patch, location)
      }
    }

    for (input <- inputs) {
      if (input.defaultInputs.nonEmpty || input.contentTypes.nonEmpty) {
        patchDefaultInputs(input)
      }
    }
  }

  private def patchWithProperties(patch: Artifact, processProperties: Boolean): Unit = {
    val extract = new AtomicStep(config, patch.parent, XProcConstants.cx_property_extract)
    val merge = new AtomicStep(config, patch.parent, XProcConstants.cx_property_merge)

    extract.location = patch.location.get
    merge.location = patch.location.get

    patch.parent.get.insertChildBefore(patch, extract)
    patch.parent.get.insertChildAfter(patch, merge)

    extract.makePortsExplicit()
    merge.makePortsExplicit()

    var input = extract.input("source").get
    var output = Option.empty[Output]
    var count = 0
    while (count < patch.children.length) {
      patch.children(count) match {
        case pinput: Input =>
          if (pinput.port.get == "source") {
            input.children.clear()
            for (pchild <- pinput.children) {
              pchild match {
                case pipe: Pipe =>
                  input.addChild(new Pipe(config, input, pipe))
                case doc: Document =>
                  input.addChild(new Document(config, input, doc))
                case inline: Inline =>
                  input.addChild(new Inline(config, input, inline))
                case empty: Empty =>
                  input.addChild(new Empty(config, input, empty))
                case _ =>
                  throw XProcException.xiBadPatchChild(pchild, location)
              }
            }
            pinput.children.clear()
          }
        case out: Output =>
          output = Some(out)
        case _ => Unit
      }
      count += 1
    }

    patchPipe(patch.name, List("result","#result"), merge.name, "result")
    input = patch.input("source").get

    if (processProperties) {
      var pipe = new Pipe(config, input, extract.name, "properties")
      input.addChild(pipe)

      input = new Input(config, merge, "source", primary=true, sequence=false)
      pipe = new Pipe(config, input, extract.name, "result")
      input.addChild(pipe)
      merge.addChild(input)

      input = new Input(config, merge, "properties", primary=true, sequence=false)
      pipe = new Pipe(config, input, patch.name, output.get.port.get)
      input.addChild(pipe)
      merge.addChild(input)
    } else {
      var pipe = new Pipe(config, input, extract.name, "result")
      input.addChild(pipe)

      input = new Input(config, merge, "source", primary=true, sequence=false)
      pipe = new Pipe(config, input, extract.name, "properties")
      input.addChild(pipe)
      merge.addChild(input)

      input = new Input(config, merge, "result", primary=true, sequence=false)
      pipe = new Pipe(config, input, patch.name, output.get.port.get)
      input.addChild(pipe)
      merge.addChild(input)
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
    var firstChild: Option[Artifact] = None
    for (child <- children) {
      child match {
        case input: Input => Unit
        case output: Output => Unit
        case variable: Variable => Unit
        case art: Artifact =>
          if (firstChild.isEmpty) {
            firstChild = Some(art)
          }
      }
    }

    val params = new ContentTypeParams(input.port.get, input.contentTypes, input.sequence)
    val checker = new AtomicStep(config, Some(this), XProcConstants.cx_content_type_checker, Some(params))
    checker.location = input.location.get

    val idinput = new WithInput(config, checker, "source")
    if (input.select.isDefined) {
      idinput.select = input.select.get
    }
    checker.addChild(idinput)

    insertChildBefore(firstChild.get, checker)
    checker.makePortsExplicit()

    val sinput = this.input(input.port.get)
    val cresult = checker.output("result")

    patchPipe(name, List(input.port.get), checker.name, "result")
    DrpRemap.remap(sinput.get, cresult.get)

    val pipe = new Pipe(config, idinput, name, input.port.get)
    pipe.priority = true
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
          throw XProcException.xiBadPatchChild(source, location)
      }
    }
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

    /*
    for (key <- attributes.keySet) {
      if (key.getNamespaceURI == "") {
        throw new ModelException(ExceptionCode.BADCONTAINERATTR, key.getLocalName, location)
      } else {
        options.put(key, lexicalAvt(key.toString, attributes(key)))
      }
    }
    */

    // N.B. The parser strips nested DeclareStep and Import elements out.
    for (child <- children) {
      valid = child.validate() && valid
    }

    /*
    val groupOne = List(classOf[Input], classOf[Output], classOf[OptionDecl],
      classOf[Serialization], classOf[Documentation], classOf[PipeInfo], classOf[Variable])
    val groupTwo = List(classOf[DeclareStep], classOf[Import], classOf[Documentation], classOf[PipeInfo])
    val groupThree = subpiplineClasses ++ List(classOf[Documentation], classOf[PipeInfo])

    valid = true
    var index = 0
    while (index < children.length && groupOne.contains(children(index).getClass)) {
      valid = valid && children(index).validate()
      index += 1
    }
    while (index < children.length && groupTwo.contains(children(index).getClass)) {
      valid = valid && children(index).validate()
      index += 1
    }
    while (index < children.length && groupThree.contains(children(index).getClass)) {
      valid = valid && children(index).validate()
      index += 1
    }
    if (index < children.length) {
      throw XProcException.xsElementNotAllowed(location, children(index).nodeName)
    }
    */

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
          stepSig.addOption(optSig, option.location.get)
        case _ =>
          println(child)
      }
    }

    stepSig
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
