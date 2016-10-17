package com.xmlcalabash.core

import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/1/16.
  */
object StepConstants {
  val _code = new QName("", "code")
  val _code_prefix = new QName("", "code-prefix")
  val _code_namespace = new QName("", "code-namespace")

  def xd(num: Int): QName = {
    new QName("err", XProcConstants.NS_XPROC_ERROR, f"XD$num%04d")
  }
}
