package com.xmlcalabash.util

import java.net.URI

import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 9/30/16.
  */
class SourceLocation(val baseURI: URI, val lineNumber: Int, val columnNumber: Int) {
  def this(node: XdmNode) {
    this(node.getBaseURI, node.getLineNumber, node.getColumnNumber)
  }
}
