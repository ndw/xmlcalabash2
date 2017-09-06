package com.xmlcalabash.drivers

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.testers.TestRunner

object TestDriver extends App {
  private val xmlCalabash = XMLCalabash.newInstance()

  /*
  val tester = new Tester(parserConfig, runtimeConfig)
  tester.pipeline = "/projects/github/xproc/meerschaum/pipe.xpl"
  tester.schematron = "/projects/github/xproc/meerschaum/pipe.sch"
  tester.run()
  */

  val runner = new TestRunner(xmlCalabash, "src/test/resources")
  try {
    runner.run()
  } catch {
    case t: Throwable => println(t)
  }


}
