package com.xmlcalabash.util

import com.xmlcalabash.config.{ErrorExplanation, XMLCalabashConfig}
import com.xmlcalabash.model.util.{ValueParser, XProcConstants}
import net.sf.saxon.s9api.QName

import scala.collection.mutable.ListBuffer
import scala.io.Source

class DefaultErrorExplanation(config: XMLCalabashConfig) extends ErrorExplanation {
  private val messages = ListBuffer.empty[ErrorExplanationTemplate]

  private var code = Option.empty[QName]
  private var message = ""
  private var explanation = ""
  private val stream = getClass.getResourceAsStream("/xproc-errors.txt")
  for (line <- Source.fromInputStream(stream).getLines) {
    if (line == "") {
      if (code.isDefined) {
        messages += new ErrorExplanationTemplate(code.get, message, explanation)
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
    messages += new ErrorExplanationTemplate(code.get, message, explanation)
  }

  override def message(code: QName): String = {
    message(code, List.empty[Any])
  }

  override def message(code: QName, details: List[Any]): String = {
    var message = template(code, details.length).message
    substitute(message, details)
  }

  override def explanation(code: QName): String = {
    explanation(code, List.empty[Any])
  }

  override def explanation(code: QName, details: List[Any]): String = {
    var message = template(code, details.length).explanation
    substitute(message, details)
  }

  private def template(code: QName, count: Integer): ErrorExplanationTemplate = {
    // Find all the messages with a matching code, with a cardinality <= details.length
    val templates = messages.filter(_.code == code).filter(_.cardinality <= count)

    if (templates.isEmpty) {
      // Return a default template
      new ErrorExplanationTemplate(code, "[No explanatory message for " + code + "]", "")
    } else {
      // Return the one with the highest cardinality.
      templates.maxBy(_.cardinality)
    }
  }

  private def substitute(text: String, details: List[Any]): String = {
    var message = text
    val detail = "^(.*?)\\$(\\d+)(.*)$".r
    var matched = true

    while (matched) {
      matched = false
      message match {
        case detail(pre,detno,post) =>
          matched = true
          val detnum = detno.toInt - 1
          if (details.length > detnum) {
            message = pre + stringify(details(detnum)) + post
          } else {
            message = pre + post
          }
        case _ =>
      }
    }

    message
  }

  private def stringify(any: Any): String = {
    any match {
      case list: List[Any] =>
        var str = "["
        var sep = ""
        for (item <- list) {
          str = str + sep + item.toString
          sep = ", "
        }
        str = str + "]"
        str
      case _ => any.toString
    }
  }

  private class ErrorExplanationTemplate(val code: QName, val message: String, val explanation: String) {
    def cardinality: Int = message.count(_=='$')
  }

}
