package com.xmlcalabash.exceptions

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Location
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.ExpressionContext
import net.sf.saxon.s9api.{QName, XdmNode}

object StepException {
  def dynamicError(code: Int): XProcException = {
    dynamicError(code, List.empty[String], None)
  }
  def dynamicError(code: Int, details: String): XProcException = {
    dynamicError(code, List(details), None)
  }
  def dynamicError(code: Int, details: List[String]): XProcException = {
    dynamicError(code, details, None)
  }
  def dynamicError(code: Int, location: Option[Location]): XProcException = {
    dynamicError(code, List.empty[String], location)
  }
  def dynamicError(code: Int, details: String, location: Option[Location]): XProcException = {
    dynamicError(code, List(details), location)
  }
  def dynamicError(code: Int, details: List[String], location: Option[Location]): XProcException = {
    XProcException.dynamicError(code, details, location)
  }

  def staticError(code: Int): XProcException = {
    staticError(code, List.empty[String], None)
  }
  def staticError(code: Int, details: String): XProcException = {
    staticError(code, List(details), None)
  }
  def staticError(code: Int, details: List[String]): XProcException = {
    staticError(code, details, None)
  }
  def staticError(code: Int, location: Option[Location]): XProcException = {
    staticError(code, List.empty[String], location)
  }
  def staticError(code: Int, details: String, location: Option[Location]): XProcException = {
    staticError(code, List(details), location)
  }
  def staticError(code: Int, details: List[String], location: Option[Location]): XProcException = {
    XProcException.staticError(code, details, location)
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
