package com.xmlcalabash.drivers

import com.xmlcalabash.model.tpl.{TplParser, TreeBuilder}

import scala.io.Source

object TplDriver extends App {
  if (args.isEmpty) {
    println("Usage: Driver filename[.tpl]")
    System.exit(1)
  }

  var text = ""
  val bufferedSource = Source.fromFile(args.head)
  for (line <- bufferedSource.getLines) {
    text += line + "\n"
  }
  bufferedSource.close

  val handler = new TreeBuilder()
  val parser = new TplParser(text, handler)
  parser.parse

  handler.stack.head.simplify()
  handler.stack.head.dump()

}
