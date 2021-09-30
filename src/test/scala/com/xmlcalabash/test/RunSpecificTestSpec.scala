package com.xmlcalabash.test

import com.xmlcalabash.util.XProcTestSpec

class RunSpecificTestSpec extends XProcTestSpec {
  if (Option(System.getenv("GO_SERVER_URL")).isDefined) {
    println("A specific test is not run on CI")
  } else {
    runtest("Run specific test", "/Users/ndw/Projects/xproc/test-suite/test-suite/tests/ab-inline-008.xml")
    //runtest("Run specific test", "/Users/ndw/Projects/xproc/test-suite/test-suite/tests/ab-http-request-061.xml")
    //runtest("Run specific test", "src/test/resources/extension-tests/tests/cx-until-001.xml")
  }
}
