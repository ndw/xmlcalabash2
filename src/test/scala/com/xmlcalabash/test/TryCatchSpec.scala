package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class TryCatchSpec extends XProcTestSpec {
  "A try/catch " should " be able to succeed" in {
    test("src/test/resources/try-catch-001.xml")
  }

  "A try/catch " should " be able to catch a specific code" in {
    test("src/test/resources/try-catch-002.xml")
  }

  "A try/catch " should " be able to catch without a code" in {
    test("src/test/resources/try-catch-003.xml")
  }

  "The finally on try/catch " should " run if the try succeeds" in {
    test("src/test/resources/try-catch-004.xml")
  }

  "The finally on try/catch " should " run the catch has a specific code" in {
    test("src/test/resources/try-catch-005.xml")
  }

  "The finally on try/catch " should " run the catch doesn't have a code" in {
    test("src/test/resources/try-catch-006.xml")
  }

  "A try/catch " should " be able to catch a specific code that's first in a list" in {
    test("src/test/resources/try-catch-007.xml")
  }

  "A try/catch " should " be able to catch a specific code that's not first in a list" in {
    test("src/test/resources/try-catch-008.xml")
  }

}
