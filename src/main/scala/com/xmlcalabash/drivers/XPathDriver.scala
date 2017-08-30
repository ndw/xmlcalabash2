package com.xmlcalabash.drivers

import com.xmlcalabash.parsers.XPathParser
import com.xmlcalabash.runtime.SaxonRuntimeConfiguration
import net.sf.saxon.s9api.Processor

object XPathDriver extends App {
  val config = new SaxonRuntimeConfiguration(new Processor(false))

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

  expr = "$foo/test[. = f($x)]"

  parser.parse(expr)

  println("E: " + expr)
  for (varname <- parser.variableRefs()) {
    println("V: " + varname)
  }
  for (fname <- parser.functionRefs()) {
    println("F: " + fname)
  }


}
