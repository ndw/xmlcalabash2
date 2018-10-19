package com.xmlcalabash.test

import com.xmlcalabash.util.XProcTestSpec

class RunTestSuiteSpec extends XProcTestSpec {
  runtests("Run the XProc test suite", "src/test/resources/test-suite/test-suite/tests")
}
