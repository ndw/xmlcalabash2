package com.xmlcalabash.drivers

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.parsers.XPathParser

import scala.collection.mutable.ListBuffer

object XPathDriver extends App {
  val config = XMLCalabashConfig.newInstance()

  var expr = ""
  for (arg <- args) {
    expr += (arg + " ")
  }

  val parser = new XPathParser(config)

  /*
  expr = "3 + 4 * f($x) idiv g($y + $z)"

  parser.parse(expr)

  println("E: " + expr)
  for (varname <- parser.variableRefs()) {
    println("V: " + varname)
  }
  for (fname <- parser.functionRefs()) {
    println("F: " + fname)
  }
  */

  /*
  expr = "$foo/test[. = f($x)]"

  parser.parse(expr)

  println("E: " + expr)
  for (varname <- parser.variableRefs()) {
    println("V: " + varname)
  }
  for (fname <- parser.functionRefs()) {
    println("F: " + fname)
  }
  */

  expr = "Test "
  val list = parseAVT(expr)
  if (list.isDefined) {
    var avt = false
    for (item <- list.get) {
      println((if (avt) "AVT:" else "STR: ") + item)
      avt = !avt
    }
  } else {
    println("ERROR: invalid expression")
  }

  def parseAVT(value: String): Option[List[String]] = {
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

  object StateChange {
    val STRING = 0
    val EXPR = 1
  }

  class StateChange(state: Int, pos: Int) {
  }
}
