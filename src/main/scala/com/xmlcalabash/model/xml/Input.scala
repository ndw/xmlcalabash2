package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Input(node: Option[XdmNode], parent: Option[XMLArtifact]) extends XMLArtifact(node, parent) {
  override def fixup(): Unit = {
    if (_children.size == 1 && _children.head.isInstanceOf[XMLLiteral]) {
      val inline = new Inline(None, Some(this))
      inline.xmlname = "inline"
      inline.addChild(_children.head)
      _children.remove(0)
      _children += inline
    }
  }
}
