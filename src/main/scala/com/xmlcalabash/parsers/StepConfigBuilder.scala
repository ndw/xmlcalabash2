package com.xmlcalabash.parsers

import java.io.InputStream

import com.jafpl.graph.Location
import com.xmlcalabash.config.{OptionSignature, PortSignature, Signatures, StepSignature}
import com.xmlcalabash.exceptions.ParseException
import com.xmlcalabash.model.util.DefaultLocation
import com.xmlcalabash.parsers.StepConfigParser.EventHandler
import net.sf.saxon.s9api.QName
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.io.Source

class StepConfigBuilder() extends EventHandler {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var input: String = _
  private var step = Option.empty[StepSignature]
  private var port = Option.empty[PortSignature]
  private var opt = Option.empty[OptionSignature]
  private var tokenList = mutable.ListBuffer.empty[String]
  private var primary = Option.empty[Boolean]
  private var prefixes = mutable.HashMap.empty[String,String]
  private var prefix = ""
  private var href = Option.empty[String]
  private var functionName = Option.empty[QName]
  private val cfgOffsets = ListBuffer.empty[Int]
  private var cfgOffset: Int = 0
  private var signatures: Signatures = _

  def parse(in: InputStream, href: Option[String]): Signatures = {
    input = null
    step = None
    port = None
    opt = None
    tokenList.clear()
    primary = None
    prefixes.clear()
    prefix = ""
    this.href = href
    cfgOffsets.clear()
    cfgOffset = 0
    signatures = new Signatures()

    val bufferedSource = Source.fromInputStream(in)
    var text = ""
    for (line <- bufferedSource.getLines) {
      cfgOffsets += cfgOffset
      cfgOffset += (line.length + 1)
      text += line + "\n"
    }
    bufferedSource.close

    val parser = new StepConfigParser(text, this)

    try {
      parser.parse
    } catch {
      case err: StepConfigParser.ParseException =>
        throw new ParseException("Syntax error", location(err.begin))
    }

    signatures
  }

  def parse(in: InputStream): Signatures = {
    parse(in, None)
  }

  def parse(in: InputStream, href: String): Signatures = {
    parse(in, Some(href))
  }

  def reset(string: String) {
    input = string
  }

  def startNonterminal(name: String, begin: Int) {
    //println(" nt " + name)

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
      case "Function" => Unit
      case "SeqType" => Unit
      case _ => println(s"Unexpected NT: $name")
    }
  }

  def endNonterminal(name: String, end: Int) {
    //println("/nt " + name)

    val loc = location(end)
    name match {
      case "Config" => Unit
      case "Prefix" => Unit
      case "Steps" => Unit
      case "Step" =>
        // Make sure ports are correctly marked as primary
        var port = Option.empty[PortSignature]
        var count = 0
        for (name <- step.get.inputPorts) {
          count += 1
          if (step.get.input(name, loc).primary) {
            port = Some(step.get.input(name, loc))
          } else if (port.isEmpty) {
            port = Some(step.get.input(name, loc))
          }
        }
        if (count == 1 && !port.get.primaryDeclared) {
          port.get.primary = true
        }

        port = None
        count = 0
        for (name <- step.get.outputPorts) {
          count += 1
          if (step.get.output(name, loc).primary) {
            port = Some(step.get.output(name, loc))
          } else if (port.isEmpty) {
            port = Some(step.get.output(name, loc))
          }
        }
        if (count == 1 && !port.get.primaryDeclared) {
          port.get.primary = true
        }

        if (step.get.implementation.isEmpty) {
          logger.debug("No implementation for step: " + step.get.stepType)
        }

       signatures.addStep(step.get)
      case "Implementation" => Unit
      case "Input" => step.get.addInput(port.get, location(end))
      case "Output" => step.get.addOutput(port.get, location(end))
      case "Option" => step.get.addOption(opt.get, location(end))
      case "TokenList" =>
        opt.get.tokenList = tokenList.toList
        tokenList.clear()
      case "DeclaredType" => Unit
      case "Function" =>
        if (!signatures.functions.contains(functionName.get)) {
          logger.debug("No implementation for extension function: " + functionName.get)
        }
        functionName = None
      case "SeqType" => Unit
      case _ => println("Unexpected /NT: " + name)
    }
  }

  def terminal(name: String, begin: Int, end: Int) {
    val tag = if (name(0) == '\'') "TOKEN" else name
    val text = characters(begin, end)

    //println(tag + ": " + text)

    if (tag == "TOKEN") {
      text match {
        case "*" => port.get.cardinality = "*"
        case "?" => opt.get.required = false
        case "=" => Unit
        case "prefix" => Unit
        case "step" => Unit
        case "function" => Unit
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
          val name = if (text.contains(":")) {
            val pfx = text.substring(0, text.indexOf(':'))
            parseClarkName(begin, pfx, expandedText(begin, text))
          } else {
            parseClarkName(begin, "", text)
          }
          step = Some(new StepSignature(name))
        case "PortName" =>
          port = Some(new PortSignature(text))
          if (primary.isDefined) {
            port.get.primary = primary.get
            primary = None
          }
        case "OptionName" =>
          val name = if (text.contains(":")) {
            val pfx = text.substring(0, text.indexOf(':'))
            parseClarkName(begin, pfx, expandedText(begin, text))
          } else {
            parseClarkName(begin, "", text)
          }
          opt = Some(new OptionSignature(name))
        case "TypeName" => opt.get.declaredType = text
        case "StringLiteral" => opt.get.defaultValue = text.substring(1, text.length - 1)
        case "Literal" => tokenList += text
        case "FunctionName" =>
          val name = if (text.contains(":")) {
            val pfx = text.substring(0, text.indexOf(':'))
            parseClarkName(begin, pfx, expandedText(begin, text))
          } else {
            parseClarkName(begin, "", text)
          }
          functionName = Some(name)
        case "ClassName" =>
          val name = if (text.contains(":")) {
            val pos = text.indexOf(':')
            val pfx = text.substring(0, pos)
            val name = text.substring(pos+1)
            if (prefixes.contains(pfx)) {
              s"${prefixes(pfx)}$name"
            } else {
              throw new ParseException(s"Invalid prefix: $pfx:", location(begin))
            }
          } else {
            text
          }
          if (functionName.isDefined) {
            signatures.addFunction(functionName.get, name)
          } else {
            step.get.implementation = name
          }
        case "Macro" => prefix = text
        case "Expansion" => prefixes.put(prefix, text.substring(1, text.length - 1))
        case "Occurrence" => opt.get.occurrence = text
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

  private def expandedText(begin: Int, text: String): String = {
    if (text.contains(":")) {
      val pos = text.indexOf(':')
      val pfx = text.substring(0, pos)
      val name = text.substring(pos+1)
      if (prefixes.contains(pfx)) {
        s"${prefixes(pfx)}$name"
      } else {
        throw new ParseException(s"Invalid prefix: $pfx:", location(begin))
      }
    } else {
      text
    }
  }

  private def parseClarkName(begin: Int, pfx: String, name: String): QName = {
    // FIXME: Better error handling for ClarkName parsing
    if (name.startsWith("{")) {
      val pos = name.indexOf("}")
      val uri = name.substring(1, pos)
      val local = name.substring(pos + 1)
      new QName(pfx, uri, local)
    } else {
      new QName("", name)
    }
  }

  private def location(pos: Int): Location = {
    // Scan forward until we pass the position or we run out of lines
    var line0 = 0
    while (cfgOffsets.length > line0 && cfgOffsets(line0) < pos) {
      line0 += 1
    }

    if (line0 < cfgOffsets.length) {
      // If we've passed the position, back up one line
      if (cfgOffsets(line0) > pos) {
        line0 -= 1
      }

      // The column is the position on this line
      val col0 = pos - cfgOffsets(line0)

      new DefaultLocation(href, (line0+1).toLong, (col0+1).toLong)
    } else {
      new DefaultLocation(href, -1, -1)
    }
  }
}
