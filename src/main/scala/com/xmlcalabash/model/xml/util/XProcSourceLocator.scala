package com.xmlcalabash.model.xml.util

import net.sf.saxon.expr.parser.Location

/**
  * Created by ndw on 10/1/16.
  */
class XProcSourceLocator(val sysId: String, val pubId: String, val line: Int, val col: Int) extends Location {
  def this(sysId: String) {
    this(sysId, null, -1, -1)
  }
  def this(sysId: String, line: Int) {
    this(sysId, null, line, -1)
  }
  def this(sysId: String, line: Int, col: Int) {
    this(sysId, null, line, col)
  }

  override def getLineNumber: Int = line

  override def getColumnNumber: Int = col

  override def saveLocation(): Location = this

  override def getSystemId: String = sysId

  override def getPublicId: String = pubId
}
