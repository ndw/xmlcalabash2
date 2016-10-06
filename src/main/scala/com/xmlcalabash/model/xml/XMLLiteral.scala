package com.xmlcalabash.model.xml

import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class XMLLiteral(node: Option[XdmNode], parent: Option[Artifact]) extends Artifact(node, parent) {
  _xmlname = "XMLLiteral"

  override def parse(node: Option[XdmNode]): Unit = {
    // nop
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addSubtree(node.get)
  }
}
