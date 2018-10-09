package com.xmlcalabash.runtime

import net.sf.saxon.s9api.XdmNode

trait ProcessMatchingNodes {
  def startDocument(node: XdmNode): Boolean
  def startElement(node: XdmNode): Boolean
  def endElement(node: XdmNode): Unit
  def endDocument(node: XdmNode): Unit

  def attribute(node: XdmNode): Unit
  def text(node: XdmNode): Unit
  def comment(node: XdmNode): Unit
  def pi(node: XdmNode): Unit
}
