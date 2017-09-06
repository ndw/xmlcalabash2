package com.xmlcalabash.parsers

// This file was generated on Tue Aug 29, 2017 19:50 (UTC-05) by REx v5.45 which is Copyright (c) 1979-2017 by Gunther Rademacher <grd@gmx.net>
// REx command line: StepConfigParser.ebnf -ll 2 -scala -tree

import scala.collection.mutable.ArrayBuffer

class StepConfigParser {

  def this(string: String, eh: StepConfigParser.EventHandler) {
    this
    initialize(string, eh)
  }

  def initialize(string: String, eh: StepConfigParser.EventHandler) {
    eventHandler = eh
    input = string
    size = input.length
    reset(0, 0, 0)
  }

  def reset(l: Int, b: Int, e: Int) {
    b0 = b; e0 = b
    l1 = l; b1 = b; e1 = e
    l2 = 0
    end = e
    eventHandler.reset(input)
  }

  def reset {
    reset(0, 0, 0)
  }

  def parse {
    eventHandler.startNonterminal("Config", e0)
    var c1 = true
    while (c1) {
      lookahead1W(19)               // Comment | WhiteSpace | EOF | 'function' | 'prefix' | 'step'
      if (l1 != 27) {               // 'prefix'
        c1 = false
      }
      else {
        whitespace
        parse_Prefix
      }
    }
    var c2 = true
    while (c2) {
      if (l1 == 4) {                // EOF
        c2 = false
      }
      else {
        l1 match {
          case 29 =>                  // 'step'
            whitespace
            parse_Step
          case _ =>
            whitespace
            parse_Function
        }
      }
    }
    consume(4)                      // EOF
    eventHandler.endNonterminal("Config", e0)
  }

  private def parse_Prefix {
    eventHandler.startNonterminal("Prefix", e0)
    consume(27)                     // 'prefix'
    lookahead1W(7)                  // Comment | WhiteSpace | Macro
    consume(11)                     // Macro
    lookahead1W(11)                 // Comment | WhiteSpace | '='
    consume(17)                     // '='
    lookahead1W(9)                  // Comment | WhiteSpace | Expansion
    consume(13)                     // Expansion
    eventHandler.endNonterminal("Prefix", e0)
  }

  private def parse_Step {
    eventHandler.startNonterminal("Step", e0)
    consume(29)                     // 'step'
    lookahead1W(2)                  // Comment | WhiteSpace | StepName
    consume(6)                      // StepName
    var c1 = true
    while (c1) {
      lookahead1W(27)               // Comment | WhiteSpace | EOF | 'function' | 'has' | 'input' | 'option' | 'output' |
      // 'primary' | 'step'
      if (l1 != 21) {               // 'has'
        c1 = false
      }
      else {
        whitespace
        parse_Implementation
      }
    }
    var c2 = true
    while (c2) {
      lookahead1W(24)               // Comment | WhiteSpace | EOF | 'function' | 'input' | 'option' | 'output' |
      // 'primary' | 'step'
      l1 match {
        case 28 =>                    // 'primary'
          lookahead2W(16)             // Comment | WhiteSpace | 'input' | 'output'
        case _ =>
          lk = l1
      }
      if (lk != 23                  // 'input'
        && lk != 1500) {             // 'primary' 'input'
        c2 = false
      }
      else {
        whitespace
        parse_Input
      }
    }
    var c3 = true
    while (c3) {
      lookahead1W(21)               // Comment | WhiteSpace | EOF | 'function' | 'option' | 'output' | 'primary' |
      // 'step'
      if (l1 != 26                  // 'output'
        && l1 != 28) {               // 'primary'
        c3 = false
      }
      else {
        whitespace
        parse_Output
      }
    }
    var c4 = true
    while (c4) {
      lookahead1W(18)               // Comment | WhiteSpace | EOF | 'function' | 'option' | 'step'
      if (l1 != 25) {               // 'option'
        c4 = false
      }
      else {
        whitespace
        parse_Option
      }
    }
    eventHandler.endNonterminal("Step", e0)
  }

  private def parse_Function {
    eventHandler.startNonterminal("Function", e0)
    consume(20)                     // 'function'
    lookahead1W(8)                  // Comment | WhiteSpace | FunctionName
    consume(12)                     // FunctionName
    var c1 = true
    while (c1) {
      lookahead1W(17)               // Comment | WhiteSpace | EOF | 'function' | 'has' | 'step'
      if (l1 != 21) {               // 'has'
        c1 = false
      }
      else {
        whitespace
        parse_Implementation
      }
    }
    eventHandler.endNonterminal("Function", e0)
  }

  private def parse_Implementation {
    eventHandler.startNonterminal("Implementation", e0)
    consume(21)                     // 'has'
    lookahead1W(12)                 // Comment | WhiteSpace | 'implementation'
    consume(22)                     // 'implementation'
    lookahead1W(4)                  // Comment | WhiteSpace | ClassName
    consume(8)                      // ClassName
    eventHandler.endNonterminal("Implementation", e0)
  }

  private def parse_Input {
    eventHandler.startNonterminal("Input", e0)
    if (l1 == 28) {                 // 'primary'
      consume(28)                   // 'primary'
    }
    lookahead1W(13)                 // Comment | WhiteSpace | 'input'
    consume(23)                     // 'input'
    lookahead1W(1)                  // Comment | WhiteSpace | PortName
    consume(5)                      // PortName
    lookahead1W(25)                 // Comment | WhiteSpace | EOF | '*' | 'function' | 'input' | 'option' | 'output' |
    // 'primary' | 'step'
    if (l1 == 16) {                 // '*'
      consume(16)                   // '*'
    }
    eventHandler.endNonterminal("Input", e0)
  }

  private def parse_Output {
    eventHandler.startNonterminal("Output", e0)
    if (l1 == 28) {                 // 'primary'
      consume(28)                   // 'primary'
    }
    lookahead1W(14)                 // Comment | WhiteSpace | 'output'
    consume(26)                     // 'output'
    lookahead1W(1)                  // Comment | WhiteSpace | PortName
    consume(5)                      // PortName
    lookahead1W(22)                 // Comment | WhiteSpace | EOF | '*' | 'function' | 'option' | 'output' | 'primary' |
    // 'step'
    if (l1 == 16) {                 // '*'
      consume(16)                   // '*'
    }
    eventHandler.endNonterminal("Output", e0)
  }

  private def parse_Option {
    eventHandler.startNonterminal("Option", e0)
    consume(25)                     // 'option'
    lookahead1W(3)                  // Comment | WhiteSpace | OptionName
    consume(7)                      // OptionName
    lookahead1W(26)                 // Comment | WhiteSpace | EOF | '=' | '?' | 'as' | 'function' | 'of' | 'option' |
    // 'step'
    if (l1 == 18) {                 // '?'
      consume(18)                   // '?'
    }
    lookahead1W(23)                 // Comment | WhiteSpace | EOF | '=' | 'as' | 'function' | 'of' | 'option' | 'step'
    if (l1 == 19                    // 'as'
      || l1 == 24) {                 // 'of'
      whitespace
      parse_DeclaredType
    }
    lookahead1W(20)                 // Comment | WhiteSpace | EOF | '=' | 'function' | 'option' | 'step'
    if (l1 == 17) {                 // '='
      consume(17)                   // '='
      lookahead1W(0)                // Comment | WhiteSpace | StringLiteral
      consume(3)                    // StringLiteral
    }
    eventHandler.endNonterminal("Option", e0)
  }

  private def parse_DeclaredType {
    eventHandler.startNonterminal("DeclaredType", e0)
    l1 match {
      case 19 =>                      // 'as'
        consume(19)                   // 'as'
        lookahead1W(5)                // Comment | WhiteSpace | TypeName
        consume(9)                    // TypeName
      case _ =>
        consume(24)                   // 'of'
        lookahead1W(10)               // Comment | WhiteSpace | '('
        whitespace
        parse_TokenList
    }
    eventHandler.endNonterminal("DeclaredType", e0)
  }

  private def parse_TokenList {
    eventHandler.startNonterminal("TokenList", e0)
    consume(14)                     // '('
    lookahead1W(6)                  // Comment | WhiteSpace | Literal
    consume(10)                     // Literal
    var c1 = true
    while (c1) {
      lookahead1W(15)               // Comment | WhiteSpace | ')' | '|'
      if (l1 != 30) {               // '|'
        c1 = false
      }
      else {
        consume(30)                 // '|'
        lookahead1W(6)              // Comment | WhiteSpace | Literal
        consume(10)                 // Literal
      }
    }
    consume(15)                     // ')'
    eventHandler.endNonterminal("TokenList", e0)
  }

  private def consume(t: Int) {
    if (l1 == t) {
      whitespace
      eventHandler.terminal(StepConfigParser.TOKEN(l1), b1, e1)
      b0 = b1; e0 = e1; l1 = l2; if (l1 != 0) {
        b1 = b2; e1 = e2; l2 = 0 }
    }
    else {
      error(b1, e1, 0, l1, t)
    }
  }

  private def whitespace {
    if (e0 != b1) {
      eventHandler.whitespace(e0, b1)
      e0 = b1
    }
  }

  private def matchW(set: Int): Int =  {
    var continue = true
    var code = 0
    while (continue) {
      code = matcher(set)
      if (code != 1                 // Comment
        && code != 2) {              // WhiteSpace
        continue = false
      }
    }
    code
  }

  private def lookahead1W(set: Int) {
    if (l1 == 0) {
      l1 = matchW(set)
      b1 = begin
      e1 = end
    }
  }

  private def lookahead2W(set: Int) {
    if (l2 == 0) {
      l2 = matchW(set)
      b2 = begin
      e2 = end
    }
    lk = (l2 << 6) | l1
  }

  def getErrorMessage(e: StepConfigParser.ParseException) = {
    val tokenSet = StepConfigParser.getExpectedTokenSet(e)
    val found = StepConfigParser.getOffendingToken(e)
    val prefix = input.substring(0, e.begin)
    val line = prefix.replaceAll("[^\n]", "").length + 1
    val column = prefix.length - prefix.lastIndexOf('\n')
    val size = e.end - e.begin
    e.getMessage + (if (found == null) "" else ", found " + found) + "\nwhile expecting " +
      (if (tokenSet.length == 1) tokenSet(0) else "[" + (tokenSet mkString ", ") + "]") + "\n" +
      (if (size == 0 || found != null) "" else "after successfully scanning " + size + " characters beginning ") +
      "at line " + line + ", column " + column + ":\n..." +
      input.substring(e.begin, math.min(input.length, e.begin + 64)) + "..."
  }

  def error(b: Int, e: Int, s: Int, l: Int, t: Int): Int = {
    throw new StepConfigParser.ParseException(b, e, s, l, t)
  }

  private def matcher(tokenSetId: Int) = {
    begin = end
    var current = end
    var result = StepConfigParser.INITIAL(tokenSetId)
    var state = 0
    var code = result & 127

    while (code != 0) {
      var charclass = -1
      var c0 = if (current < size) input(current) else 0
      current += 1
      if (c0 < 0x80) {
        charclass = StepConfigParser.MAP0(c0)
      }
      else if (c0 < 0xd800) {
        val c1 = c0 >> 4
        charclass = StepConfigParser.MAP1((c0 & 15) + StepConfigParser.MAP1((c1 & 31) + StepConfigParser.MAP1(c1 >> 5)))
      }
      else {
        if (c0 < 0xdc00) {
          val c1 = if (current < size) input(current) else 0
          if (c1 >= 0xdc00 && c1 < 0xe000) {
            current += 1
            c0 = ((c0 & 0x3ff) << 10) + (c1 & 0x3ff) + 0x10000
          }
        }

        var lo = 0
        var hi = 5
        var m = 3
        while (charclass < 0) {
          if (StepConfigParser.MAP2(m) > c0) hi = m - 1
          else if (StepConfigParser.MAP2(6 + m) < c0) lo = m + 1
          else charclass = StepConfigParser.MAP2(12 + m)
          if (lo > hi) charclass = 0 else m = (hi + lo) >> 1
        }
      }

      state = code
      val i0 = (charclass << 7) + code - 1
      code = StepConfigParser.TRANSITION((i0 & 7) + StepConfigParser.TRANSITION(i0 >> 3))

      if (code > 127) {
        result = code
        code &= 127
        end = current
      }
    }

    result >>= 7
    if (result == 0) {
      end = current - 1
      val c1 = if (end < size) input(end) else 0
      if (c1 >= 0xdc00 && c1 < 0xe000) {
        end -= 1
      }
      error(begin, end, state, -1, -1)
    }
    else {
      if (end > size) end = size
      (result & 31) - 1
    }
  }

  var input: String = null
  var size = 0
  var begin = 0
  var end = 0
  var lk = 0
  var b0 = 0
  var e0 = 0
  var l1 = 0
  var b1 = 0
  var e1 = 0
  var l2 = 0
  var b2 = 0
  var e2 = 0
  var eventHandler: StepConfigParser.EventHandler = null
}

object StepConfigParser {

  def getOffendingToken(e: ParseException) = {
    if (e.offending < 0) null else TOKEN(e.offending)
  }

  class ParseException(val begin: Int, val end: Int, val state: Int, val offending: Int, val expected: Int) extends RuntimeException {
    override def getMessage = {
      if (offending < 0) "lexical analysis failed" else "syntax error"
    }
  }

  def getExpectedTokenSet(e: ParseException) = {
    if (e.expected < 0) {
      getTokenSet(- e.state)
    }
    else {
      Array(TOKEN(e.expected))
    }
  }

  trait EventHandler {
    def reset(string: String)
    def startNonterminal(name: String, begin: Int)
    def endNonterminal(name: String, end: Int)
    def terminal(name: String, begin: Int, end: Int)
    def whitespace(begin: Int, end: Int)
  }

  class TopDownTreeBuilder extends EventHandler {
    private var input: String = null
    private var stack = new ArrayBuffer[Nonterminal](64)
    private var top = -1

    override def reset(input: String) {
      this.input = input
      top = -1
    }

    override def startNonterminal(name: String, begin: Int) {
      val nonterminal = new Nonterminal(name, begin, begin, new ArrayBuffer[Symbol])
      if (top >= 0) addChild(nonterminal)
      top += 1
      if (top == stack.length) stack += nonterminal else stack(top) = nonterminal
    }

    override def endNonterminal(name: String, end: Int) {
      var nonterminal = stack(top)
      nonterminal.end = end
      if (top > 0) top -= 1
    }

    override def terminal(name: String, begin: Int, end: Int) {
      addChild(new Terminal(name, begin, end))
    }

    override def whitespace(begin: Int, end: Int) {
    }

    private def addChild(s: Symbol) {
      var current = stack(top)
      current.children += s
    }

    def serialize(e: EventHandler) {
      e.reset(input)
      stack(0).send(e)
    }
  }

  abstract class Symbol(n: String, b: Int, e: Int) {
    var name = n
    var begin = b
    var end = e

    def send(e: EventHandler)
  }

  class Terminal(name: String, begin: Int, end: Int) extends Symbol(name, begin, end) {
    override def send(e: EventHandler) {
      e.terminal(name, begin, end)
    }
  }

  class Nonterminal(name: String, begin: Int, end: Int, c: ArrayBuffer[Symbol]) extends Symbol(name, begin, end) {
    var children = c

    override def send(e: EventHandler) {
      e.startNonterminal(name, begin)
      var pos = begin
      for (c <- children) {
        if (pos < c.begin) e.whitespace(pos, c.begin)
        c.send(e)
        pos = c.end
      }
      if (pos < end) e.whitespace(pos, end)
      e.endNonterminal(name, end)
    }
  }

  private def getTokenSet(tokenSetId: Int) = {
    var expected = new ArrayBuffer[String]
    val s = if (tokenSetId < 0) - tokenSetId else INITIAL(tokenSetId) & 127
    var i = 0
    while (i < 31) {
      var j = i
      val i0 = (i >> 5) * 105 + s - 1
      var f = EXPECTED(i0)
      while (f != 0) {
        if ((f & 1) != 0) {
          expected += TOKEN(j)
        }
        f >>>= 1
        j += 1
      }
      i += 32
    }
    expected.toArray
  }

  private final val MAP0 = Array(
    /*   0 */ 36, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7,
    /*  35 */ 6, 6, 6, 6, 8, 9, 10, 11, 6, 6, 12, 12, 13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 14, 6, 6, 15, 6, 16,
    /*  64 */ 6, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
    /*  91 */ 6, 6, 6, 6, 17, 6, 18, 17, 19, 17, 20, 21, 17, 22, 23, 17, 17, 24, 25, 26, 27, 28, 17, 29, 30, 31, 32, 17,
    /* 119 */ 17, 33, 34, 17, 6, 35, 6, 6, 6
  )

  private final val MAP1 = Array(
    /*   0 */ 108, 124, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 156, 181, 181, 181, 181,
    /*  21 */ 181, 214, 215, 213, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  42 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  63 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /*  84 */ 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214,
    /* 105 */ 214, 214, 214, 247, 261, 277, 415, 322, 293, 339, 355, 392, 392, 392, 384, 323, 315, 323, 315, 323, 323,
    /* 126 */ 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 409, 409, 409, 409, 409, 409, 409,
    /* 147 */ 308, 323, 323, 323, 323, 323, 323, 323, 323, 368, 392, 392, 393, 391, 392, 392, 323, 323, 323, 323, 323,
    /* 168 */ 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 392, 392, 392, 392, 392, 392, 392, 392,
    /* 189 */ 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392, 392,
    /* 210 */ 392, 392, 392, 322, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323,
    /* 231 */ 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 392, 36, 1, 1, 1, 1, 1, 1, 1,
    /* 255 */ 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7, 6, 6, 6, 6, 8, 9, 10, 11, 6, 6,
    /* 290 */ 12, 12, 13, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 6, 6, 6, 6, 17, 17, 17, 17, 17, 17, 17, 17, 17,
    /* 317 */ 17, 17, 17, 17, 17, 6, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 6, 18, 17, 19, 17,
    /* 344 */ 20, 21, 17, 22, 23, 17, 17, 24, 25, 26, 27, 28, 17, 29, 30, 31, 32, 17, 17, 33, 34, 17, 6, 35, 6, 6, 6, 6,
    /* 372 */ 6, 6, 6, 6, 6, 6, 6, 6, 17, 17, 6, 6, 6, 6, 6, 6, 6, 6, 6, 12, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    /* 406 */ 6, 6, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 14, 6, 6, 15, 6, 16
  )

  private final val MAP2 = Array(
    /*  0 */ 57344, 63744, 64976, 65008, 65536, 983040, 63743, 64975, 65007, 65533, 983039, 1114111, 6, 17, 6, 17, 17,
    /* 17 */ 6
  )

  private final val INITIAL = Array(
    /*  0 */ 1, 2, 899, 4, 1157, 6, 7, 8, 1673, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
    /* 27 */ 28
  )

  private final val TRANSITION = Array(
    /*    0 */ 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915, 915,
    /*   21 */ 915, 915, 1161, 915, 914, 915, 915, 915, 915, 915, 915, 592, 592, 592, 603, 915, 1166, 915, 1161, 915,
    /*   41 */ 914, 915, 915, 915, 915, 915, 915, 592, 592, 592, 595, 915, 1166, 915, 911, 915, 914, 915, 915, 915, 915,
    /*   62 */ 915, 915, 592, 592, 592, 618, 915, 915, 915, 1161, 915, 914, 915, 915, 915, 915, 915, 915, 630, 630, 630,
    /*   83 */ 634, 915, 1166, 915, 911, 915, 914, 915, 915, 915, 915, 915, 915, 915, 915, 915, 1100, 915, 1166, 915,
    /*  103 */ 1161, 915, 914, 915, 915, 915, 915, 915, 915, 642, 868, 915, 927, 915, 790, 915, 840, 915, 914, 915, 915,
    /*  124 */ 915, 915, 915, 915, 609, 851, 915, 811, 915, 1171, 915, 846, 915, 914, 915, 915, 915, 915, 915, 915, 915,
    /*  145 */ 1097, 915, 1100, 915, 1166, 915, 1161, 915, 914, 915, 915, 915, 915, 915, 915, 915, 705, 915, 1100, 915,
    /*  165 */ 1166, 915, 1161, 915, 914, 915, 915, 915, 915, 915, 915, 915, 915, 777, 651, 915, 1166, 915, 906, 915,
    /*  185 */ 666, 915, 915, 915, 915, 915, 915, 1156, 675, 915, 1100, 684, 720, 915, 1161, 915, 914, 915, 915, 915,
    /*  205 */ 915, 915, 915, 692, 692, 692, 696, 915, 1166, 915, 1161, 915, 704, 915, 915, 915, 915, 915, 915, 915,
    /*  225 */ 915, 915, 1100, 895, 901, 915, 1161, 915, 914, 915, 915, 915, 915, 915, 915, 915, 971, 1054, 1122, 915,
    /*  245 */ 1166, 915, 1161, 915, 914, 915, 915, 915, 915, 915, 915, 915, 915, 915, 1136, 915, 1166, 915, 1161, 915,
    /*  265 */ 914, 915, 915, 915, 915, 915, 915, 713, 738, 915, 1100, 684, 720, 915, 1161, 915, 914, 915, 915, 915,
    /*  285 */ 915, 915, 915, 713, 738, 916, 1030, 684, 720, 747, 1161, 915, 914, 610, 915, 939, 915, 915, 915, 713,
    /*  305 */ 738, 915, 1100, 684, 720, 915, 1161, 915, 757, 915, 915, 915, 915, 915, 915, 713, 738, 915, 1100, 684,
    /*  325 */ 720, 915, 1161, 975, 914, 775, 853, 915, 915, 915, 915, 713, 738, 954, 958, 684, 720, 915, 785, 915, 658,
    /*  346 */ 915, 915, 915, 915, 915, 915, 713, 738, 808, 1176, 684, 720, 915, 1161, 915, 914, 915, 915, 915, 915,
    /*  366 */ 915, 915, 713, 819, 824, 833, 684, 720, 915, 1161, 643, 861, 870, 1080, 749, 915, 915, 915, 713, 738,
    /*  386 */ 915, 1100, 684, 720, 915, 1161, 915, 878, 915, 915, 915, 915, 915, 915, 713, 738, 915, 1100, 684, 889,
    /*  406 */ 915, 1161, 915, 914, 924, 935, 915, 915, 915, 915, 713, 738, 915, 1100, 684, 947, 915, 1161, 1050, 914,
    /*  426 */ 915, 993, 966, 983, 915, 915, 713, 1005, 1011, 1016, 684, 720, 915, 1161, 915, 914, 1083, 667, 676, 915,
    /*  446 */ 915, 915, 713, 738, 795, 800, 684, 720, 725, 730, 1024, 1038, 915, 915, 915, 915, 915, 915, 713, 738,
    /*  466 */ 915, 1100, 684, 720, 622, 1161, 915, 914, 915, 881, 915, 915, 915, 915, 713, 738, 1062, 1066, 684, 720,
    /*  486 */ 739, 1161, 988, 914, 915, 915, 915, 915, 915, 915, 713, 738, 915, 1100, 684, 720, 1044, 1161, 1074, 914,
    /*  506 */ 1150, 1091, 767, 915, 915, 915, 713, 738, 915, 1100, 684, 1108, 1116, 1161, 915, 1130, 762, 915, 915,
    /*  525 */ 915, 915, 915, 713, 738, 915, 1100, 684, 720, 915, 1161, 915, 914, 915, 997, 915, 915, 915, 915, 713,
    /*  545 */ 738, 915, 1100, 684, 720, 915, 1161, 915, 914, 915, 915, 1144, 915, 915, 915, 915, 825, 915, 1100, 915,
    /*  565 */ 1166, 915, 1161, 915, 914, 915, 915, 915, 915, 915, 915, 915, 915, 1184, 1188, 915, 915, 915, 915, 915,
    /*  585 */ 915, 915, 915, 915, 915, 915, 915, 384, 384, 384, 384, 384, 384, 384, 384, 30, 31, 0, 384, 384, 384, 384,
    /*  607 */ 0, 30, 31, 0, 0, 0, 0, 0, 0, 0, 94, 384, 384, 384, 384, 0, 0, 0, 0, 71, 0, 72, 0, 413, 413, 413, 413,
    /*  634 */ 413, 413, 413, 413, 0, 30, 31, 0, 30, 0, 0, 0, 0, 0, 0, 0, 81, 0, 2176, 0, 0, 0, 30, 31, 60, 0, 0, 0, 0,
    /*  663 */ 0, 0, 87, 73, 0, 0, 0, 0, 0, 0, 0, 99, 1706, 0, 0, 0, 0, 0, 0, 0, 105, 801, 930, 931, 1060, 1189, 1190,
    /*  690 */ 1319, 1448, 32, 32, 32, 32, 32, 32, 32, 32, 0, 30, 31, 317, 256, 0, 0, 0, 0, 0, 0, 0, 2048, 0, 801, 931,
    /*  716 */ 1060, 1190, 1319, 1448, 1577, 1706, 1707, 44, 45, 0, 0, 0, 70, 0, 70, 0, 0, 60, 317, 0, 0, 74, 1707, 0,
    /*  740 */ 0, 0, 0, 0, 0, 0, 2560, 0, 68, 0, 0, 0, 0, 0, 0, 104, 0, 60, 0, 0, 0, 85, 0, 0, 0, 90, 0, 0, 0, 0, 101,
    /*  771 */ 0, 103, 0, 0, 0, 89, 0, 0, 0, 0, 0, 0, 2176, 0, 3200, 0, 0, 60, 317, 0, 0, 0, 1854, 45, 0, 0, 0, 53, 0,
    /*  800 */ 55, 55, 0, 55, 0, 30, 31, 0, 0, 50, 0, 0, 0, 0, 0, 0, 30, 571, 0, 1707, 0, 0, 0, 46, 47, 0, 0, 0, 0, 0,
    /*  830 */ 0, 0, 3968, 47, 47, 0, 47, 0, 30, 31, 0, 30, 0, 60, 317, 44, 0, 0, 31, 60, 317, 0, 45, 0, 0, 0, 0, 0, 0,
    /*  859 */ 98, 0, 60, 0, 0, 0, 0, 0, 86, 0, 44, 0, 0, 0, 0, 0, 0, 93, 0, 60, 82, 0, 0, 0, 0, 0, 0, 97, 0, 0, 1577,
    /*  890 */ 1706, 1707, 44, 45, 64, 0, 0, 34, 0, 0, 37, 0, 0, 42, 44, 45, 0, 0, 0, 73, 317, 0, 0, 0, 60, 0, 0, 0, 0,
    /*  919 */ 0, 0, 0, 0, 56, 88, 0, 0, 0, 0, 0, 0, 0, 570, 31, 0, 95, 0, 0, 0, 0, 0, 0, 0, 102, 0, 0, 0, 1577, 1706,
    /*  949 */ 1707, 44, 45, 0, 65, 0, 49, 49, 49, 49, 49, 49, 49, 0, 30, 31, 0, 0, 100, 2688, 0, 0, 0, 0, 0, 2304, 0,
    /*  976 */ 0, 0, 0, 78, 0, 80, 0, 2944, 0, 0, 0, 0, 0, 0, 0, 2816, 0, 0, 0, 0, 3328, 0, 0, 0, 0, 3584, 0, 0, 0,
    /* 1005 */ 1707, 0, 0, 0, 0, 0, 48, 0, 52, 0, 52, 54, 54, 57, 54, 0, 30, 31, 0, 75, 0, 0, 0, 0, 0, 0, 0, 56, 0, 0,
    /* 1035 */ 30, 31, 0, 60, 0, 0, 84, 0, 3840, 0, 0, 69, 0, 0, 0, 0, 0, 77, 0, 0, 0, 0, 0, 2304, 0, 0, 2304, 0, 51,
    /* 1064 */ 51, 51, 51, 51, 51, 51, 0, 30, 31, 0, 0, 76, 0, 0, 0, 79, 0, 0, 96, 0, 0, 0, 0, 0, 92, 0, 0, 0, 3456, 0,
    /* 1094 */ 0, 0, 0, 0, 0, 1920, 0, 0, 0, 0, 0, 30, 31, 0, 1577, 1706, 1707, 44, 45, 0, 0, 66, 67, 0, 0, 0, 0, 66, 0,
    /* 1123 */ 0, 2304, 0, 0, 30, 31, 0, 60, 0, 83, 0, 0, 0, 0, 0, 2432, 0, 0, 30, 31, 0, 3712, 0, 0, 0, 0, 0, 0, 0,
    /* 1152 */ 3072, 0, 91, 0, 0, 0, 930, 0, 1189, 0, 0, 0, 60, 317, 0, 0, 0, 44, 45, 0, 0, 0, 44, 1855, 0, 0, 0, 50, 0,
    /* 1181 */ 30, 31, 0, 0, 640, 640, 640, 640, 640, 640, 640, 0, 0, 0, 0
  )

  private final val EXPECTED = Array(
    /*   0 */ 14, 38, 70, 134, 262, 518, 1030, 2054, 4102, 8198, 16390, 131078, 4194310, 8388614, 67108870, 1073774598,
    /*  16 */ 75497478, 540016662, 571473942, 672137238, 571605014, 907018262, 907083798, 588906518, 915406870,
    /*  25 */ 915472406, 589168662, 917504022, 4, 8, 8, 2, 32, 64, 64, 128, 256, 256, 512, 1024, 2048, 4096, 4096, 8192,
    /*  44 */ 8192, 4194304, 8388608, 67108864, 1048576, 2097152, 536870912, 33554432, 134217728, 100663296, 268435456,
    /*  55 */ 524288, 50331648, 8, 8, 2, 2, 8192, 8192, 4194304, 8388608, 67108864, 1048576, 2097152, 536870912,
    /*  69 */ 33554432, 134217728, 268435456, 2, 4194304, 8388608, 67108864, 1048576, 536870912, 33554432, 134217728,
    /*  80 */ 268435456, 4194304, 8388608, 67108864, 1048576, 33554432, 134217728, 268435456, 4194304, 67108864,
    /*  90 */ 1048576, 33554432, 134217728, 268435456, 4194304, 1048576, 268435456, 4194304, 1048576, 4194304, 4194304,
    /* 101 */ 4194304, 4194304, 4194304, 4194304
  )

  private final val TOKEN = Array(
    "(0)",
    "Comment",
    "WhiteSpace",
    "StringLiteral",
    "EOF",
    "PortName",
    "StepName",
    "OptionName",
    "ClassName",
    "TypeName",
    "Literal",
    "Macro",
    "FunctionName",
    "Expansion",
    "'('",
    "')'",
    "'*'",
    "'='",
    "'?'",
    "'as'",
    "'function'",
    "'has'",
    "'implementation'",
    "'input'",
    "'of'",
    "'option'",
    "'output'",
    "'prefix'",
    "'primary'",
    "'step'",
    "'|'"
  )
}

// End
