package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class WithPropertiesSpec extends XProcTestSpec {
  "A p:with-properties " should " modify properties" in {
    test("src/test/resources/with-prop-001.xml")
  }

}
