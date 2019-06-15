package com.xmlcalabash.model.xml

import com.xmlcalabash.config.Signatures
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException, XProcException}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, UniqueId, XProcConstants}
import com.xmlcalabash.model.xml.containers.{Catch, Choose, DeclarationContainer, Finally, ForEach, Group, Otherwise, Try, When, WithDocument, WithProperties}
import com.xmlcalabash.model.xml.datasource.{Document, Empty, Inline, Pipe}
import com.xmlcalabash.runtime.injection.{XProcPortInjectable, XProcStepInjectable}
import com.xmlcalabash.runtime.{ExpressionContext, NodeLocation, StaticContext, XMLCalabashRuntime, XProcVtExpression, XProcXPathExpression}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer

class Parser(val config: XMLCalabashRuntime) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var exception: Option[Throwable] = None
  private val injectables = ListBuffer.empty[Injectable]

  def parsePipeline(node: XdmNode): DeclareStep = {
    for (injectable <- injectables) {
      injectable.findSteps(node)
    }

    val art = parse(node)
    if (exception.nonEmpty) {
      throw exception.get
    }

    if (art.isEmpty) {
      exception = Some(new ModelException(ExceptionCode.INTERNAL, "This can't happen", node))
      config.errorListener.error(exception.get)
      throw exception.get
    }

    if (! art.get.isInstanceOf[DeclareStep]) {
      exception = Some(new ModelException(ExceptionCode.BADPIPELINEROOT, node.toString, node))
      config.errorListener.error(exception.get)
      throw exception.get
    }

    val step = art.get.asInstanceOf[DeclareStep]

    for (injectable <- injectables) {
      if (!injectable.matched) {
        if (injectable.nodes.nonEmpty) {
          logger.warn(s"XPath expression did not match any steps: ${injectable.stepXPath.expr}")
        }
      }
    }

    step
  }

  private def parse(node: XdmNode): Option[Artifact] = {
    val root = parse(None, node)
    if (exception.isEmpty) {
      var valid = root.get.validate()
      valid = valid && root.get.makePortsExplicit()
      valid = valid && root.get.makePipesExplicit()
      valid = valid && root.get.makeBindingsExplicit()
      if (!valid) {
        config.errorListener.error(new ModelException(ExceptionCode.INVALIDPIPELINE, List(), node))
      }
    }
    root
  }

  private def parse(parent: Option[Artifact], node: XdmNode): Option[Artifact] = {
    node.getNodeKind match {
      case XdmNodeKind.DOCUMENT =>
        var art: Option[Artifact] = None
        val iter = node.axisIterator(Axis.CHILD)
        while (iter.hasNext) {
          val child = iter.next().asInstanceOf[XdmNode]
          if (child.getNodeKind == XdmNodeKind.ELEMENT) {
            art = parse(None, child)
          }
        }
        art

      case XdmNodeKind.ELEMENT =>
        try {
          val art: Option[Artifact] = node.getNodeName match {
            case XProcConstants.p_declare_step => Some(parseDeclareStep(parent, node))
            case XProcConstants.p_pipeline => Some(parsePipeline(parent, node))
            case XProcConstants.p_serialization => Some(parseSerialization(parent, node))
            case XProcConstants.p_output => Some(parseOutput(parent, node))
            case XProcConstants.p_input => Some(parseInput(parent, node))
            case XProcConstants.p_with_input => Some(parseWithInput(parent, node))
            case XProcConstants.p_option => Some(parseOption(parent, node))
            case XProcConstants.p_variable => Some(parseVariable(parent, node))
            case XProcConstants.p_inline => Some(parseInline(parent, node))
            case XProcConstants.p_empty => Some(parseEmpty(parent, node))
            case XProcConstants.p_pipe => Some(parsePipe(parent, node))
            case XProcConstants.p_document => Some(parseDocument(parent, node))
            case XProcConstants.p_with_option => Some(parseWithOption(parent, node))
            case XProcConstants.p_group => Some(parseGroup(parent, node))
            case XProcConstants.p_choose => Some(parseChoose(parent, node))
            case XProcConstants.p_when => Some(parseWhen(parent, node))
            case XProcConstants.p_otherwise => Some(parseOtherwise(parent, node))
            case XProcConstants.p_if => Some(parseIf(parent, node))
            case XProcConstants.p_try => Some(parseTry(parent, node))
            case XProcConstants.p_catch => Some(parseCatch(parent, node))
            case XProcConstants.p_finally => Some(parseFinally(parent, node))
            case XProcConstants.p_for_each => Some(parseForEach(parent, node))
            case XProcConstants.p_documentation => Some(parseDocumentation(parent, node))
            case XProcConstants.p_pipeinfo => Some(parsePipeInfo(parent, node))
            case XProcConstants.p_library => Some(parseLibrary(parent, node))
            case XProcConstants.p_function => Some(parseFunction(parent, node))
            case _ =>
              if (parent.isDefined) {
                if (knownStep(parent.get, node.getNodeName)) {
                  Some(parseAtomicStep(parent, node))
                } else {
                  parent.get match {
                    case input: WithInput =>
                      Some(parseInline(parent, node))
                    case input: Input =>
                      Some(parseInline(parent, node))
                    case variable: Variable =>
                      Some(parseInline(parent, node))
                    case _ =>
                      throw XProcException.xsElementNotAllowed(Some(new NodeLocation(node)), node.getNodeName)
                  }
                }
              } else {
                throw new ModelException(ExceptionCode.NOTASTEP, node.getNodeName.toString, node)
              }
          }

          if (art.isDefined) {
            art.get match {
              case step: PipelineStep =>
                for (injectable <- injectables) {
                  if (injectable.matches(node)) {
                    for (ref <- injectable.findVariableRefs) {
                      step.addVariableRef(ref)
                    }
                    injectable.itype match {
                      case XProcConstants.p_input =>
                        val inj = new XProcPortInjectable(injectable)
                        step.addInputInjectable(inj)
                      case XProcConstants.p_output =>
                        val inj = new XProcPortInjectable(injectable)
                        step.addOutputInjectable(inj)
                      case XProcConstants.p_start =>
                        val inj = new XProcStepInjectable(injectable)
                        step.addStepInjectable(inj)
                      case XProcConstants.p_end =>
                        val inj = new XProcStepInjectable(injectable)
                        step.addStepInjectable(inj)
                    }
                  }
                }
              case _ => Unit
            }
          }

          art
        } catch {
          case t: Throwable =>
            if (exception.isEmpty) {
              exception = Some(t)
            }
            config.errorListener.error(t)
            None
        }
      case XdmNodeKind.TEXT =>
        if (node.getStringValue.trim != "") {
          throw XProcException.xsTextNotAllowed(Some(new NodeLocation(node)), node.getStringValue.trim)
        }
        None
      case _ => None
    }
  }

  private def knownStep(parent: Artifact, stepType: QName): Boolean = {
    val decl = parent.stepDeclaration(stepType)
    if (decl.isDefined) {
      true
    } else {
      config.signatures.stepTypes.contains(stepType)
    }
  }

  private def parseChildren(parent: Artifact, node: XdmNode): Unit = {
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      val art = parse(Some(parent), child)
      if (art.isDefined) {
        parent.addChild(art.get)
      }
    }
  }

  // ==========================================================================================

  private def parseDeclareStep(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new DeclareStep(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parsePipeline(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new DeclareStep(config, parent)
    val input = new Input(config, Some(art))
    input.attributes.put(XProcConstants._port, "source")
    input.attributes.put(XProcConstants._primary, "true")
    art.children += input

    val output = new Output(config, Some(art))
    output.attributes.put(XProcConstants._port, "result")
    output.attributes.put(XProcConstants._primary, "true")
    art.children += output

    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseAtomicStep(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new AtomicStep(config, parent, node.getNodeName)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseSerialization(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Serialization(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseOutput(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Output(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseInput(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Input(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art.manageDefaultInputs()
    art
  }

  private def parseWithInput(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new WithInput(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseInline(parent: Option[Artifact], node: XdmNode): Artifact = {
    var exclPrefixes: String = null
    val nodes = ListBuffer.empty[XdmNode]
    if (node.getNodeName == XProcConstants.p_inline) {
      exclPrefixes = node.getAttributeValue(XProcConstants._exclude_inline_prefixes)
      val iter = node.axisIterator(Axis.CHILD)
      while (iter.hasNext) {
        nodes += iter.next()
      }
    } else {
      exclPrefixes = node.getParent.getAttributeValue(XProcConstants._exclude_inline_prefixes)
      val iter = node.getParent.axisIterator(Axis.CHILD)
      while (iter.hasNext) {
        nodes += iter.next()
      }
    }

    // Find exclude-inline-prefixes
    var contextNode = node
    while (contextNode != null && contextNode.getNodeKind == XdmNodeKind.ELEMENT && exclPrefixes == null) {
      contextNode = contextNode.getParent
      if (contextNode != null && contextNode.getNodeKind == XdmNodeKind.ELEMENT) {
        if (contextNode.getNodeName.getNamespaceURI == XProcConstants.ns_p) {
          exclPrefixes = contextNode.getAttributeValue(XProcConstants._exclude_inline_prefixes)
        } else {
          exclPrefixes = contextNode.getAttributeValue(XProcConstants.p_exclude_inline_prefixes)
        }
      }
    }

    var excludeUriBindings = Set.empty[String]
    if (exclPrefixes != null) {
      val prefixes = exclPrefixes.split("\\s+").toList
      excludeUriBindings = S9Api.urisForPrefixes(contextNode, prefixes)
    }

    val art = new Inline(config, parent, node.getNodeName != XProcConstants.p_inline, excludeUriBindings, nodes.toList)
    art.parse(node)
    art
  }

  private def parseEmpty(parent: Option[Artifact], node: XdmNode): Artifact = {
    // FIXME: check that p:empty is empty!
    val art = new Empty(config, parent)
    art.parse(node)
    art
  }

  private def parseDocument(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Document(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parsePipe(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Pipe(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseOption(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new OptionDecl(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseVariable(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Variable(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseWithOption(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new WithOption(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseGroup(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Group(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseChoose(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Choose(config, parent)
    art.parse(node)
    parseChildren(art, node)

    var hasOtherwise = false
    for (child <- art.children) {
      child match {
        case other: Otherwise =>
          hasOtherwise = true
        case _ => Unit
      }
    }

    if (!hasOtherwise) {
      val builder = new SaxonTreeBuilder(config)
      builder.startDocument(node.getBaseURI)
      builder.addStartElement(XProcConstants.p_otherwise)
      builder.startContent()
      builder.addPI("_xmlcalabash", nodeLocationPItext(node))
      builder.addStartElement(XProcConstants.p_error)
      builder.addNamespace("err", XProcConstants.ns_err)
      builder.addAttribute(XProcConstants._code, "err:XD0004")
      builder.startContent()
      builder.addEndElement()
      builder.addEndElement()
      builder.endDocument()
      val synthetic = builder.result
      val other = parse(Some(art), synthetic).get
      art.addChild(other)
    }

    art
  }

  private def parseWhen(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new When(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseOtherwise(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Otherwise(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseTry(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Try(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseCatch(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Catch(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseFinally(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Finally(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseIf(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Choose(config, parent)
    val when = new When(config, Some(art))

    when.parse(node)
    parseChildren(when, node)

    art.addChild(when)
    art
  }

  private def parseForEach(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new ForEach(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseDocumentation(parent: Option[Artifact], node: XdmNode): Artifact = {
    val nodes = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      nodes += child
    }
    val art = new Documentation(config, parent, nodes.toList)
    art.parse(node)
    art
  }

  private def parsePipeInfo(parent: Option[Artifact], node: XdmNode): Artifact = {
    val nodes = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val child = iter.next().asInstanceOf[XdmNode]
      nodes += child
    }
    val art = new PipeInfo(config, parent,nodes.toList)
    art.parse(node)
    art
  }

  private def parseLibrary(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Library(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseFunction(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Function(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  // =====================================================================================

  protected[xmlcalabash] def signatures(doc: XdmNode): Signatures = {
    val xdmLibrary = S9Api.documentElement(doc).get
    if (xdmLibrary.getNodeName != XProcConstants.p_library) {
      throw new RuntimeException("Signatures can only be loaded from a p:library")
    }
    val library = parse(xdmLibrary).get.asInstanceOf[Library]

    val signatures = new Signatures()
    for (child <- library.declarations) {
      child match {
        case fdef: Function =>
          signatures.addFunction(fdef.functionName, fdef.functionClass)
        case sdef: DeclareStep =>
          signatures.addStep(sdef.signature)
        case _ =>
          Unit
      }
    }

    signatures
  }
  // =====================================================================================

  private def nodeLocationPItext(node: XdmNode): String = {
    var str = "uri=\"" + node.getBaseURI + "\""
    if (node.getLineNumber > 0) {
      str += " line=\"" + node.getLineNumber + "\""
    }
    if (node.getColumnNumber > 0) {
      str += " column=\"" + node.getColumnNumber + "\""
    }
    str
  }

  def parseInjectables(doc: XdmNode): Unit = {
    val root = S9Api.documentElement(doc)
    if (root.get.getNodeName != XProcConstants.p_injectable) {
      logger.warn(s"Ignoring ${root.get.getNodeName} in injectable")
      return
    }

    val iter = root.get.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val node = iter.next().asInstanceOf[XdmNode]
      node.getNodeKind match {
        case XdmNodeKind.ELEMENT =>
          parseInjectable(node)
        case XdmNodeKind.TEXT =>
          if (node.getStringValue.trim != "") {
            logger.warn("Ignoring non-empty text node in injectables")
          }
        case _ => Unit
      }
    }
  }

  private def parseInjectable(node: XdmNode): Unit = {
    if (!List(XProcConstants.p_input, XProcConstants.p_output, XProcConstants.p_start, XProcConstants.p_end).contains(node.getNodeName)) {
      logger.warn(s"Ignoring ${node.getNodeName} in injectable")
      return
    }

    val step = Option(node.getAttributeValue(XProcConstants._step))
    val message = Option(node.getAttributeValue(XProcConstants._message))
    val uriStr = Option(node.getAttributeValue(XProcConstants._base_uri))
    val condition = Option(node.getAttributeValue(XProcConstants._condition))
    var messageNodes = Option.empty[ListBuffer[XdmNode]]
    val baseURI = if (uriStr.isDefined) {
      val uri = node.getBaseURI.resolve(uriStr.get)
      Some(uri)
    } else {
      None
    }

    val scontext = new StaticContext()
    scontext.baseURI = node.getBaseURI
    scontext.inScopeNS = S9Api.inScopeNamespaces(node)
    scontext.location = new NodeLocation(node)

    val context = new ExpressionContext(scontext)
    val stepExpr = new XProcXPathExpression(context, step.getOrElse("/p:declare-step/p:*"))
    val conditionExpr = new XProcXPathExpression(context, condition.getOrElse("true()"))

    if (message.isEmpty) {
      val nodes = ListBuffer.empty[XdmNode]
      val iter = node.axisIterator(Axis.CHILD)
      var onlySeenWhitespace = true
      while (iter.hasNext) {
        val node = iter.next().asInstanceOf[XdmNode]
        if (onlySeenWhitespace && (node.getNodeKind == XdmNodeKind.TEXT)) {
          if (node.getStringValue.trim == "") {
            // drop leading whitespace on the floor
          } else {
            onlySeenWhitespace = false
            nodes += node
          }
        } else {
          onlySeenWhitespace = false
          nodes += node
        }
      }

      onlySeenWhitespace = true
      while (onlySeenWhitespace) {
        onlySeenWhitespace = false
        if (nodes.nonEmpty && (nodes.last.getNodeKind == XdmNodeKind.TEXT)
          && (nodes.last.getStringValue.trim == "")) {
          onlySeenWhitespace = true
          nodes.remove(nodes.length - 1)
        }
      }

      messageNodes= Some(nodes)
    }

    val xmlId = Option(node.getAttributeValue(XProcConstants.xml_id))
    val id = if (xmlId.isDefined) {
      xmlId.get
    } else {
      "inj-" + UniqueId.nextId
    }

    val injectable = new Injectable(config, id, node.getNodeName, stepExpr ,conditionExpr, baseURI, new NodeLocation(node))
    if ((node.getNodeName == XProcConstants.p_input) || (node.getNodeName == XProcConstants.p_output)) {
      val port = Option(node.getAttributeValue(XProcConstants._port))
      if (port.isDefined) {
        injectable.port = port.get
      }
    }

    if (message.isDefined) {
      val messageExpr = new XProcVtExpression(context, message.get, true)
      injectable.messageXPath = messageExpr
    } else {
      injectable.messageNodes = messageNodes.get.toList
    }

    injectables += injectable
  }
}
