package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcConstants, XProcEngine}
import com.xmlcalabash.model.xml.bindings.{Data, Document, Inline, Pipe}
import com.xmlcalabash.model.xml.decl.StepLibrary
import com.xmlcalabash.model.xml.util.{RelevantNodes, TreeWriter}
import com.xmlcalabash.util.{NodeUtils, PipelineErrorListener, SourceLocation}
import net.sf.saxon.s9api.{Axis, XdmNode}
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/4/16.
  */
class Parser(val engine: XProcEngine, val errorListener: PipelineErrorListener) {
  val logger = LoggerFactory.getLogger(this.getClass)
  val listener = errorListener match {
    case xml: XMLErrorListener => xml
    case any: PipelineErrorListener => new XMLErrorListener(any)
  }

  def parse(document: XdmNode, against: List[StepLibrary]): Option[Artifact] = {
    logger.debug("Parsing " + document.getBaseURI)

    val node = NodeUtils.getDocumentElement(document)
    if (node.isEmpty) {
      listener.error(new SourceLocation(document), "Attempt to parse empty XML document")
      return None
    }

    val artifact = walk(node.get)
    if (artifact.isDefined) {
      if (artifact.get.validate(against, listener)) {
        artifact.get.fixup()
        artifact
      } else {
        None
      }
    } else {
      None
    }
  }

  def walk(node: XdmNode): Option[PipelineDocument] = {
    val artifact = node.getNodeName match {
      case XProcConstants.p_pipeline => Some(new Pipeline(Some(node), None))
      case XProcConstants.p_declare_step => Some(new DeclareStep(Some(node), None))
      case XProcConstants.p_library => Some(new Library(Some(node), None))
      case _ =>
        listener.error(Some(node), "Attempt to parse something that isn't a pipeline")
        None
    }
    if (artifact.isDefined) {
      walkChildren(artifact.get, node, 0)
    }
    artifact
  }

  def walkChildren(artifact: Artifact, node: XdmNode, depth: Int): Unit = {
    //println(" " * (depth*2) + node.getNodeName)
    for (childitem <- RelevantNodes.filter(node, Axis.CHILD)) {
      val child = childitem.asInstanceOf[XdmNode]

      val node: Option[Artifact] = child.getNodeName match {
        case XProcConstants.p_catch => Some(new Catch(Some(child), Some(artifact)))
        case XProcConstants.p_choose => Some(new Choose(Some(child), Some(artifact)))
        case XProcConstants.p_data => Some(new Data(Some(child), Some(artifact)))
        case XProcConstants.p_declare_step => Some(new DeclareStep(Some(child), Some(artifact)))
        case XProcConstants.p_document => Some(new Document(Some(child), Some(artifact)))
        case XProcConstants.p_empty => Some(new Empty(Some(child), Some(artifact)))
        case XProcConstants.p_for_each => Some(new ForEach(Some(child), Some(artifact)))
        case XProcConstants.p_group => Some(new Group(Some(child), Some(artifact)))
        case XProcConstants.p_import => Some(new Import(Some(child), Some(artifact)))
        case XProcConstants.p_inline => Some(new Inline(Some(child), Some(artifact)))
        case XProcConstants.p_input => Some(new Input(Some(child), Some(artifact)))
        case XProcConstants.p_iteration_source => Some(new IterationSource(Some(child), Some(artifact)))
        case XProcConstants.p_library => Some(new Library(Some(child), Some(artifact)))
        case XProcConstants.p_log => Some(new Log(Some(child), Some(artifact)))
        case XProcConstants.p_namespaces => Some(new Namespaces(Some(child), Some(artifact)))
        case XProcConstants.p_option => Some(new OptionDecl(Some(child), Some(artifact)))
        case XProcConstants.p_otherwise => Some(new When(Some(child), Some(artifact), otherwise=true))
        case XProcConstants.p_output => Some(new Output(Some(child), Some(artifact)))
        case XProcConstants.p_pipe => Some(new Pipe(Some(child), Some(artifact)))
        case XProcConstants.p_pipeline => Some(new Pipeline(Some(child), Some(artifact)))
        case XProcConstants.p_serialization => Some(new Serialization(Some(child), Some(artifact)))
        case XProcConstants.p_try => Some(new Try(Some(child), Some(artifact)))
        case XProcConstants.p_variable => Some(new Variable(Some(child), Some(artifact)))
        case XProcConstants.p_viewport => Some(new Viewport(Some(child), Some(artifact)))
        case XProcConstants.p_viewport_source => Some(new ViewportSource(Some(child), Some(artifact)))
        case XProcConstants.p_when => Some(new When(Some(child), Some(artifact)))
        case XProcConstants.p_with_option => Some(new WithOption(Some(child), Some(artifact)))
        case XProcConstants.p_with_param => Some(new WithParam(Some(child), Some(artifact)))
        case XProcConstants.p_xpath_context => Some(new XPathContext(Some(child), Some(artifact)))
        case XProcConstants.p_pipeinfo => None
        case XProcConstants.p_documentation => None
        case _ =>
          if (artifact.isInstanceOf[CompoundStep]) {
            Some(new AtomicStep(Some(child), Some(artifact)))
          } else {
            Some(new XMLLiteral(Some(child), Some(artifact)))
          }
      }

      if (node.isDefined) {
        artifact.addChild(node.get)
        if (!node.get.isInstanceOf[XMLLiteral]) {
          walkChildren(node.get, child, depth + 1)
        }
      }
    }

    artifact.parseNamespaces(node)
    artifact.parseAttributes(node)
  }

  def dump(artifact: Artifact): XdmNode = {
    val tree = new TreeWriter(engine)
    tree.startDocument(null)

    tree.addStartElement(XProcConstants.px("pipeline"))
    artifact.dump(tree)
    tree.addEndElement()
    tree.endDocument()

    tree.getResult
  }
}
