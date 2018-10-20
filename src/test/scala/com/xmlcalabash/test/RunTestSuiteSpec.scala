package com.xmlcalabash.test

import java.io.File

import com.xmlcalabash.util.XProcTestSpec

class RunTestSuiteSpec extends XProcTestSpec {
  val root = new File("src/test/resources/test-suite/test-suite/tests")
  if (root.exists) {
    runtests("Run the XProc test suite", root.getAbsolutePath)
  } else {
    println("XProc test suite not present: assuming pass :-)")
  }
}
