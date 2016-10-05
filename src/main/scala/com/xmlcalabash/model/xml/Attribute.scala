package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.{QName, XdmNode}

/**
  * Created by ndw on 10/4/16.
  */
class Attribute(val name: QName, val value: String) extends XMLArtifact(None, None) {
  def this(node: XdmNode) {
    this(node.getNodeName, node.getStringValue)
  }
}

