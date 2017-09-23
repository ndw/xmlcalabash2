package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class CollectionAttrSpec extends XProcTestSpec {
  "A p:variable " should " support collection=true" in {
    test("src/test/resources/collection-attr-001.xml")
  }

  "A p:variable " should " fail without collection=true" in {
    test("src/test/resources/collection-attr-002.xml")
  }

  "A p:with-option " should " fail without collection=true" in {
    test("src/test/resources/collection-attr-003.xml")
  }
}
