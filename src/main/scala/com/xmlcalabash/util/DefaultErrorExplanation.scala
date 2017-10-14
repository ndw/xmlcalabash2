package com.xmlcalabash.util

import com.xmlcalabash.config.{ErrorExplanation, XMLCalabash}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import net.sf.saxon.s9api.QName

import scala.collection.mutable
import scala.io.Source

class DefaultErrorExplanation(config: XMLCalabash) extends ErrorExplanation {
  private val messages = mutable.HashMap.empty[QName, String]
  private val explain = mutable.HashMap.empty[QName, String]

  private var code = Option.empty[QName]
  private var message = ""
  private var explanation = ""
  private val stream = getClass.getResourceAsStream("/xproc-errors.txt")
  for (line <- Source.fromInputStream(stream).getLines) {
    if (line == "") {
      if (code.isDefined) {
        messages.put(code.get, message)
        explain.put(code.get, explanation)
        code = None
        message = ""
        explanation = ""
      }
    } else {
      if (code.isEmpty) {
        if (line.startsWith("{")) {
          code = Some(ValueParser.parseClarkName(line))
        } else {
          code = Some(new QName(XProcConstants.ns_err, line))
        }
      } else if (message == "") {
        message = line
      } else {
        explanation += line + "\n"
      }
    }
  }

  if (code.isDefined) {
    messages.put(code.get, message)
    explain.put(code.get, explanation)
  }

  override def message(code: QName): String = {
    message(code, List.empty[Any])
  }

  override def message(code: QName, details: List[Any]): String = {
    val detail = "^(.*)\\$(\\d+)(.*)$".r
    var message = messages.getOrElse(code, "[No explanatory message for " + code + "]")
    var matched = true
    while (matched) {
      matched = false
      message match {
        case detail(pre,detno,post) =>
          matched = true
          val detnum = detno.toInt - 1
          if (details.length > detnum) {
            message = pre + details(detnum) + post
          } else {
            message = pre + post
          }
        case _ =>
      }
    }
    message
  }

  override def explanation(code: QName): String = {
    explanation(code, List.empty[Any])
  }

  override def explanation(code: QName, details: List[Any]): String = {
    explain.getOrElse(code, "")
  }
}
