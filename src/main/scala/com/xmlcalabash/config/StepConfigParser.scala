package com.xmlcalabash.config

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
      lookahead1W(17)               // Comment | WhiteSpace | EOF | 'prefix' | 'step'
      if (l1 != 25) {               // 'prefix'
        c1 = false
      }
      else {
        whitespace
        parse_Prefix
      }
    }
    var c2 = true
    while (c2) {
      if (l1 != 27) {               // 'step'
        c2 = false
      }
      else {
        whitespace
        parse_Step
      }
    }
    consume(4)                      // EOF
    eventHandler.endNonterminal("Config", e0)
  }

  private def parse_Prefix {
    eventHandler.startNonterminal("Prefix", e0)
    consume(25)                     // 'prefix'
    lookahead1W(7)                  // Comment | WhiteSpace | Macro
    consume(11)                     // Macro
    lookahead1W(10)                 // Comment | WhiteSpace | '='
    consume(16)                     // '='
    lookahead1W(8)                  // Comment | WhiteSpace | Expansion
    consume(12)                     // Expansion
    eventHandler.endNonterminal("Prefix", e0)
  }

  private def parse_Step {
    eventHandler.startNonterminal("Step", e0)
    consume(27)                     // 'step'
    lookahead1W(2)                  // Comment | WhiteSpace | StepName
    consume(6)                      // StepName
    var c1 = true
    while (c1) {
      lookahead1W(25)               // Comment | WhiteSpace | EOF | 'has' | 'input' | 'option' | 'output' | 'primary' |
      // 'step'
      if (l1 != 19) {               // 'has'
        c1 = false
      }
      else {
        whitespace
        parse_Implementation
      }
    }
    var c2 = true
    while (c2) {
      lookahead1W(22)               // Comment | WhiteSpace | EOF | 'input' | 'option' | 'output' | 'primary' | 'step'
      l1 match {
        case 26 =>                    // 'primary'
          lookahead2W(15)             // Comment | WhiteSpace | 'input' | 'output'
        case _ =>
          lk = l1
      }
      if (lk != 21                  // 'input'
        && lk != 698) {              // 'primary' 'input'
        c2 = false
      }
      else {
        whitespace
        parse_Input
      }
    }
    var c3 = true
    while (c3) {
      lookahead1W(19)               // Comment | WhiteSpace | EOF | 'option' | 'output' | 'primary' | 'step'
      if (l1 != 24                  // 'output'
        && l1 != 26) {               // 'primary'
        c3 = false
      }
      else {
        whitespace
        parse_Output
      }
    }
    var c4 = true
    while (c4) {
      lookahead1W(16)               // Comment | WhiteSpace | EOF | 'option' | 'step'
      if (l1 != 23) {               // 'option'
        c4 = false
      }
      else {
        whitespace
        parse_Option
      }
    }
    eventHandler.endNonterminal("Step", e0)
  }

  private def parse_Implementation {
    eventHandler.startNonterminal("Implementation", e0)
    consume(19)                     // 'has'
    lookahead1W(11)                 // Comment | WhiteSpace | 'implementation'
    consume(20)                     // 'implementation'
    lookahead1W(4)                  // Comment | WhiteSpace | ClassName
    consume(8)                      // ClassName
    eventHandler.endNonterminal("Implementation", e0)
  }

  private def parse_Input {
    eventHandler.startNonterminal("Input", e0)
    if (l1 == 26) {                 // 'primary'
      consume(26)                   // 'primary'
    }
    lookahead1W(12)                 // Comment | WhiteSpace | 'input'
    consume(21)                     // 'input'
    lookahead1W(1)                  // Comment | WhiteSpace | PortName
    consume(5)                      // PortName
    lookahead1W(23)                 // Comment | WhiteSpace | EOF | '*' | 'input' | 'option' | 'output' | 'primary' |
    // 'step'
    if (l1 == 15) {                 // '*'
      consume(15)                   // '*'
    }
    eventHandler.endNonterminal("Input", e0)
  }

  private def parse_Output {
    eventHandler.startNonterminal("Output", e0)
    if (l1 == 26) {                 // 'primary'
      consume(26)                   // 'primary'
    }
    lookahead1W(13)                 // Comment | WhiteSpace | 'output'
    consume(24)                     // 'output'
    lookahead1W(1)                  // Comment | WhiteSpace | PortName
    consume(5)                      // PortName
    lookahead1W(20)                 // Comment | WhiteSpace | EOF | '*' | 'option' | 'output' | 'primary' | 'step'
    if (l1 == 15) {                 // '*'
      consume(15)                   // '*'
    }
    eventHandler.endNonterminal("Output", e0)
  }

  private def parse_Option {
    eventHandler.startNonterminal("Option", e0)
    consume(23)                     // 'option'
    lookahead1W(3)                  // Comment | WhiteSpace | OptionName
    consume(7)                      // OptionName
    lookahead1W(24)                 // Comment | WhiteSpace | EOF | '=' | '?' | 'as' | 'of' | 'option' | 'step'
    if (l1 == 17) {                 // '?'
      consume(17)                   // '?'
    }
    lookahead1W(21)                 // Comment | WhiteSpace | EOF | '=' | 'as' | 'of' | 'option' | 'step'
    if (l1 == 18                    // 'as'
      || l1 == 22) {                 // 'of'
      whitespace
      parse_DeclaredType
    }
    lookahead1W(18)                 // Comment | WhiteSpace | EOF | '=' | 'option' | 'step'
    if (l1 == 16) {                 // '='
      consume(16)                   // '='
      lookahead1W(0)                // Comment | WhiteSpace | StringLiteral
      consume(3)                    // StringLiteral
    }
    eventHandler.endNonterminal("Option", e0)
  }

  private def parse_DeclaredType {
    eventHandler.startNonterminal("DeclaredType", e0)
    l1 match {
      case 18 =>                      // 'as'
        consume(18)                   // 'as'
        lookahead1W(5)                // Comment | WhiteSpace | TypeName
        consume(9)                    // TypeName
      case _ =>
        consume(22)                   // 'of'
        lookahead1W(9)                // Comment | WhiteSpace | '('
        whitespace
        parse_TokenList
    }
    eventHandler.endNonterminal("DeclaredType", e0)
  }

  private def parse_TokenList {
    eventHandler.startNonterminal("TokenList", e0)
    consume(13)                     // '('
    lookahead1W(6)                  // Comment | WhiteSpace | Literal
    consume(10)                     // Literal
    var c1 = true
    while (c1) {
      lookahead1W(14)               // Comment | WhiteSpace | ')' | '|'
      if (l1 != 28) {               // '|'
        c1 = false
      }
      else {
        consume(28)                 // '|'
        lookahead1W(6)              // Comment | WhiteSpace | Literal
        consume(10)                 // Literal
      }
    }
    consume(14)                     // ')'
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
    lk = (l2 << 5) | l1
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
    while (i < 29) {
      var j = i
      val i0 = (i >> 5) * 94 + s - 1
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
    /*   0 */ 35, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7,
    /*  35 */ 6, 6, 6, 6, 8, 9, 10, 11, 6, 6, 12, 12, 13, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 14, 6, 6, 15, 6, 16,
    /*  64 */ 6, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
    /*  91 */ 6, 6, 6, 6, 17, 6, 18, 17, 17, 17, 19, 20, 17, 21, 22, 17, 17, 23, 24, 25, 26, 27, 17, 28, 29, 30, 31, 17,
    /* 119 */ 17, 32, 33, 17, 6, 34, 6, 6, 6
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
    /* 231 */ 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 323, 392, 35, 1, 1, 1, 1, 1, 1, 1,
    /* 255 */ 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7, 6, 6, 6, 6, 8, 9, 10, 11, 6, 6,
    /* 290 */ 12, 12, 13, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 6, 6, 6, 6, 17, 17, 17, 17, 17, 17, 17, 17, 17,
    /* 317 */ 17, 17, 17, 17, 17, 6, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 6, 18, 17, 17, 17,
    /* 344 */ 19, 20, 17, 21, 22, 17, 17, 23, 24, 25, 26, 27, 17, 28, 29, 30, 31, 17, 17, 32, 33, 17, 6, 34, 6, 6, 6, 6,
    /* 372 */ 6, 6, 6, 6, 6, 6, 6, 6, 17, 17, 6, 6, 6, 6, 6, 6, 6, 6, 6, 12, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    /* 406 */ 6, 6, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 14, 6, 6, 15, 6, 16
  )

  private final val MAP2 = Array(
    /*  0 */ 57344, 63744, 64976, 65008, 65536, 983040, 63743, 64975, 65007, 65533, 983039, 1114111, 6, 17, 6, 17, 17,
    /* 17 */ 6
  )

  private final val INITIAL = Array(
    /*  0 */ 1, 2, 899, 4, 1157, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
  )

  private final val TRANSITION = Array(
    /*    0 */ 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663,
    /*   21 */ 663, 653, 663, 723, 663, 663, 663, 663, 663, 663, 663, 576, 576, 576, 599, 664, 662, 653, 663, 723, 663,
    /*   42 */ 663, 663, 663, 663, 663, 663, 576, 576, 576, 581, 664, 662, 605, 663, 723, 663, 663, 663, 663, 663, 663,
    /*   63 */ 663, 576, 576, 576, 613, 663, 663, 653, 663, 723, 663, 663, 663, 663, 663, 663, 663, 623, 623, 623, 629,
    /*   84 */ 664, 662, 605, 663, 723, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 586, 664, 662, 653, 663, 723,
    /*  105 */ 663, 663, 663, 663, 663, 663, 663, 642, 702, 663, 789, 703, 662, 776, 702, 723, 663, 663, 663, 663, 663,
    /*  126 */ 663, 663, 603, 662, 663, 794, 664, 651, 726, 661, 723, 663, 663, 663, 663, 663, 663, 663, 663, 672, 663,
    /*  147 */ 586, 664, 662, 653, 663, 723, 663, 663, 663, 663, 663, 663, 663, 663, 899, 663, 586, 664, 662, 653, 663,
    /*  168 */ 723, 663, 663, 663, 663, 663, 663, 663, 663, 663, 1081, 634, 664, 662, 674, 663, 1010, 663, 663, 663,
    /*  188 */ 663, 663, 663, 663, 682, 663, 663, 591, 695, 662, 653, 663, 723, 663, 663, 663, 663, 663, 663, 663, 711,
    /*  209 */ 711, 711, 717, 664, 662, 653, 663, 734, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 586, 745, 662,
    /*  230 */ 653, 663, 723, 663, 663, 663, 663, 663, 663, 663, 663, 756, 753, 767, 664, 662, 653, 663, 723, 663, 663,
    /*  251 */ 663, 663, 663, 663, 663, 663, 663, 663, 784, 664, 662, 653, 663, 723, 663, 663, 663, 663, 663, 663, 663,
    /*  272 */ 807, 663, 663, 591, 695, 662, 653, 663, 723, 663, 663, 663, 663, 663, 663, 663, 807, 663, 737, 815, 695,
    /*  293 */ 662, 687, 663, 723, 643, 663, 823, 663, 663, 663, 663, 807, 663, 663, 591, 695, 662, 653, 825, 723, 833,
    /*  314 */ 836, 663, 663, 663, 663, 663, 807, 663, 663, 591, 695, 662, 844, 663, 723, 852, 663, 663, 663, 663, 663,
    /*  335 */ 663, 807, 663, 663, 862, 695, 662, 653, 663, 723, 663, 663, 663, 663, 663, 663, 663, 807, 799, 615, 870,
    /*  356 */ 695, 662, 653, 663, 878, 854, 663, 772, 663, 663, 663, 663, 807, 663, 663, 591, 695, 662, 653, 663, 885,
    /*  377 */ 663, 663, 663, 663, 663, 663, 663, 807, 663, 663, 591, 695, 897, 653, 663, 723, 907, 917, 663, 663, 663,
    /*  398 */ 663, 663, 807, 663, 663, 591, 695, 927, 653, 663, 723, 663, 938, 930, 663, 663, 663, 663, 807, 1013, 946,
    /*  419 */ 954, 695, 662, 653, 663, 723, 759, 663, 1098, 663, 663, 663, 663, 807, 663, 962, 970, 695, 978, 983, 991,
    /*  440 */ 1069, 663, 663, 663, 663, 663, 663, 663, 807, 663, 663, 591, 695, 1003, 1021, 663, 723, 663, 1055, 663,
    /*  460 */ 663, 663, 663, 663, 807, 663, 1029, 1035, 695, 662, 1043, 663, 1051, 663, 663, 663, 663, 663, 663, 663,
    /*  480 */ 807, 663, 663, 591, 695, 1063, 653, 889, 723, 1094, 1077, 1089, 663, 663, 663, 663, 807, 663, 663, 591,
    /*  500 */ 695, 1106, 653, 663, 1126, 995, 663, 663, 663, 663, 663, 663, 807, 663, 663, 591, 695, 662, 653, 663,
    /*  520 */ 723, 663, 1131, 663, 663, 663, 663, 663, 807, 663, 663, 591, 695, 662, 653, 663, 723, 663, 909, 663, 663,
    /*  541 */ 663, 663, 663, 663, 919, 663, 586, 664, 662, 653, 663, 723, 663, 663, 663, 663, 663, 663, 663, 663, 663,
    /*  562 */ 1114, 1120, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 663, 384, 384, 384, 384, 384, 384,
    /*  582 */ 384, 384, 28, 29, 0, 0, 0, 28, 29, 0, 0, 0, 28, 29, 0, 799, 928, 384, 384, 0, 28, 29, 0, 0, 0, 0, 0, 0,
    /*  610 */ 0, 55, 0, 384, 384, 0, 0, 0, 0, 0, 0, 43, 43, 411, 411, 411, 411, 411, 411, 411, 411, 0, 28, 29, 0, 0, 0,
    /*  637 */ 28, 29, 55, 0, 0, 28, 0, 0, 0, 0, 0, 0, 0, 85, 1722, 0, 0, 0, 0, 0, 0, 0, 55, 312, 0, 41, 0, 0, 0, 0, 0,
    /*  668 */ 0, 0, 0, 40, 0, 1792, 0, 0, 0, 0, 0, 0, 67, 312, 0, 0, 928, 0, 1187, 0, 0, 0, 66, 0, 0, 55, 312, 929,
    /*  696 */ 1058, 1187, 1188, 1317, 1446, 1575, 40, 0, 0, 0, 0, 0, 0, 0, 1721, 30, 30, 30, 30, 30, 30, 30, 30, 0, 28,
    /*  721 */ 29, 312, 0, 0, 55, 0, 0, 0, 0, 0, 29, 55, 312, 0, 0, 256, 0, 0, 0, 0, 0, 50, 0, 0, 32, 0, 0, 35, 0, 0, 0,
    /*  752 */ 40, 0, 0, 2176, 0, 0, 2176, 0, 0, 0, 0, 0, 83, 0, 0, 2176, 0, 0, 28, 29, 0, 0, 0, 93, 0, 0, 0, 0, 28, 0,
    /*  782 */ 55, 312, 2304, 0, 0, 28, 29, 0, 0, 0, 565, 29, 0, 0, 0, 28, 566, 0, 0, 0, 42, 43, 0, 0, 43, 0, 799, 929,
    /*  810 */ 1058, 1188, 1317, 1446, 1575, 50, 0, 0, 28, 29, 0, 799, 928, 0, 91, 0, 0, 0, 0, 0, 0, 72, 73, 0, 0, 81,
    /*  836 */ 0, 0, 0, 0, 0, 88, 0, 0, 0, 0, 2944, 0, 0, 0, 55, 312, 79, 0, 0, 0, 0, 0, 0, 0, 84, 0, 0, 52, 0, 28, 29,
    /*  867 */ 0, 799, 928, 0, 43, 0, 28, 29, 0, 799, 928, 74, 0, 55, 0, 0, 0, 78, 0, 0, 55, 75, 0, 0, 0, 0, 70, 71, 0,
    /*  896 */ 0, 41, 59, 0, 0, 0, 0, 0, 0, 1920, 0, 0, 80, 0, 0, 0, 0, 0, 0, 3456, 0, 86, 0, 0, 0, 0, 0, 0, 0, 3712, 0,
    /*  927 */ 41, 0, 60, 0, 0, 0, 0, 0, 2688, 0, 0, 0, 0, 3072, 0, 0, 0, 0, 89, 45, 0, 45, 48, 48, 51, 48, 48, 51, 48,
    /*  956 */ 0, 28, 29, 0, 799, 928, 0, 47, 0, 49, 49, 0, 49, 49, 0, 49, 0, 28, 29, 0, 799, 928, 41, 0, 0, 0, 62, 0,
    /*  984 */ 0, 62, 0, 0, 0, 55, 312, 0, 0, 68, 69, 0, 0, 0, 0, 82, 0, 0, 0, 41, 0, 0, 0, 0, 0, 64, 0, 0, 67, 0, 0, 0,
    /* 1016 */ 0, 0, 44, 0, 44, 65, 0, 0, 0, 0, 0, 55, 312, 46, 46, 46, 46, 46, 46, 46, 46, 0, 28, 29, 0, 799, 928, 0,
    /* 1044 */ 2432, 0, 0, 0, 0, 55, 312, 0, 2560, 55, 0, 0, 0, 0, 0, 87, 0, 0, 0, 41, 0, 0, 0, 0, 63, 0, 0, 55, 0, 0,
    /* 1074 */ 77, 0, 3584, 0, 3200, 0, 0, 0, 0, 0, 0, 2048, 0, 0, 2048, 90, 0, 92, 0, 0, 0, 0, 0, 2816, 0, 0, 0, 0, 94,
    /* 1103 */ 0, 0, 0, 41, 0, 0, 61, 0, 0, 0, 61, 640, 640, 640, 640, 640, 640, 640, 640, 0, 0, 0, 0, 0, 0, 55, 0, 76,
    /* 1131 */ 0, 0, 0, 3328, 0, 0, 0, 0
  )

  private final val EXPECTED = Array(
    /*  0 */ 14, 38, 70, 134, 262, 518, 1030, 2054, 4102, 8198, 65542, 1048582, 2097158, 16777222, 268451846, 18874374,
    /* 16 */ 142606358, 167772182, 142671894, 226492438, 226525206, 147128342, 228589590, 228622358, 147259414,
    /* 25 */ 229113878, 4, 8, 8, 2, 32, 64, 64, 128, 256, 256, 512, 1024, 2048, 4096, 4096, 1048576, 2097152, 16777216,
    /* 44 */ 8388608, 134217728, 33554432, 25165824, 67108864, 262144, 12582912, 524288, 8, 8, 2, 2, 4096, 4096,
    /* 58 */ 1048576, 2097152, 16777216, 8388608, 134217728, 33554432, 67108864, 524288, 2, 1048576, 2097152, 16777216,
    /* 70 */ 8388608, 134217728, 33554432, 67108864, 1048576, 2097152, 16777216, 8388608, 33554432, 67108864, 1048576,
    /* 81 */ 16777216, 8388608, 33554432, 67108864, 1048576, 67108864, 1048576, 1048576, 1048576, 1048576, 1048576,
    /* 92 */ 1048576, 1048576
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
    "Expansion",
    "'('",
    "')'",
    "'*'",
    "'='",
    "'?'",
    "'as'",
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
