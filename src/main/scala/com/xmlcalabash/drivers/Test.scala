package com.xmlcalabash.drivers

import java.io.File

import com.xmlcalabash.config.XMLCalabashConfig
import com.xmlcalabash.testing.TestRunner
import com.xmlcalabash.util.S9Api
import net.sf.saxon.s9api.Serializer

import scala.collection.mutable.ListBuffer

object Test extends App {
  private val xmlCalabash = XMLCalabashConfig.newInstance()

  private var debug = false
  private var xmlOutput: Option[String] = None
  private var testLocations = ListBuffer.empty[String]

  crudeArgParse()

  if (testLocations.isEmpty) {
    println("Usage: com.xmlcalabash.drivers.Test [-h htmloutput] [-j junitoutput] testlocation [testlocation+]")
  }

  try {
    val runner = new TestRunner(xmlCalabash, testLocations.toList)

    if (xmlOutput.isDefined) {
      val junit = runner.junit()
      println(s"Writing JUnit XML test report to ${xmlOutput.get}")
      val serializer = xmlCalabash.processor.newSerializer()
      serializer.setOutputFile(new File(xmlOutput.get))
      serializer.setOutputProperty(Serializer.Property.METHOD, "xml")
      S9Api.serialize(xmlCalabash, junit, serializer)
    } else {
      var total = 0
      var pass = 0
      var skip = 0
      var fail = 0
      for (result <- runner.run()) {
        total += 1
        if (result.skipped.isDefined) {
          println(s"SKIP: ${result.baseURI}")
          skip += 1
        } else if (result.passed) {
          //println(s"PASS: ${result.baseURI}")
          pass += 1
        } else if (result.failed) {
          println(s"FAIL: ${result.baseURI}")
          fail += 1
        }
      }

      if (total == 1) {
        println(s"$total test: [passed: $pass, skip: $skip, fail: $fail]")
      } else {
        println(s"$total tests: [passed: $pass, skip: $skip, fail: $fail]")
      }
    }
  } catch {
    case ex: Exception =>
      println(ex.getMessage)
      if (debug) {
        ex.printStackTrace()
      }
  }

  private def crudeArgParse(): Unit = {
    // Read the command line arguments crudely

    val optd = "-(d)".r
    val optj = "-[jx](.*)".r
    val optx = "-(.*)".r

    var pos = 0
    while (pos < args.length) {
      val arg = args(pos)
      arg match {
        case optd(opt) =>
          debug = true
        case optj(opt) =>
          xmlOutput = Some(opt)
          if (opt == "") {
            pos += 1
            xmlOutput = Some(args(pos))
          }
        case optx(opt) =>
          throw new RuntimeException(s"Unknown option: -$opt")
        case _ =>
          testLocations += arg
      }
      pos += 1
    }
  }
}
