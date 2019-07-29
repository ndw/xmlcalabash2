package com.xmlcalabash.test

import java.io.File

import com.xmlcalabash.util.XProcTestSpec

import scala.collection.mutable.ListBuffer
import scala.io.Source

class RunPassingTestsSpec extends XProcTestSpec {
  val tests = ListBuffer.empty[String]

  val overlist = Option(System.getenv("TEST_LIST")).getOrElse("")
  if (overlist != "") {
    for (test <- overlist.split("\\s+")) {
      tests += s"src/test/resources/test-suite/test-suite/tests/$test"
    }
  } else {
    val filename = "src/test/resources/passing-tests.txt"
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
    runtests("Expected to pass", tests.toList)
  }
}
