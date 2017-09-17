package com.xmlcalabash.exceptions

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Location
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.ExpressionContext
import net.sf.saxon.s9api.{QName, XdmNode}

object StepException {
  def dynamicError(code: Int): StepException = {
    val qname = new QName("err", XProcConstants.ns_err, "D%04d".format(code))
    new StepException(qname)
  }
}

class StepException(override val code: QName) extends PipelineException(code) {
  private var _errors = Option.empty[XdmNode]

  def this(code: QName, message: String) {
    this(code)
    _message = Some(message)
  }

  def this(code: QName, message: String, location: Location) {
    this(code)
    _message = Some(message)
    _location = Some(location)
  }

  def this(code: QName, message: String, location: Option[Location]) {
    this(code)
    _message = Some(message)
    _location = location
  }

  def this(code: QName, message: String, context: ExpressionContext) {
    this(code)
    _message = Some(message)
    _location = context.location
  }

  def this(code: QName, context: ExpressionContext, errors: XdmNode) {
    this(code)
    _location = context.location
    _errors = Some(errors)
  }

  def this(code: QName, message: String, cause: Throwable, location: Option[Location]) {
    this(code)
    _message = Some(message)
    _location = location
    _cause = Some(cause)
  }

  def errors: Option[XdmNode] = _errors

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
