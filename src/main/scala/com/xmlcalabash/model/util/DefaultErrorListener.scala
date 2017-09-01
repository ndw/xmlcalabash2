package com.xmlcalabash.model.util

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.ModelException

class DefaultErrorListener extends ErrorListener {
  override def error(message: String, location: Option[Location]): Unit = {
    msg("error", message, location)
  }

  override def error(cause: Throwable, location: Option[Location]): Unit = {
    msg("error", cause, location)
  }

  override def warning(message: String, location: Option[Location]): Unit = {
    msg("warn", message, location)
  }

  override def warning(cause: Throwable, location: Option[Location]): Unit = {
    msg("warn", cause, location)
  }

  override def info(message: String, location: Option[Location]): Unit = {
    msg("info", message, location)
  }

  private def msg(level: String, cause: Throwable, location: Option[Location]): Unit = {
    var loc = location
    var msg = ""

    cause match {
      case pe: PipelineException =>
        if (loc.isEmpty) {
          loc = pe.location
        }
        msg = pe.message
      case me: ModelException =>
        if (loc.isEmpty) {
          loc = me.location
        }
        msg = me.message
      case t: Throwable =>
        msg = t.getMessage
    }

    val sloc = loc.getOrElse("")
    println(s"$level:$sloc:$msg")
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
