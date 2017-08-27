package com.xmlcalabash.drivers

import com.xmlcalabash.model.tpl.{PipelineBuilder, TplParser, TreeBuilder}
import com.xmlcalabash.model.util.DefaultParserConfiguration

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

  val config = new DefaultParserConfiguration()
  val tbuilder = new TreeBuilder()
  val pbuilder = new PipelineBuilder(config)
  val parser = new TplParser(text, tbuilder)
  parser.parse


}
