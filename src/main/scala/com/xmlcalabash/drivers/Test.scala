package com.xmlcalabash.drivers

import java.io.File
import java.net.URI

import com.xmlcalabash.config.{DocumentRequest, XMLCalabashConfig}
import com.xmlcalabash.testing.TestRunner
import com.xmlcalabash.util.{MediaType, S9Api}
import net.sf.saxon.s9api.Serializer

import scala.collection.mutable.ListBuffer

object Test extends App {
  private val xmlCalabash = XMLCalabashConfig.newInstance()

  private var debug = false
  private var showPassing = false
  private var showFailing = false
  private var showSkipping = false
  private var xmlOutput: Option[String] = None
  private var testLocations = ListBuffer.empty[String]

  protected val online: Boolean = try {
    val docreq = new DocumentRequest(new URI("http://www.w3.org/"), MediaType.HTML)
    val doc = xmlCalabash.documentManager.parse(docreq)
    true
  } catch {
    case ex: Exception => false
  }

  crudeArgParse()

  private val showAll = !showPassing && !showFailing && !showSkipping

  if (testLocations.isEmpty) {
    println("Usage: com.xmlcalabash.drivers.Test [-h htmloutput] [-j junitoutput] testlocation [testlocation+]")
  }

  try {
    val runner = new TestRunner(xmlCalabash, online, testLocations.toList)

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
          if (showAll || showSkipping) {
            println(s"SKIP: ${result.baseURI}")
          }
          skip += 1
        } else if (result.passed) {
          if (showAll || showPassing) {
            println(s"PASS: ${result.baseURI}")
          }
          pass += 1
        } else if (result.failed) {
          if (showAll || showFailing) {
            println(s"FAIL: ${result.baseURI}")
          }
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
    val optp = "-(p)".r
    val optf = "-(f)".r
    val opts = "-(s)".r
    val optx = "-(.*)".r

    var pos = 0
    while (pos < args.length) {
      val arg = args(pos)
      arg match {
        case optd(opt) =>
          debug = true
        case optp(opt) =>
          showPassing = true
        case optf(opt) =>
          showFailing = true
        case opts(opt) =>
          showSkipping = true
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
