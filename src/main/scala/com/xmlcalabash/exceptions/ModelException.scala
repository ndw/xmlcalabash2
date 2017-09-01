package com.xmlcalabash.exceptions

import com.jafpl.graph.Location

class ModelException(val code: String, val message: String, val location: Option[Location]) extends Throwable {
  override def toString: String = {
    s"Exception('$code','$message')"
  }
}
