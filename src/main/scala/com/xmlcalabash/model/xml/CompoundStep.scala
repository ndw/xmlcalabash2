package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class CompoundStep(node: Option[XdmNode], parent: Option[XMLArtifact]) extends XMLArtifact(node, parent) {
  override def parse(node: Option[XdmNode]): Unit = {
    if (node.isDefined) {
      parseNamespaces(node.get)
      parseAttributes(node.get)
      parseChildren(node.get, stepsAllowed = true)
    }
  }
}
