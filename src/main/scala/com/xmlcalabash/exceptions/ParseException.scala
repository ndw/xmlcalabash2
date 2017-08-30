package com.xmlcalabash.exceptions

class ParseException(val begin: Int, val message: String) extends Throwable {
  override def toString: String = {
    s"ParseException('$begin','$message')"
  }
}
