package com.xmlcalabash.testers

import java.net.URI

import com.xmlcalabash.exceptions.ExceptionCode.ExceptionCode
import com.xmlcalabash.exceptions.{ModelException, StepException, XProcException}
import net.sf.saxon.s9api.QName

class TestResult(pass: Boolean) {
  private var _passed = pass
  private var _skipped = false
  private var _message = ""
  private var _baseURI = Option.empty[URI]
  private var _errQName = Option.empty[QName]
  private var _errCode = Option.empty[ExceptionCode]
  private var _except = Option.empty[Exception]

  def failed: Boolean = !passed

  def passed: Boolean = _passed

  def passed_=(pass: Boolean): Unit = {
    _passed = pass
  }

  def skipped: Boolean = _skipped

  def skipped_=(skip: Boolean): Unit = {
    _skipped = skip
  }

  def baseURI: Option[URI] = _baseURI

  def baseURI_=(base: URI): Unit = {
    _baseURI = Some(base)
  }

  def message: String = _message

  def errQName: Option[QName] = _errQName

  def errCode: Option[ExceptionCode] = _errCode

  def exception: Option[Throwable] = _except

  def this(pass: Boolean, msg: String) = {
    this(pass)
    _message = msg
  }

  def this(except: Exception) = {
    this(false, except.getMessage)
    _except = Some(XProcException.mapPipelineException(except))
    _except.get match {
      case model: ModelException =>
        _errQName = Some(model.exceptionQName)
        _errCode = Some(model.code)
      case xproc: XProcException =>
        _errQName = Some(xproc.code)
      case step: StepException =>
        _errQName = Some(step.code)
      case _ =>
        Unit
    }
  }

  override def toString: String = {
    message
  }
}
