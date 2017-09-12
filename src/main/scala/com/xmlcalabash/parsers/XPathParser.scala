package com.xmlcalabash.parsers

import com.xmlcalabash.config.XMLCalabash
import com.xmlcalabash.model.util.{ExpressionParser, ParserConfiguration}
import com.xmlcalabash.parsers.XPath31.EventHandler
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class XPathParser(config: XMLCalabash) extends ExpressionParser {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val handler = new FindRefs()
  private val parser = new XPath31()
  private var _errors = false

  def parse(expr: String): Unit = {
    handler.initialize()
    parser.initialize(expr, handler)
    try {
      parser.parse_XPath
    } catch {
      case e: Throwable =>
        _errors = true
    }
  }

  def errors: Boolean = _errors

  def variableRefs: List[String] = {
    handler.variableRefs()
  }

  def functionRefs: List[String] = {
    handler.functionRefs()
  }

  class FindRefs extends EventHandler {
    private var input: String = null
    private val varlist = mutable.ListBuffer.empty[String]
    private val funclist = mutable.ListBuffer.empty[String]
    private var sawDollar = false
    // Simple switches won't work if they can nest, but I don't think they can...
    private var functionCall = false
    private var functionName = false

    def initialize(): Unit = {
      input = null
      varlist.clear()
      funclist.clear()
      sawDollar = false
      functionCall = false
      functionName = false
    }

    def variableRefs(): List[String] = {
      varlist.toList
    }

    def functionRefs(): List[String] = {
      funclist.toList
    }

    override def reset(string: String): Unit = {
      input = string
    }

    override def startNonterminal(name: String, begin: Int): Unit = {
      if (config.traceEventManager.traceEnabled("XPathParser")) {
        logger.debug("XPathParser:  NT: {}", name)
      }
      if (name == "FunctionCall") {
        functionCall = true
      }
      if (name == "FunctionName") {
        functionName = true
      }
    }

    override def endNonterminal(name: String, end: Int): Unit = {
      if (config.traceEventManager.traceEnabled("XPathParser")) {
        logger.debug("XPathParser: /NT: {}", name)
      }
      if (name == "FunctionCall") {
        functionCall = false
      }
      if (name == "FunctionName") {
        functionName = false
      }
    }

    override def terminal(name: String, begin: Int, end: Int): Unit = {
      if (config.traceEventManager.traceEnabled("XPathParser")) {
        logger.debug(s"XPathParser:   T: $name: ${characters(begin,end)}")
      }
      if (sawDollar) {
        varlist += characters(begin, end)
      } else {
        if (functionCall && functionName) {
          funclist += characters(begin, end)
        }
      }
      sawDollar = name == "'$'"
    }

    override def whitespace(begin: Int, end: Int): Unit = {
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
}
