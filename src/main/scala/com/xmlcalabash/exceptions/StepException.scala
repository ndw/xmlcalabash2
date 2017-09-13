package com.xmlcalabash.exceptions

import com.jafpl.graph.Location
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.ExpressionContext
import net.sf.saxon.s9api.QName

object StepException {
  def dynamicError(code: Int): StepException = {
    val qname = new QName("err", XProcConstants.ns_err, "D%04d".format(code))
    new StepException(qname)
  }
}

class StepException(val code: QName, val message: Option[String], val location: Option[Location]) extends Throwable {
  def this(code: QName) {
    this(code, None, None)
  }

  def this(code: QName, message: String) {
    this(code, Some(message), None)
  }

  def this(code: QName, message: String, location: Location) {
    this(code, Some(message), Some(location))
  }

  def this(code: QName, message: String, context: ExpressionContext) {
    this(code, Some(message), context.location)
  }

  def this(code: QName, context: ExpressionContext) {
    this(code, None, context.location)
  }

  override def toString: String = {
    var msg = "ERROR " + code

    if (location.isDefined) {
      msg += " " + location.get
    }

    if (message.isDefined) {
      msg += " " + message.get
    }

    msg
  }
}
