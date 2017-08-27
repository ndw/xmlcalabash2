package com.xmlcalabash.model.util

/** A location.
  *
  * This interface indentifies a location for use in subsequent reporting.
  *
  */
trait Location {
  /** The URI */
  def uri: Option[String]

  /** The line number. */
  def line: Option[Long]

  /** The column number. */
  def column: Option[Long]
}

