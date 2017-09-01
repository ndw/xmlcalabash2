package com.xmlcalabash.model.util

import com.jafpl.graph.Location

class DefaultLocation(href: Option[String], lnum: Long, cnum: Option[Long]) extends Location {
  def this(lnum: Long) {
    this(None, lnum, None)
  }

  def this(lnum: Long, cnum: Long) {
    this(None, lnum, Some(cnum))
  }

  def this(href: String, lnum: Long, cnum: Long) {
    this(Some(href), lnum, Some(cnum))
  }

  def this(href: Option[String], lnum: Long, cnum: Long) {
    this(href, lnum, Some(cnum))
  }

  /** The URI */
  override def uri: Option[String] = href

  /** The line number. */
  override def line: Option[Long] = Some(lnum)

  /** The column number. */
  override def column: Option[Long] = cnum
}
