package com.xmlcalabash.util

import java.net.URI

import com.jafpl.util.SourceLocation
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 9/30/16.
  */
class XProcSourceLocation(val baseURI: URI, val lineNumber: Int, val columnNumber: Int) extends SourceLocation {
  def this(node: XdmNode) {
    this(node.getBaseURI, node.getLineNumber, node.getColumnNumber)
  }
}
