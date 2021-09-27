package com.xmlcalabash.test

import com.xmlcalabash.util.XProcTestSpec

import java.io.File
import scala.collection.mutable.ListBuffer
import scala.io.Source

class RunFailingTestsSpec extends XProcTestSpec {
  if (Option(System.getenv("GO_SERVER_URL")).isDefined) {
    println("Failing tests are not run on CI")
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
        for (line <- bufferedSource.getLines()) {
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
