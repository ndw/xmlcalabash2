package com.xmlcalabash.test

import com.xmlcalabash.util.XProcTestSpec

class RunSpecificTestSpec extends XProcTestSpec {
  if (Option(System.getenv("TRAVIS")).isDefined) {
    println("A specific test is not run on Travis")
  } else {
    runtest("Run specific test", "src/test/resources/test-suite/test-suite/tests/ab-tvt-006.xml")
    //runtest("Run specific test", "src/test/resources/extension-tests/tests/system-property-001.xml")
  }
}
