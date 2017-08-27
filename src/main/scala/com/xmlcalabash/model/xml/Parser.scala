package com.xmlcalabash.model.xml

import com.xmlcalabash.model.exceptions.ModelException
import com.xmlcalabash.model.util.{ErrorListener, ParserConfiguration}
import com.xmlcalabash.model.xml.datasource.{Document, Inline}
import net.sf.saxon.s9api.{Axis, XdmNode, XdmNodeKind}

import scala.collection.mutable.ListBuffer
import scala.reflect.macros.ParseException

class Parser(config: ParserConfiguration) {
  private var exception: Option[Throwable] = None

  def parsePipeline(node: XdmNode): DeclareStep = {
    val art = parse(node)
    art match {
      case step: DeclareStep =>
        step
      case _ => throw new ModelException("badroot", s"Node did not define a pipeline: $node")
    }
  }

  private def parse(node: XdmNode): Artifact = {
    val root = parse(None, node)
    if (root.isDefined) {
      try {
        if (!root.get.validate()) {
          // throw new ModelException("invalid", "Pipeline is invalid")
          println("PIPELINE IS INVALID")
        }
      } catch {
        case cause: Throwable => error(cause)
        case _: Throwable => Unit
      }
    }

    if (exception.isDefined) {
      throw exception.get
    } else {
      root.get
    }
  }

  private def error(cause: Throwable): Unit = {
    exception = Some(cause)
    config.errorListener.error(cause, None)
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
        val art: Option[Artifact] = node.getNodeName match {
          case XProcConstants.p_declare_step => Some(parseDeclareStep(parent, node))
          case XProcConstants.p_pipeline => Some(parsePipeline(parent, node))
          case XProcConstants.p_serialization => Some(parseSerialization(parent, node))
          case XProcConstants.p_output => Some(parseOutput(parent, node))
          case XProcConstants.p_input => Some(parseInput(parent, node))
          case XProcConstants.p_inline => Some(parseInline(parent, node))
          case XProcConstants.p_document => Some(parseDocument(parent, node))
          case XProcConstants.p_documentation => Some(parseDocumentation(parent, node))
          case XProcConstants.p_pipeinfo => Some(parsePipeInfo(parent, node))
          case _ => Some(parseAtomicStep(parent, node))
        }
        art
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
    input.properties.put(XProcConstants._port, "source")
    input.properties.put(XProcConstants._primary, "true")
    art.children += input

    val output = new Output(config, Some(art))
    output.properties.put(XProcConstants._port, "result")
    output.properties.put(XProcConstants._primary, "true")
    art.children += output

    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseAtomicStep(parent: Option[Artifact], node: XdmNode): Artifact = {
    println(s"Assuming atomic step: ${node.getNodeName}")
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
    val art = new Inline(config, parent)
    art.parse(node)
    parseChildren(art, node)
    art
  }

  private def parseDocument(parent: Option[Artifact], node: XdmNode): Artifact = {
    val art = new Document(config, parent)
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
}
