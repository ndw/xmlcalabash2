package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class ChooseSpec extends XProcTestSpec {
  "A choose " should " be able to match the first when" in {
    test("src/test/resources/choose-001.xml")
  }

  "A choose " should " be able to match the second when" in {
    test("src/test/resources/choose-002.xml")
  }

  "A choose " should " be able to match the otherwise" in {
    test("src/test/resources/choose-003.xml")
  }

  "A choose " should " fail if no conditions match and there is no otherwise" in {
    test("src/test/resources/choose-004.xml")
  }
}
