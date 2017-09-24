package com.xmlcalabash.test

import com.xmlcalabash.testers.XProcTestSpec

class RunTestSuiteSpec extends XProcTestSpec {
  runtests("Run the XProc test suite", "src/test/resources/test-suite/tests")
}
