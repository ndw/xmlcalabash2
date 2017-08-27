package com.xmlcalabash.model.exceptions

class ModelException(val code: String, val message: String) extends Throwable {
  override def toString: String = {
    s"Exception('$code','$message')"
  }
}
