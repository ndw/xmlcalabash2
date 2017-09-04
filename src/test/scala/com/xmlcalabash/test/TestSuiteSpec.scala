package com.xmlcalabash.test

import com.xmlcalabash.model.util.DefaultParserConfiguration
import com.xmlcalabash.runtime.SaxonRuntimeConfiguration
import com.xmlcalabash.testers.TestRunner
import net.sf.saxon.s9api.Processor
import org.scalatest.FlatSpec

class TestSuiteSpec extends FlatSpec {
  val processor = new Processor(false)
  val parserConfig = new DefaultParserConfiguration()
  val runtimeConfig = new SaxonRuntimeConfiguration(processor)

  var error = Option.empty[String]

  "A non-pipeline document " should " fail " in {
    test("src/test/resources/not-a-pipeline.xml")
  }

  "An inline AVT " should " pass " in {
    test("src/test/resources/inline-avt.xml")
  }

  "An inline TVT " should " pass " in {
    test("src/test/resources/inline-tvt.xml")
  }

  "A p:inline " should " be defaulted " in {
    test("src/test/resources/default-p-input.xml")
  }

  "A p:parameters " should " read an AVT map " in {
    test("src/test/resources/parameters-001.xml")
  }

  "A p:parameters " should " read an inline with-option " in {
    test("src/test/resources/parameters-002.xml")
  }

  "A p:parameters " should " read a piped with-option " in {
    test("src/test/resources/parameters-003.xml")
  }

  "The context for step option AVTs " should " be the default readable port" in {
    test("src/test/resources/opt-drp.xml")
  }

  def test(fn: String) {
    val runner = new TestRunner(parserConfig, runtimeConfig, fn)
    try {
      error = runner.run()
    } catch {
      case t: Throwable => throw t
    }
    assert(error.isEmpty)
  }
}
