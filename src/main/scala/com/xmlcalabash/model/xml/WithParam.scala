package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class WithParam extends XMLArtifact {
  def this(node: XdmNode, parent: Option[XMLArtifact]) {
    this()
    initNode(node, parent)
    parse(node)
  }
}
