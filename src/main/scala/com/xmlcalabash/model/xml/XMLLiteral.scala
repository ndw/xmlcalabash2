package com.xmlcalabash.model.xml

import com.xmlcalabash.core.XProcConstants
import com.xmlcalabash.model.xml.util.TreeWriter
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class XMLLiteral extends XMLArtifact {
  def this(node: XdmNode, parent: Option[XMLArtifact]) {
    this()
    _xmlname = "XMLLiteral"
    _node = node
    _parent = parent
  }

  override def dump(tree: TreeWriter): Unit = {
    tree.addSubtree(_node)
  }
}
