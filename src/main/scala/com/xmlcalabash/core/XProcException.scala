package com.xmlcalabash.core

import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/1/16.
  */
class XProcException(val err: Option[QName], val msg: String, val throwable: Option[Throwable]) extends RuntimeException {
  def this(msg: String) {
    this(None, msg, None)
  }

  def this(err: Option[QName], msg: String) {
    this(err, msg, None)
  }
  def this(throwable: Throwable) {
    this(None, throwable.getCause.getMessage, Some(throwable))
  }
}
