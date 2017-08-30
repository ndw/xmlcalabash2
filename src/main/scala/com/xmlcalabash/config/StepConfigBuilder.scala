package com.xmlcalabash.config

import com.xmlcalabash.config.StepConfigParser.EventHandler
import com.xmlcalabash.exceptions.ParseException
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class StepConfigBuilder extends EventHandler {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var input: String = null
  private var step = Option.empty[StepConfig]
  private var port = Option.empty[PortConfig]
  private var opt = Option.empty[OptionConfig]
  private var stepHash = mutable.HashMap.empty[String,StepConfig]
  private var tokenList = mutable.ListBuffer.empty[String]
  private var primary = Option.empty[Boolean]
  private var prefixes = mutable.HashMap.empty[String,String]
  private var prefix = ""

  def steps: List[StepConfig] = stepHash.values.toList

  def reset(string: String) {
    input = string
  }

  def startNonterminal(name: String, begin: Int) {
    name match {
      case "Config" => Unit
      case "Prefix" => Unit
      case "Steps" => Unit
      case "Step" => Unit
      case "Implementation" => Unit
      case "Input" => Unit
      case "Output" => Unit
      case "Option" => Unit
      case "DeclaredType" => Unit
      case "TokenList" => Unit
      case _ => println(s"Unexpected NT: $name")
    }
  }

  def endNonterminal(name: String, end: Int) {
    name match {
      case "Config" => Unit
      case "Prefix" => Unit
      case "Steps" => Unit
      case "Step" =>
        if (stepHash.contains(step.get.name)) {
          logger.warn(s"Duplicate definition of ${step.get.name}")
        }
        stepHash.put(step.get.name, step.get)
      case "Implementation" => Unit
      case "Input" => step.get.addInputPort(port.get)
      case "Output" => step.get.addOutputPort(port.get)
      case "Option" => step.get.addOption(opt.get)
      case "TokenList" =>
        opt.get.tokenList = tokenList.toList
        tokenList.clear()
      case "DeclaredType" => Unit
      case _ => println("Unexpected /NT: " + name)
    }
  }

  def terminal(name: String, begin: Int, end: Int) {
    val tag = if (name(0) == '\'') "TOKEN" else name
    val text = characters(begin, end)

    if (tag == "TOKEN") {
      text match {
        case "*" => port.get.cardinality = "*"
        case "?" => opt.get.required = false
        case "=" => Unit
        case "prefix" => Unit
        case "step" => Unit
        case "primary" => primary = Some(true)
        case "input" => Unit
        case "output" => Unit
        case "option" => Unit
        case "as" => Unit
        case "has" => Unit
        case "implementation" => Unit
        case "of" => Unit
        case "(" => Unit
        case "|" => Unit
        case ")" => Unit
        case _ => println(s"Unexpected token: $text")
      }
    } else {
      name match {
        case "StepName" =>
          if (text.contains(":")) {
            val pos = text.indexOf(':')
            val pfx = text.substring(0, pos)
            val name = text.substring(pos+1)
            if (prefixes.contains(pfx)) {
              step = Some(new StepConfig(s"${prefixes(pfx)}$name"))
            } else {
              throw new ParseException(begin, s"Invalid prefix: $pfx:")
            }
          } else {
            step = Some(new StepConfig(text))
          }
        case "PortName" =>
          port = Some(new PortConfig(text))
          if (primary.isDefined) {
            port.get.primary = primary.get
            primary = None
          }
        case "OptionName" => opt = Some(new OptionConfig(text))
        case "TypeName" => opt.get.declaredType = text
        case "StringLiteral" => opt.get.defaultValue = text.substring(1, text.length - 1)
        case "Literal" => tokenList += text
        case "ClassName" =>
          if (text.contains(":")) {
            val pos = text.indexOf(':')
            val pfx = text.substring(0, pos)
            val name = text.substring(pos+1)
            if (prefixes.contains(pfx)) {
              step.get.implementation = s"${prefixes(pfx)}$name"
            } else {
              throw new ParseException(begin, s"Invalid prefix: $pfx:")
            }
          } else {
            step.get.implementation = text
          }
        case "Macro" => prefix = text
        case "Expansion" => prefixes.put(prefix, text.substring(1, text.length - 1))
        case "EOF" => Unit
        case _ => println("Unexpected T: " + name + ": " + text)
      }
    }
  }

  def whitespace(begin: Int, end: Int) {
    // nop
  }

  private def characters(begin: Int, end: Int): String = {
    if (begin < end) {
      input.substring(begin, end)
    } else {
      ""
    }
  }

}
