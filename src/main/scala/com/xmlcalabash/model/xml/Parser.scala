package com.xmlcalabash.model.xml

import java.io.{FileWriter, PrintWriter, StringWriter}

import com.xmlcalabash.core.{XProcConstants, XProcEngine, XProcException}
import com.xmlcalabash.model.xml.util.{NodeUtils, TreeWriter}
import net.sf.saxon.s9api.XdmNode
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/4/16.
  */
class Parser(val engine: XProcEngine) {
  val logger = LoggerFactory.getLogger(this.getClass)

  def parse(document: XdmNode): Artifact = {
    logger.debug("Parsing " + document.getBaseURI)

    val node = NodeUtils.getDocumentElement(document)
    if (node.isEmpty) {
      throw new XProcException("Attempt to parse empty XML document")
    }

    val artifact = node.get.getNodeName match {
      case XProcConstants.p_pipeline => new Pipeline(node, None)
      case XProcConstants.p_declare_step => new DeclareStep(node, None)
      case XProcConstants.p_library => new Library(node, None)
      case _ => throw new XProcException("Attempt to parse something that isn't a pipeline")
    }

    artifact.fixup()
    artifact
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
