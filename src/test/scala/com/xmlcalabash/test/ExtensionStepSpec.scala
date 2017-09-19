package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class ExtensionStepSpec extends XProcTestSpec {
  "A cx:property-extract " should " extract properties" in {
    test("src/test/resources/step-property-extract-001.xml")
  }

  "A cx:property-merge " should " merge properties" in {
    test("src/test/resources/step-property-merge-001.xml")
  }

  "A cx:property-merge " should " force a content type" in {
    test("src/test/resources/step-property-merge-002.xml")
  }
}
