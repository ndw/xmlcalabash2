package com.xmlcalabash.parsers

// This file was generated on Wed Aug 15, 2018 16:55 (UTC+01) by REx v5.47 which is Copyright (c) 1979-2017 by Gunther Rademacher <grd@gmx.net>
// REx command line: StepConfigParser.ebnf -ll 2 -scala -tree

import collection.mutable.ArrayBuffer

class StepConfigParser {

  def this(string: String, eh: StepConfigParser.EventHandler) {
    this
    initialize(string, eh)
  }

  def initialize(source: String, parsingEventHandler: StepConfigParser.EventHandler) {
    eventHandler = parsingEventHandler
    input = source
    size = source.length
    reset(0, 0, 0)
  }

  def getInput: String = {
    return input
  }

  def getTokenOffset: Int = {
    return b0
  }

  def getTokenEnd: Int = {
    return e0
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
      if (l1 != 28) {               // 'prefix'
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
          case 30 =>                  // 'step'
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
    consume(28)                     // 'prefix'
    lookahead1W(7)                  // Comment | WhiteSpace | Macro
    consume(11)                     // Macro
    lookahead1W(11)                 // Comment | WhiteSpace | '='
    consume(18)                     // '='
    lookahead1W(9)                  // Comment | WhiteSpace | Expansion
    consume(13)                     // Expansion
    eventHandler.endNonterminal("Prefix", e0)
  }

  private def parse_Step {
    eventHandler.startNonterminal("Step", e0)
    consume(30)                     // 'step'
    lookahead1W(2)                  // Comment | WhiteSpace | StepName
    consume(6)                      // StepName
    var c1 = true
    while (c1) {
      lookahead1W(28)               // Comment | WhiteSpace | EOF | 'function' | 'has' | 'input' | 'option' | 'output' |
      // 'primary' | 'step'
      if (l1 != 22) {               // 'has'
        c1 = false
      }
      else {
        whitespace
        parse_Implementation
      }
    }
    var c2 = true
    while (c2) {
      lookahead1W(25)               // Comment | WhiteSpace | EOF | 'function' | 'input' | 'option' | 'output' |
      // 'primary' | 'step'
      l1 match {
        case 29 =>                    // 'primary'
          lookahead2W(16)             // Comment | WhiteSpace | 'input' | 'output'
        case _ =>
          lk = l1
      }
      if (lk != 24                  // 'input'
        && lk != 1565) {             // 'primary' 'input'
        c2 = false
      }
      else {
        whitespace
        parse_Input
      }
    }
    var c3 = true
    while (c3) {
      lookahead1W(22)               // Comment | WhiteSpace | EOF | 'function' | 'option' | 'output' | 'primary' |
      // 'step'
      if (l1 != 27                  // 'output'
        && l1 != 29) {               // 'primary'
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
      if (l1 != 26) {               // 'option'
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
    consume(21)                     // 'function'
    lookahead1W(8)                  // Comment | WhiteSpace | FunctionName
    consume(12)                     // FunctionName
    var c1 = true
    while (c1) {
      lookahead1W(17)               // Comment | WhiteSpace | EOF | 'function' | 'has' | 'step'
      if (l1 != 22) {               // 'has'
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
    consume(22)                     // 'has'
    lookahead1W(12)                 // Comment | WhiteSpace | 'implementation'
    consume(23)                     // 'implementation'
    lookahead1W(4)                  // Comment | WhiteSpace | ClassName
    consume(8)                      // ClassName
    eventHandler.endNonterminal("Implementation", e0)
  }

  private def parse_Input {
    eventHandler.startNonterminal("Input", e0)
    if (l1 == 29) {                 // 'primary'
      consume(29)                   // 'primary'
    }
    lookahead1W(13)                 // Comment | WhiteSpace | 'input'
    consume(24)                     // 'input'
    lookahead1W(1)                  // Comment | WhiteSpace | PortName
    consume(5)                      // PortName
    lookahead1W(26)                 // Comment | WhiteSpace | EOF | '*' | 'function' | 'input' | 'option' | 'output' |
    // 'primary' | 'step'
    if (l1 == 17) {                 // '*'
      consume(17)                   // '*'
    }
    eventHandler.endNonterminal("Input", e0)
  }

  private def parse_Output {
    eventHandler.startNonterminal("Output", e0)
    if (l1 == 29) {                 // 'primary'
      consume(29)                   // 'primary'
    }
    lookahead1W(14)                 // Comment | WhiteSpace | 'output'
    consume(27)                     // 'output'
    lookahead1W(1)                  // Comment | WhiteSpace | PortName
    consume(5)                      // PortName
    lookahead1W(23)                 // Comment | WhiteSpace | EOF | '*' | 'function' | 'option' | 'output' | 'primary' |
    // 'step'
    if (l1 == 17) {                 // '*'
      consume(17)                   // '*'
    }
    eventHandler.endNonterminal("Output", e0)
  }

  private def parse_Option {
    eventHandler.startNonterminal("Option", e0)
    consume(26)                     // 'option'
    lookahead1W(3)                  // Comment | WhiteSpace | OptionName
    consume(7)                      // OptionName
    lookahead1W(27)                 // Comment | WhiteSpace | EOF | '=' | '?' | 'as' | 'function' | 'of' | 'option' |
    // 'step'
    if (l1 == 19) {                 // '?'
      consume(19)                   // '?'
    }
    lookahead1W(24)                 // Comment | WhiteSpace | EOF | '=' | 'as' | 'function' | 'of' | 'option' | 'step'
    if (l1 == 20                    // 'as'
      || l1 == 25) {                 // 'of'
      whitespace
      parse_DeclaredType
    }
    lookahead1W(20)                 // Comment | WhiteSpace | EOF | '=' | 'function' | 'option' | 'step'
    if (l1 == 18) {                 // '='
      consume(18)                   // '='
      lookahead1W(0)                // Comment | WhiteSpace | StringLiteral
      consume(3)                    // StringLiteral
    }
    eventHandler.endNonterminal("Option", e0)
  }

  private def parse_DeclaredType {
    eventHandler.startNonterminal("DeclaredType", e0)
    l1 match {
      case 20 =>                      // 'as'
        consume(20)                   // 'as'
        lookahead1W(5)                // Comment | WhiteSpace | TypeName
        whitespace
        parse_SeqType
      case _ =>
        consume(25)                   // 'of'
        lookahead1W(10)               // Comment | WhiteSpace | '('
        whitespace
        parse_TokenList
    }
    eventHandler.endNonterminal("DeclaredType", e0)
  }

  private def parse_TokenList {
    eventHandler.startNonterminal("TokenList", e0)
    consume(15)                     // '('
    lookahead1W(6)                  // Comment | WhiteSpace | Literal
    consume(10)                     // Literal
    var c1 = true
    while (c1) {
      lookahead1W(15)               // Comment | WhiteSpace | ')' | '|'
      if (l1 != 31) {               // '|'
        c1 = false
      }
      else {
        consume(31)                 // '|'
        lookahead1W(6)              // Comment | WhiteSpace | Literal
        consume(10)                 // Literal
      }
    }
    consume(16)                     // ')'
    eventHandler.endNonterminal("TokenList", e0)
  }

  private def parse_SeqType {
    eventHandler.startNonterminal("SeqType", e0)
    consume(9)                      // TypeName
    lookahead1W(21)                 // Comment | WhiteSpace | EOF | Occurrence | '=' | 'function' | 'option' | 'step'
    if (l1 == 14) {                 // Occurrence
      consume(14)                   // Occurrence
    }
    eventHandler.endNonterminal("SeqType", e0)
  }

  def getErrorMessage(e: StepConfigParser.ParseException) = {
    var message = e.getMessage
    val tokenSet = StepConfigParser.getExpectedTokenSet(e)
    val found = StepConfigParser.getOffendingToken(e)
    val size = e.end - e.begin
    message += (if (found == null) "" else ", found " + found) + "\nwhile expecting " +
      (if (tokenSet.length == 1) tokenSet(0) else "[" + (tokenSet mkString ", ") + "]") + "\n" +
      (if (size == 0 || found != null) "" else "after successfully scanning " + size + " characters beginning ")
    val prefix = input.substring(0, e.begin)
    val line = prefix.replaceAll("[^\n]", "").length + 1
    val column = prefix.length - prefix.lastIndexOf('\n')
    message +
      "at line " + line + ", column " + column + ":\n..." +
      input.substring(e.begin, math.min(input.length, e.begin + 64)) + "..."
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

  private def matchW(set: Int): Int = {
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
      (result & 63) - 1
    }
  }

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
  var input: String = null
  var size = 0
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
      val nonterminal = new Nonterminal(name, begin, begin, ArrayBuffer[Symbol]())
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
    var expected = ArrayBuffer[String]()
    val s = if (tokenSetId < 0) - tokenSetId else INITIAL(tokenSetId) & 127
    var i = 0
    while (i < 32) {
      var j = i
      val i0 = (i >> 5) * 106 + s - 1
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
    /*   0 */ 37, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7,
    /*  35 */ 6, 6, 6, 6, 8, 9, 10, 11, 12, 6, 13, 13, 14, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 15, 6, 6, 16, 6, 17,
    /*  64 */ 6, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
    /*  91 */ 6, 6, 6, 6, 18, 6, 19, 18, 20, 18, 21, 22, 18, 23, 24, 18, 18, 25, 26, 27, 28, 29, 18, 30, 31, 32, 33, 18,
    /* 119 */ 18, 34, 35, 18, 6, 36, 6, 6, 6
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
    /* 231 */ 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 392, 37, 1, 1, 1, 1, 1, 1, 1,
    /* 255 */ 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7, 6, 6, 6, 6, 8, 9, 10, 11, 12,
    /* 289 */ 6, 13, 13, 14, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 6, 6, 6, 6, 18, 18, 18, 18, 18, 18, 18, 18, 18,
    /* 317 */ 18, 18, 18, 18, 18, 6, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 6, 19, 18, 20, 18,
    /* 344 */ 21, 22, 18, 23, 24, 18, 18, 25, 26, 27, 28, 29, 18, 30, 31, 32, 33, 18, 18, 34, 35, 18, 6, 36, 6, 6, 6, 6,
    /* 372 */ 6, 6, 6, 6, 6, 6, 6, 6, 18, 18, 6, 6, 6, 6, 6, 6, 6, 6, 6, 13, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    /* 406 */ 6, 6, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 15, 6, 6, 16, 6, 17
  )

  private final val MAP2 = Array(
    /*  0 */ 57344, 63744, 64976, 65008, 65536, 983040, 63743, 64975, 65007, 65533, 983039, 1114111, 6, 18, 6, 18, 18,
    /* 17 */ 6
  )

  private final val INITIAL = Array(
    /*  0 */ 1, 2, 899, 4, 1157, 6, 7, 8, 1673, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
    /* 27 */ 28, 29
  )

  private final val TRANSITION = Array(
    /*    0 */ 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935,
    /*   21 */ 935, 935, 969, 935, 933, 935, 935, 935, 935, 935, 935, 608, 608, 608, 618, 935, 1077, 935, 969, 935, 933,
    /*   42 */ 935, 935, 935, 935, 935, 935, 608, 608, 608, 610, 935, 1077, 935, 930, 935, 933, 935, 935, 935, 935, 935,
    /*   63 */ 935, 608, 608, 608, 634, 935, 935, 935, 969, 935, 933, 935, 935, 935, 935, 935, 935, 647, 647, 647, 650,
    /*   84 */ 935, 1077, 935, 930, 935, 933, 935, 935, 935, 935, 935, 935, 935, 935, 935, 669, 935, 1077, 935, 969,
    /*  104 */ 935, 933, 935, 935, 935, 935, 935, 935, 658, 667, 935, 801, 935, 1047, 935, 957, 935, 933, 935, 935, 935,
    /*  125 */ 935, 935, 935, 625, 677, 935, 679, 935, 902, 935, 639, 935, 933, 935, 935, 935, 935, 935, 935, 935, 687,
    /*  146 */ 935, 669, 935, 1077, 935, 969, 935, 933, 935, 935, 935, 935, 935, 935, 935, 730, 935, 669, 935, 1077,
    /*  166 */ 935, 969, 935, 933, 935, 935, 935, 935, 935, 935, 935, 935, 874, 698, 934, 1077, 935, 791, 935, 706, 935,
    /*  187 */ 935, 935, 935, 935, 935, 935, 935, 1170, 669, 935, 1077, 935, 969, 935, 933, 935, 935, 935, 935, 935,
    /*  207 */ 935, 716, 729, 935, 669, 738, 781, 935, 969, 935, 933, 935, 935, 935, 935, 935, 935, 746, 746, 746, 749,
    /*  228 */ 862, 1077, 935, 969, 935, 757, 935, 935, 935, 935, 935, 935, 935, 935, 935, 669, 721, 1145, 935, 969,
    /*  248 */ 935, 933, 935, 935, 935, 935, 935, 935, 935, 1073, 1059, 767, 935, 1077, 935, 969, 935, 933, 935, 935,
    /*  268 */ 935, 935, 935, 935, 935, 935, 1170, 1132, 935, 1077, 935, 969, 935, 933, 935, 935, 935, 935, 935, 935,
    /*  288 */ 775, 799, 935, 669, 738, 781, 935, 969, 935, 933, 935, 935, 935, 935, 935, 935, 775, 799, 935, 809, 738,
    /*  309 */ 781, 1104, 969, 935, 933, 935, 817, 820, 935, 935, 935, 775, 799, 935, 669, 738, 781, 935, 969, 935, 828,
    /*  330 */ 935, 935, 935, 935, 935, 935, 775, 799, 935, 669, 738, 781, 935, 969, 1107, 933, 1167, 659, 935, 935,
    /*  350 */ 935, 935, 775, 799, 846, 849, 738, 781, 935, 857, 935, 933, 871, 935, 935, 935, 935, 935, 775, 799, 882,
    /*  371 */ 838, 738, 781, 935, 969, 935, 933, 935, 935, 935, 935, 935, 935, 775, 893, 898, 910, 738, 781, 935, 969,
    /*  392 */ 935, 918, 936, 834, 626, 935, 935, 935, 775, 799, 935, 669, 738, 781, 935, 969, 935, 926, 935, 935, 935,
    /*  413 */ 935, 935, 935, 775, 799, 935, 669, 738, 944, 935, 969, 935, 933, 965, 977, 935, 935, 935, 935, 775, 799,
    /*  434 */ 935, 669, 738, 989, 935, 969, 787, 933, 935, 1205, 997, 1009, 935, 935, 775, 1021, 1027, 1035, 738, 781,
    /*  454 */ 935, 969, 935, 933, 708, 935, 1043, 1055, 935, 935, 775, 799, 1183, 1188, 738, 781, 981, 1067, 1085,
    /*  473 */ 1097, 935, 935, 935, 935, 935, 935, 775, 799, 935, 669, 738, 781, 690, 969, 935, 933, 935, 759, 935, 935,
    /*  494 */ 935, 935, 775, 799, 1115, 1118, 738, 781, 935, 1126, 1089, 933, 935, 935, 935, 935, 935, 935, 775, 799,
    /*  514 */ 935, 669, 738, 781, 1201, 969, 1160, 933, 951, 1140, 1013, 935, 935, 935, 775, 799, 935, 669, 738, 781,
    /*  534 */ 1153, 969, 935, 1178, 1001, 935, 935, 935, 935, 935, 775, 799, 935, 669, 738, 781, 935, 969, 935, 933,
    /*  554 */ 935, 885, 935, 935, 935, 935, 775, 799, 935, 669, 738, 781, 935, 969, 935, 933, 935, 935, 1196, 935, 935,
    /*  575 */ 935, 935, 863, 935, 669, 935, 1077, 935, 969, 935, 933, 935, 935, 935, 935, 935, 935, 935, 935, 1213,
    /*  595 */ 1216, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 935, 384, 384, 384, 384, 384, 384, 384, 384,
    /*  616 */ 31, 32, 384, 384, 384, 384, 384, 0, 31, 32, 0, 0, 0, 0, 0, 0, 0, 105, 384, 384, 384, 384, 384, 0, 0, 0,
    /*  642 */ 32, 61, 318, 0, 46, 414, 414, 414, 414, 414, 414, 414, 414, 0, 31, 32, 31, 0, 0, 0, 0, 0, 0, 0, 99, 0,
    /*  668 */ 45, 0, 0, 0, 0, 0, 0, 31, 32, 0, 46, 0, 0, 0, 0, 0, 0, 31, 572, 0, 0, 2048, 0, 0, 0, 0, 0, 72, 0, 73, 0,
    /*  699 */ 0, 2304, 0, 0, 0, 31, 32, 0, 74, 0, 0, 0, 0, 0, 0, 93, 0, 0, 0, 931, 0, 1190, 0, 0, 0, 35, 0, 0, 38, 0,
    /*  729 */ 1707, 0, 0, 0, 0, 0, 0, 0, 2176, 0, 802, 931, 932, 1061, 1190, 1191, 1320, 33, 33, 33, 33, 33, 33, 33,
    /*  753 */ 33, 0, 31, 32, 0, 256, 0, 0, 0, 0, 0, 0, 98, 0, 2432, 0, 0, 2432, 0, 0, 31, 32, 0, 802, 932, 1061, 1191,
    /*  780 */ 1320, 1449, 1578, 1707, 1708, 45, 46, 0, 0, 0, 78, 0, 0, 0, 0, 74, 318, 0, 0, 1708, 0, 0, 0, 0, 0, 0, 0,
    /*  807 */ 571, 32, 57, 0, 0, 57, 0, 0, 31, 32, 95, 0, 0, 0, 0, 0, 0, 0, 103, 0, 0, 0, 61, 0, 0, 0, 86, 0, 0, 0, 97,
    /*  838 */ 0, 0, 0, 0, 51, 0, 31, 32, 0, 50, 50, 50, 50, 50, 50, 50, 0, 31, 32, 0, 3328, 0, 0, 61, 318, 0, 0, 0, 0,
    /*  867 */ 0, 0, 0, 4096, 88, 0, 0, 0, 0, 0, 0, 0, 1920, 0, 2304, 0, 51, 0, 0, 0, 0, 0, 0, 3712, 0, 0, 1708, 0, 0,
    /*  896 */ 0, 47, 48, 0, 0, 0, 0, 0, 0, 0, 45, 1856, 0, 0, 0, 48, 48, 0, 48, 0, 31, 32, 82, 61, 0, 0, 0, 0, 0, 87,
    /*  926 */ 0, 61, 83, 0, 0, 0, 0, 0, 61, 0, 0, 0, 0, 0, 0, 0, 0, 94, 1449, 1578, 1707, 1708, 45, 46, 65, 0, 0, 0,
    /*  954 */ 3200, 0, 92, 0, 0, 31, 0, 61, 318, 45, 0, 0, 89, 0, 0, 0, 0, 0, 0, 61, 318, 0, 0, 0, 96, 0, 0, 0, 0, 0,
    /*  984 */ 0, 71, 0, 71, 0, 1449, 1578, 1707, 1708, 45, 46, 0, 66, 0, 0, 101, 2816, 0, 0, 0, 0, 91, 0, 0, 0, 0,
    /* 1010 */ 3072, 0, 0, 0, 0, 0, 0, 102, 0, 104, 0, 1708, 0, 0, 0, 0, 0, 49, 0, 53, 0, 53, 53, 55, 55, 58, 55, 55,
    /* 1038 */ 58, 55, 0, 31, 32, 100, 0, 0, 0, 0, 0, 0, 0, 1855, 46, 0, 0, 106, 0, 0, 0, 0, 0, 0, 0, 2432, 2432, 0, 0,
    /* 1067 */ 0, 71, 0, 0, 61, 318, 0, 0, 0, 2432, 0, 0, 0, 0, 45, 46, 0, 0, 75, 76, 0, 0, 0, 0, 0, 0, 2944, 0, 0, 0,
    /* 1097 */ 0, 61, 0, 0, 85, 0, 3968, 0, 0, 69, 0, 0, 0, 0, 0, 79, 0, 81, 0, 52, 52, 52, 52, 52, 52, 52, 0, 31, 32,
    /* 1126 */ 2688, 0, 0, 0, 61, 318, 0, 0, 0, 2560, 0, 0, 31, 32, 0, 0, 3584, 0, 0, 0, 0, 0, 43, 45, 46, 0, 0, 67, 68,
    /* 1155 */ 0, 0, 0, 0, 67, 0, 0, 77, 0, 0, 0, 80, 0, 0, 90, 0, 0, 0, 0, 0, 1920, 0, 0, 0, 61, 0, 84, 0, 0, 0, 0, 54,
    /* 1187 */ 0, 0, 56, 56, 0, 56, 0, 31, 32, 0, 3840, 0, 0, 0, 0, 0, 0, 70, 0, 0, 0, 0, 3456, 0, 0, 0, 0, 640, 640,
    /* 1216 */ 640, 640, 640, 640, 640, 0, 0, 0
  )

  private final val EXPECTED = Array(
    /*   0 */ 14, 38, 70, 134, 262, 518, 1030, 2054, 4102, 8198, 32774, 262150, 8388614, 16777222, 134217734,
    /*  15 */ -2147418106, 150994950, 1080033302, 1142947862, 1344274454, 1143210006, 1143226390, 1814036502,
    /*  23 */ 1814167574, 1177813014, 1830813718, 1830944790, 1178337302, 1835008022, 4, 8, 8, 2, 32, 64, 64, 128, 256,
    /*  38 */ 256, 512, 1024, 2048, 4096, 4096, 8192, 8192, 8388608, 16777216, 134217728, 2097152, 4194304, 1073741824,
    /*  52 */ 67108864, 268435456, 201326592, 536870912, 1048576, 100663296, 8, 8, 2, 2, 8192, 8192, 8388608, 16777216,
    /*  66 */ 134217728, 2097152, 4194304, 1073741824, 67108864, 268435456, 536870912, 2, 8388608, 16777216, 134217728,
    /*  77 */ 2097152, 1073741824, 67108864, 268435456, 536870912, 8388608, 16777216, 134217728, 2097152, 67108864,
    /*  87 */ 268435456, 536870912, 8388608, 134217728, 2097152, 67108864, 268435456, 536870912, 8388608, 2097152,
    /*  97 */ 536870912, 8388608, 2097152, 8388608, 8388608, 8388608, 8388608, 8388608, 8388608
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
    "Occurrence",
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
