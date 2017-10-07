package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class RunSpecificTestSpec extends XProcTestSpec {
  runtests("Run the extensions test suite", "src/test/resources/test-suite/test-suite/tests/with-input-select-004.xml")
}
