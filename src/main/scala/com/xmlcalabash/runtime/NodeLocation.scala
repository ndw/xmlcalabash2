package com.xmlcalabash.runtime

import com.jafpl.graph.Location
import net.sf.saxon.s9api.XdmNode

class NodeLocation(private val node: XdmNode) extends Location {
  private val _uri = node.getBaseURI
  private val _line = node.getLineNumber
  private val _col = node.getColumnNumber

  override def uri: Option[String] = Some(_uri.toASCIIString)

  override def line: Option[Long] = {
    if (_line > 0) {
      Some(_line.toLong)
    } else {
      None
    }
  }

  override def column: Option[Long] = {
    if (_col > 0) {
      Some(_col.toLong)
    } else {
      None
    }
  }

  override def toString: String = {
    var str = uri.get
    if (line.isDefined) {
      str += ":" + line.get.toString
    }
    if (column.isDefined) {
      str += ":" + column.get.toString
    }
    str
  }
}
