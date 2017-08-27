package com.xmlcalabash.model.util

trait ErrorListener {
  def error(message: String, location: Option[Location])
  def error(cause: Throwable, location: Option[Location])
  def warning(message: String, location: Option[Location])
  def warning(cause: Throwable, location: Option[Location])
  def info(message: String, location: Option[Location])
}
