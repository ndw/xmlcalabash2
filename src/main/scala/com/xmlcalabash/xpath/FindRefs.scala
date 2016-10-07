package com.xmlcalabash.xpath

import com.xmlcalabash.xpath.CR_xpath_31_20151217.EventHandler

import scala.collection.mutable

/**
  * Created by ndw on 10/6/16.
  */
class FindRefs extends EventHandler {
  private val varlist = mutable.ListBuffer.empty[String]
  private val funclist = mutable.ListBuffer.empty[String]
  private var chars: CharSequence = ""
  private var sawDollar = false
  private var maybeFunction = false

  def variableRefs(): List[String] = {
    varlist.toList
  }

  def functionRefs(): List[String] = {
    funclist.toList
  }

  override def reset(str: CharSequence): Unit = {
    chars = str
  }

  override def startNonterminal(name: String, begin: Int): Unit = {
    //println("SNT: " + name)
    maybeFunction = (name == "FunctionName")
  }

  override def endNonterminal(name: String, end: Int): Unit = {
    //println("ENT: " + name)
    if (name == "FunctionName") {
      maybeFunction = false
    }
  }

  override def terminal(name: String, begin: Int, end: Int): Unit = {
    //println("T: " + name)
    if (sawDollar) {
      varlist += chars.subSequence(begin, end).toString
    } else {
      if (maybeFunction) {
        funclist += chars.subSequence(begin, end).toString
      }
    }
    sawDollar = (name == "'$'")
  }

  override def whitespace(begin: Int, end: Int): Unit = {
    // nop
  }
}
