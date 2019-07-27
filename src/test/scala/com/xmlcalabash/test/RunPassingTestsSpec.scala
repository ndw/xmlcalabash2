package com.xmlcalabash.test

import java.io.File

import com.xmlcalabash.util.XProcTestSpec

import scala.collection.mutable.ListBuffer
import scala.io.Source

class RunPassingTestsSpec extends XProcTestSpec {
  val filename = "src/test/resources/passing-tests.txt"
  val passing = new File(filename)
  if (passing.exists) {
    val tests = ListBuffer.empty[String]
    val bufferedSource = Source.fromFile(passing)
    for (line <- bufferedSource.getLines) {
      tests += s"src/test/resources/test-suite/test-suite/tests/$line"
    }
    bufferedSource.close
    runtests("Expected to pass", tests.toList)
  } else {
    println(s"No tests? $filename")
  }
}
