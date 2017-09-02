package com.xmlcalabash.drivers

import com.xmlcalabash.model.util.DefaultParserConfiguration
import com.xmlcalabash.runtime.SaxonRuntimeConfiguration
import com.xmlcalabash.testers.{TestRunner, Tester}
import net.sf.saxon.s9api.Processor

object TestDriver extends App {
  private val processor = new Processor(false)
  val parserConfig = new DefaultParserConfiguration()
  val runtimeConfig = new SaxonRuntimeConfiguration(processor)

  /*
  val tester = new Tester(parserConfig, runtimeConfig)
  tester.pipeline = "/projects/github/xproc/meerschaum/pipe.xpl"
  tester.schematron = "/projects/github/xproc/meerschaum/pipe.sch"
  tester.run()
  */

  val runner = new TestRunner(parserConfig, runtimeConfig, "src/test/resources")
  try {
    runner.run()
  } catch {
    case t: Throwable => println(t)
  }


}
