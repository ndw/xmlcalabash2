package com.xmlcalabash.test

import com.xmlcalabash.util.XProcTestSpec

class RunSpecificTestSpec extends XProcTestSpec {
  if (Option(System.getenv("GO_SERVER_URL")).isDefined) {
    println("A specific test is not run on Travis")
  } else {
    runtest("Run specific test", "src/test/resources/test-suite/test-suite/tests/ab-depends-004.xml")
    //runtest("Run specific test", "src/test/resources/extension-tests/tests/system-property-001.xml")
  }
}
