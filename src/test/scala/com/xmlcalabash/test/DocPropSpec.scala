package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class DocPropSpec extends XProcTestSpec {
  "Document properties from an inline XML document" should " work" in {
    test("src/test/resources/doc-prop-001.xml")
  }

  "Document properties from an XML document" should " work" in {
    test("src/test/resources/doc-prop-002.xml")
  }

  "Document properties from a non-XML document" should " work" in {
    test("src/test/resources/doc-prop-003.xml")
  }
  /*
  // FIXME: when did I break this?
  "Document properties from a variable" should " work" in {
    test("src/test/resources/doc-prop-004.xml")
  }
  */
  "Document properties from a non-document variable" should " fail" in {
    test("src/test/resources/doc-prop-005.xml")
  }
}
