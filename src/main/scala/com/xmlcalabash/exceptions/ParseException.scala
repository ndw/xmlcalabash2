package com.xmlcalabash.exceptions

import com.xmlcalabash.model.util.Location

class ParseException(val message: String, val location: Option[Location]) extends Throwable {
  def this(msg: String, loc: Location) {
    this(msg, Some(loc))
  }

  override def toString: String = {
    if (location.isDefined) {
      s"ParseException('$message', ${location.get})"
    } else {
      s"ParseException('$message')"
    }
  }
}
