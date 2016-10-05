package com.xmlcalabash.model.xml

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 10/4/16.
  */
class Input extends XMLArtifact {
  def this(node: XdmNode, parent: Option[XMLArtifact]) {
    this()
    initNode(node, parent)
    parse(node)
  }

  override def fixup(): Unit = {
    if (children.size == 1 && children.head.isInstanceOf[XMLLiteral]) {
      println("FIXUP UNWRAPPED INLINE")
      val inline = new Inline()
      inline.xmlname = "inline"
      inline.addChild(children.head)
      children.remove(0)
      children += inline
    }
  }
}
