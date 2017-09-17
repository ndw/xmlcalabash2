package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class PipeAttrSpec extends XProcTestSpec {
  "The pipe attribute " should " work on p:input" in {
    test("src/test/resources/pipe-attr-001.xml")
  }

  "The pipe attribute " should " fail if the input also has connections" in {
    test("src/test/resources/pipe-attr-002.xml")
  }
}
