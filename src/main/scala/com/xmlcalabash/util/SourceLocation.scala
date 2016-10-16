package com.xmlcalabash.util

import java.net.URI

import net.sf.saxon.expr.parser.Location
import net.sf.saxon.s9api.XdmNode

/**
  * Created by ndw on 9/30/16.
  */
class SourceLocation(val uri: URI, val line: Int, val column: Int) extends Location {
  def this(node: XdmNode) {
    this(node.getBaseURI, node.getLineNumber, node.getColumnNumber)
  }

  override def getLineNumber: Int = line

  override def getColumnNumber: Int = column

  override def saveLocation(): Location = this

  override def getSystemId: String = uri.toASCIIString

  override def getPublicId: String = null
}
