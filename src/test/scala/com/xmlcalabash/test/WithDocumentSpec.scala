package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class WithDocumentSpec extends XProcTestSpec {
  "A p:with-document " should " preserve properties" in {
    test("src/test/resources/with-doc-001.xml")
  }

}
