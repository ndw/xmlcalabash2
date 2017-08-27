package com.xmlcalabash.model.util

class DefaultErrorListener extends ErrorListener {
  override def error(message: String, location: Option[Location]): Unit = {
    msg("error", message, location)
  }

  override def error(cause: Throwable, location: Option[Location]): Unit = {
    msg("error", cause.getMessage, location)
  }

  override def warning(message: String, location: Option[Location]): Unit = {
    msg("warn", message, location)
  }

  override def warning(cause: Throwable, location: Option[Location]): Unit = {
    msg("warn", cause.getMessage, location)
  }

  override def info(message: String, location: Option[Location]): Unit = {
    msg("info", message, location)
  }

  private def msg(level: String, message: String, location: Option[Location]): Unit = {
    var prefix = ""

    print(level)
    prefix = ":"

    if (location.isDefined) {
      val loc = location.get
      if (loc.uri.isDefined) {
        print(loc.uri.get)
        prefix = ":"
      }
      if (loc.line.isDefined) {
        print(prefix, loc.line.get)
        prefix = ":"
      }
      if (loc.column.isDefined) {
        print(prefix, loc.column.get)
        prefix = ":"
      }
    }

    println(prefix, message)
  }
}
