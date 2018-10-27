package com.xmlcalabash.util

import javax.xml.transform.{ErrorListener, TransformerException}
import org.xml.sax.{ErrorHandler, SAXParseException}

import scala.collection.mutable.ListBuffer

class CachingErrorListener() extends ErrorListener with ErrorHandler {
  private val _exceptions = ListBuffer.empty[Exception]
  private var _listener = Option.empty[ErrorListener]
  private var _handler = Option.empty[ErrorHandler]

  def this(listener: ErrorListener) = {
    this()
    _listener = Some(listener)
  }

  def this(handler: ErrorHandler) = {
    this()
    _handler = Some(handler)
  }

  def chainedListener: Option[ErrorListener] = _listener
  def chainedListener_=(listen: ErrorListener): Unit = {
    _listener = Some(listen)
  }

  def chainedHandler: Option[ErrorHandler] = _handler
  def chainedHandler_=(handler: ErrorHandler): Unit = {
    _handler = Some(handler)
  }

  def exceptions: List[Exception] = _exceptions.toList

  override def warning(exception: TransformerException): Unit = {
    if (_listener.isDefined) {
      _listener.get.warning(exception)
    }
    // I don't care about warnings; they won't stop the parse
  }

  override def error(exception: TransformerException): Unit = {
    if (_listener.isDefined) {
      _listener.get.error(exception)
    }
    _exceptions += exception
  }

  override def fatalError(exception: TransformerException): Unit = {
    if (_listener.isDefined) {
      _listener.get.fatalError(exception)
    }
    _exceptions += exception
  }

  override def warning(exception: SAXParseException): Unit = {
    if (_handler.isDefined) {
      _handler.get.warning(exception)
    }
    _exceptions += exception
  }

  override def error(exception: SAXParseException): Unit = {
    if (_handler.isDefined) {
      _handler.get.error(exception)
    }
    _exceptions += exception
  }

  override def fatalError(exception: SAXParseException): Unit = {
    if (_handler.isDefined) {
      _handler.get.fatalError(exception)
    }
    _exceptions += exception
  }
}
