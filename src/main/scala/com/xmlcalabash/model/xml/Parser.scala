package com.xmlcalabash.model.xml

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.exceptions.{ExceptionCode, ModelException}
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.model.xml.containers.{Catch, Choose, Finally, ForEach, Group, Otherwise, Try, When}
import com.xmlcalabash.model.xml.datasource.{Document, Inline, Pipe}
import net.sf.saxon.s9api.{Axis, XdmNode, XdmNodeKind}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ListBuffer

class Parser(config: XMLCalabash) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var exception: Option[Throwable] = None

  def parsePipeline(node: XdmNode): DeclareStep = {
    val art = parse(node)
    if (exception.isEmpty) {
      if (art.isEmpty) {
        exception = Some(new ModelException(ExceptionCode.INTERNAL, "This can't happen", node))
        config.errorListener.error(exception.get)
        throw exception.get
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
            case XProcConstants.p_documentation => Some(parseDocumentation(parent, node))
            case XProcConstants.p_pipeinfo => Some(parsePipeInfo(parent, node))
            case _ =>
              if (config.signatures.stepTypes.contains(node.getNodeName)) {
                Some(parseAtomicStep(parent, node))
              } else {
                if (parent.isDefined) {
                  parent.get match {
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
}
