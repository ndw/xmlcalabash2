package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, UniqueId, XProcConstants}
import com.xmlcalabash.model.xml.containers.{Catch, Choose, Finally, ForEach, Group, Otherwise, Try, When, WithDocument, WithProperties}
import com.xmlcalabash.model.xml.datasource.{Document, Inline, Pipe}
import com.xmlcalabash.runtime.injection.{XProcPortInjectable, XProcStepInjectable}
import com.xmlcalabash.runtime.{ExpressionContext, NodeLocation, XProcAvtExpression, XProcXPathExpression}
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.{Axis, QName, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

class Parser(config: XMLCalabash) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var exception: Option[Throwable] = None
  private val injectables = ListBuffer.empty[Injectable]

  def parsePipeline(node: XdmNode): DeclareStep = {
    for (injectable <- injectables) {
      injectable.findSteps(node)
    }

    val art = parse(node)
    if (exception.isEmpty) {
      if (art.isEmpty) {
        exception = Some(new ModelException(ExceptionCode.INTERNAL, "This can't happen", node))
        config.errorListener.error(exception.get)
        throw exception.get
      }

      for (injectable <- injectables) {
        if (!injectable.matched) {
          if (injectable.nodes.nonEmpty) {
            logger.warn(s"XPath expression did not match any steps: ${injectable.stepXPath.expr}")
          }
        }
      }

      art.get match {
        case step: DeclareStep =>
          return step
        case _ =>
          val badroot = new ModelException(ExceptionCode.BADPIPELINEROOT, node.toString, node)
          config.errorListener.error(badroot)
          exception = Some(badroot)
      }
    }

    throw exception.get
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
            case XProcConstants.p_with_properties => Some(parseWithProperties(parent, node))
            case XProcConstants.p_with_document => Some(parseWithDocument(parent, node))
            case XProcConstants.p_documentation => Some(parseDocumentation(parent, node))
            case XProcConstants.p_pipeinfo => Some(parsePipeInfo(parent, node))
            case _ =>
              if (config.signatures.stepTypes.contains(node.getNodeName)) {
                Some(parseAtomicStep(parent, node))
              } else {
                if (parent.isDefined) {
                  parent.get match {
                    case input: WithInput =>
                      logger.debug("Interpreting naked content of p:with-input as a p:inline")
                      Some(parseInline(parent, node))
                    case input: Input =>
                      logger.debug("Interpreting naked content of p:input as a p:inline")
                      Some(parseInline(parent, node))
                    case _ =>
                      throw new ModelException(ExceptionCode.NOTASTEP, node.getNodeName.toString, node)
                  }
                } else {
                  throw new ModelException(ExceptionCode.NOTASTEP, node.getNodeName.toString, node)
                }
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
      case _ => None
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
    art
  }

  private def parseWithInput(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new WithInput(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseInline(parent: Option[Artifact], node: XdmNode): Artifact = {
    val nodes = ListBuffer.empty[XdmNode]
    if (node.getNodeName == XProcConstants.p_inline) {
      val iter = node.axisIterator(Axis.CHILD)
      while (iter.hasNext) {
        nodes += iter.next().asInstanceOf[XdmNode]
      }
    } else {
      val iter = node.getParent.axisIterator(Axis.CHILD)
      while (iter.hasNext) {
        nodes += iter.next().asInstanceOf[XdmNode]
      }
    }

    val art = new Inline(config, parent, nodes.toList)
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

  private def parseWithProperties(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new WithProperties(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseWithDocument(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new WithDocument(config, parent)
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

    val context = new ExpressionContext(node.getBaseURI, S9Api.inScopeNamespaces(node), new NodeLocation(node))
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
      val messageExpr = new XProcAvtExpression(context, message.get)
      injectable.messageXPath = messageExpr
    } else {
      injectable.messageNodes = messageNodes.get.toList
    }

    injectables += injectable
  }
}
