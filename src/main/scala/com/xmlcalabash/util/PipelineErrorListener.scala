package com.xmlcalabash.util

import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/16/16.
  */
trait PipelineErrorListener {
  def error(code: QName, loc: SourceLocation, msg: String)
  def error(code: QName, msg: String)
  def error(loc: SourceLocation, msg: String)
  def error(msg: String)
  def warn(code: QName, loc: SourceLocation, msg: String)
  def warn(code: QName, msg: String)
  def warn(loc: SourceLocation, msg: String)
  def warn(msg: String)
}
