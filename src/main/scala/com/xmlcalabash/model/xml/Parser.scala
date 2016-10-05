package com.xmlcalabash.model.xml

import com.xmlcalabash.core.{XProcConstants, XProcEngine, XProcException}
import com.xmlcalabash.model.xml.util.{NodeUtils, TreeWriter}
import net.sf.saxon.s9api.XdmNode
import org.slf4j.LoggerFactory

import scala.collection.mutable.Stack

/**
  * Created by ndw on 10/4/16.
  */
class Parser(val engine: XProcEngine) {
  val logger = LoggerFactory.getLogger(this.getClass)

  def parse(document: XdmNode): XMLArtifact = {
    logger.debug("Parsing " + document.getBaseURI)

    val node = NodeUtils.getDocumentElement(document)
    if (node.isEmpty) {
      throw new XProcException("Attempt to parse empty XML document")
    }

    val artifact = node.get.getNodeName match {
      case XProcConstants.p_pipeline => new Pipeline(node.get, None)
      case XProcConstants.p_declare_step => new DeclareStep(node.get, None)
      case XProcConstants.p_library => new Library(node.get, None)
      case _ => throw new XProcException("Attempt to parse something that isn't a pipeline")
    }

    artifact.fixup()

    val dumpNode = dump(artifact)
    println(dumpNode)

    artifact
  }

  private def dump(artifact: XMLArtifact): XdmNode = {
    val tree = new TreeWriter(engine)
    tree.startDocument(null)

    tree.addStartElement(XProcConstants.px("pipeline"))
    artifact.dump(tree)
    tree.addEndElement()
    tree.endDocument()

    tree.getResult
  }
}
