package com.xmlcalabash.exceptions

class TestException(val code: String, val message: String) extends Throwable {
  def this(msg: String) {
    this("ERROR", msg)
  }

  override def getMessage(): String = {
    message
  }

  override def toString: String = {
    "{" + code + ":" + message + "}"
  }
}
