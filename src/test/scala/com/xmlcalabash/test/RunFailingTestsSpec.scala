package com.xmlcalabash.test

import java.io.File

import com.xmlcalabash.util.XProcTestSpec

import scala.collection.mutable.ListBuffer
import scala.io.Source

class RunFailingTestsSpec extends XProcTestSpec {
  if (Option(System.getenv("TRAVIS")).isDefined) {
    println("Failing tests are not run on Travis")
  } else {
    val tests = ListBuffer.empty[String]

    val overlist = Option(System.getenv("TEST_LIST")).getOrElse("")
    if (overlist != "") {
      for (test <- overlist.split("\\s+")) {
        tests += s"src/test/resources/test-suite/test-suite/tests/$test"
      }
    } else {
      val filename = "src/test/resources/failing-tests.txt"
      val passing = new File(filename)
      if (passing.exists) {
        var skip = false
        val bufferedSource = Source.fromFile(passing)
        for (line <- bufferedSource.getLines) {
          skip = skip || line == "EOF"
          if (!skip) {
            tests += s"src/test/resources/test-suite/test-suite/tests/$line"
          }
        }
        bufferedSource.close
      } else {
        println(s"No tests? $filename")
      }
    }

    if (tests.nonEmpty) {
      runtests("Expected to fail", tests.toList)
    }
  }
}
