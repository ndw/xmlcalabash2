package com.xmlcalabash.test

import java.io.File

import com.xmlcalabash.util.XProcTestSpec

class RunFailingTestsSpec extends XProcTestSpec {
  if (Option(System.getenv("TRAVIS")).isDefined) {
    println("Failing tests are not run on Travis")
  } else {
    val root = new File("src/test/resources/test-suite/failing-x")
    if (root.exists) {
      runtests("Run the XProc test suite", root.getAbsolutePath)
    } else {
      println("XProc test suite not present: assuming pass :-)")
    }
  }
}
