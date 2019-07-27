package com.xmlcalabash.test

import java.io.File

import com.xmlcalabash.util.XProcTestSpec

import scala.collection.mutable.ListBuffer
import scala.io.Source

class RunFailingTestsSpec extends XProcTestSpec {
  if (Option(System.getenv("TRAVIS")).isDefined) {
    println("Failing tests are not run on Travis")
  } else {
    val filename = "src/test/resources/failing-tests.txt"
    val passing = new File(filename)
    if (passing.exists) {
      val tests = ListBuffer.empty[String]
      val bufferedSource = Source.fromFile(passing)
      for (line <- bufferedSource.getLines) {
        tests += s"src/test/resources/test-suite/test-suite/tests/$line"
      }
      bufferedSource.close
      runtests("Expected to fail", tests.toList)
    } else {
      println(s"No tests? $filename")
    }
  }
}
