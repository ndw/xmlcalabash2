package com.xmlcalabash.model.tpl

import com.xmlcalabash.model.tpl.TplParser.EventHandler

import scala.collection.mutable

class TreeBuilder extends EventHandler {
  private var input: String = null

  val stack: mutable.ListBuffer[TplNode] = mutable.ListBuffer.empty[TplNode]

  def reset(string: String) {
    input = string
  }

  def startNonterminal(name: String, begin: Int) {
    println(" NT: " + name)
    stack += new TplNode(name)
  }

  def endNonterminal(name: String, end: Int) {
    println("/NT: " + name)
    val popped = mutable.ListBuffer.empty[TplNode]
    while (stack.last.name != name) {
      popped.insert(0, stack.last)
      stack.remove(stack.size - 1)
    }
    for (arg <- popped) {
      stack.last.args += arg
    }
  }

  def terminal(name: String, begin: Int, end: Int) {
    val tag = if (name(0) == '\'') "TOKEN" else name
    if (tag == "TOKEN") {
      // nop
    } else {
      println("  T: " + name)
    }

    stack += new TplNode(tag, Some(characters(begin,end)))
  }

  def whitespace(begin: Int, end: Int) {
    // nop
  }

  private def characters(begin: Int, end: Int): String = {
    if (begin < end) {
      input.substring(begin, end)
    } else {
      ""
    }
  }

}
