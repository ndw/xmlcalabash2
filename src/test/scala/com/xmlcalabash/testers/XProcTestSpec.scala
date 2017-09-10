package com.xmlcalabash.testers

import com.xmlcalabash.config.XMLCalabash
import org.scalatest.FlatSpec

class XProcTestSpec extends FlatSpec {
  protected val runtimeConfig = XMLCalabash.newInstance()
  protected var error = Option.empty[String]

  def test(fn: String) {
    val runner = new TestRunner(runtimeConfig, fn)
    try {
      error = runner.run()
    } catch {
      case t: Throwable =>
        println(t)
        throw t
    }
    if (error.isDefined) {
      println(error.toString)
    }
    assert(error.isEmpty)
  }

}
