package com.xmlcalabash.model.util

import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer

object StringParsers {
  def parseAvt(value: String): Option[List[String]] = {
    val list = ListBuffer.empty[String]
    var state = StateChange.STRING
    var pos = 0
    var substr = ""

    while (pos < value.length) {
      val ch = value.substring(pos, pos + 1)
      val nextch = if (pos + 1 < value.length) {
        value.substring(pos + 1, pos + 2)
      } else {
        ""
      }
      ch match {
        case "{" =>
          if (nextch == "{") {
            pos += 2
            substr += "{"
          } else {
            state match {
              case StateChange.STRING =>
                list += substr
                substr = ""
                state = StateChange.EXPR
                pos += 1
              case StateChange.EXPR =>
                substr += "{"
                pos += 1
            }
          }
        case "}" =>
          if (nextch == "}") {
            pos += 2
            substr += "}"
          } else {
            state match {
              case StateChange.STRING =>
                return None
              case StateChange.EXPR =>
                if (list.isEmpty) {
                  list += ""
                }
                list += substr
                substr = ""
                state = StateChange.STRING
                pos += 1
            }
          }
        case _ =>
          substr += ch
          pos += 1
      }
    }

    if (state != StateChange.STRING) {
      None
    } else {
      if (substr != "") {
        list += substr
      }
      Some(list.toList)
    }
  }

  def parseClarkName(name: String): QName = {
    parseClarkName(name, None)
  }

  def parseClarkName(name: String, prefix: String): QName = {
    parseClarkName(name, Some(prefix))
  }

  private def parseClarkName(name: String, pfx: Option[String]): QName = {
    // FIXME: Better error handling for ClarkName parsing
    if (name.startsWith("{")) {
      val pos = name.indexOf("}")
      val uri = name.substring(1, pos)
      val local = name.substring(pos + 1)
      if (pfx.isDefined) {
        new QName(pfx.get, uri, local)
      } else {
        new QName(uri, local)
      }
    } else {
      new QName("", name)
    }
  }

  object StateChange {
    val STRING = 0
    val EXPR = 1
  }
}
