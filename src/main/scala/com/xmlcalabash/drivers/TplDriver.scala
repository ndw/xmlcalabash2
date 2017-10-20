package com.xmlcalabash.drivers

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.tpl.{PipelineBuilder, TplParser, TreeBuilder}

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

  val config = XMLCalabash.newInstance()
  //val tbuilder = new TreeBuilder()
  val pbuilder = new PipelineBuilder(config)
  val parser = new TplParser(text, pbuilder)
  parser.parse

  val pipeline = pbuilder.pipeline
  println(pipeline)

}
