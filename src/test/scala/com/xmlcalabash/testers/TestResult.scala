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
  private var _except = Option.empty[Throwable]

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

  def this(model: ModelException) = {
    this(false, model.message)
    _except = Some(model)
    _errQName = Some(model.exceptionQName)
    _errCode = Some(model.code)
  }

  def this(xproc: XProcException) = {
    this(false, xproc.message.getOrElse(""))
    _except = Some(xproc)
    _errQName = Some(xproc.code)
  }

  def this(step: StepException) = {
    this(false, step.message.getOrElse(""))
    _except = Some(step)
    _errQName = Some(step.code)
  }

  def this(thrown: Throwable) = {
    this(false, thrown.getMessage)
    _except = Some(thrown)
  }

  override def toString(): String = {
    message
  }
}
