package com.xmlcalabash.runtime

import java.net.URI

import com.jafpl.graph.Location
import net.sf.saxon.s9api.{Axis, XdmNode, XdmNodeKind}

class NodeLocation(private val node: XdmNode) extends Location {
  private var _uri: URI = node.getBaseURI
  private var _line: Int = node.getLineNumber
  private var _col: Int = node.getColumnNumber

  private var pi = Option.empty[XdmNode]
  private var found = false
  private val iter = node.axisIterator(Axis.PRECEDING_SIBLING)
  while (iter.hasNext) {
    val pnode = iter.next()
    if (!found) {
      pnode.getNodeKind match {
        case XdmNodeKind.TEXT =>
          if (pnode.getStringValue.trim != "") {
            found = true
          }
        case XdmNodeKind.PROCESSING_INSTRUCTION =>
          if (pnode.getNodeName.getLocalName == "_xmlcalabash") {
            pi = Some(pnode)
            found = true
          }
        case _ => found = true
      }
    }
  }

  if (found && pi.isDefined) {
    val str      = pi.get.getStringValue
    val uripatn  = ".*uri=\"([^\"]+)\".*".r
    val linepatn = ".*line=\"(\\d+)\".*".r
    val colpatn  = ".*column=\"(\\d+)\".*".r

    str match {
      case uripatn(uri) =>
        _uri = new URI(uri)
      case _ =>
        _uri = null
    }

    str match {
      case linepatn(line) =>
        _line = line.toInt
      case _ =>
        _line = -1
    }

    str match {
      case colpatn(col) =>
        _col = col.toInt
      case _ =>
        _col = -1
    }
  }

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
