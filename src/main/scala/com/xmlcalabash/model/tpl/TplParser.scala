package com.xmlcalabash.model.tpl

// This file was generated on Sun Aug 27, 2017 17:24 (UTC-05) by REx v5.45 which is Copyright (c) 1979-2017 by Gunther Rademacher <grd@gmx.net>
// REx command line: TplParser.ebnf -tree -scala

// Hacked slightly by norm to change the method name for the main entry point.

import collection.mutable.ArrayBuffer

class TplParser {

  def this(string: String, eh: TplParser.EventHandler) {
    this
    initialize(string, eh)
  }

  def initialize(string: String, eh: TplParser.EventHandler) {
    eventHandler = eh
    input = string
    size = input.length
    reset(0, 0, 0)
  }

  def reset(l: Int, b: Int, e: Int) {
    b0 = b; e0 = b
    l1 = l; b1 = b; e1 = e
    end = e
    eventHandler.reset(input)
  }

  def reset {
    reset(0, 0, 0)
  }

  def parse {
    eventHandler.startNonterminal("Pipeline", e0)
    lookahead1W(8)                  // Comment | WhiteSpace | 'pipeline'
    consume(23)                     // 'pipeline'
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    consume(28)                     // '{'
    var c1 = true
    while (c1) {
      lookahead1W(21)               // Comment | WhiteSpace | StepName | '$' | '[' | 'choose' | 'for-each' | 'group' |
      // 'try' | 'until' | 'while'
      l1 match {
        case 8 =>                     // '$'
          whitespace
          parse_VarBinding
        case _ =>
          whitespace
          parse_Cut
      }
      lookahead1W(22)               // Comment | WhiteSpace | StepName | '$' | '[' | 'choose' | 'for-each' | 'group' |
      // 'try' | 'until' | 'while' | '}'
      if (l1 == 29) {               // '}'
        c1 = false
      }
    }
    consume(29)                     // '}'
    lookahead1W(1)                  // Comment | WhiteSpace | EOF
    consume(5)                      // EOF
    eventHandler.endNonterminal("Pipeline", e0)
  }

  def parse_VarRef {
    eventHandler.startNonterminal("VarRef", e0)
    lookahead1W(3)                  // Comment | WhiteSpace | '$'
    consume(8)                      // '$'
    lookahead1W(2)                  // Comment | WhiteSpace | AnyName
    consume(6)                      // AnyName
    eventHandler.endNonterminal("VarRef", e0)
  }

  private def parse_Cut {
    eventHandler.startNonterminal("Cut", e0)
    parse_Step
    var c1 = true
    while (c1) {
      lookahead1W(23)               // Comment | WhiteSpace | ARR | StepName | '$' | '[' | 'choose' | 'for-each' |
      // 'group' | 'try' | 'until' | 'while' | '}'
      if (l1 != 4) {                // ARR
        c1 = false
      }
      else {
        consume(4)                  // ARR
        lookahead1W(20)             // Comment | WhiteSpace | StepName | '[' | 'choose' | 'for-each' | 'group' | 'try' |
        // 'until' | 'while'
        whitespace
        parse_Step
      }
    }
    eventHandler.endNonterminal("Cut", e0)
  }

  private def parse_Step {
    eventHandler.startNonterminal("Step", e0)
    if (l1 == 16) {                 // '['
      parse_PortMap
    }
    lookahead1W(19)                 // Comment | WhiteSpace | StepName | 'choose' | 'for-each' | 'group' | 'try' |
    // 'until' | 'while'
    l1 match {
      case 7 =>                       // StepName
        whitespace
        parse_AtomicStep
      case _ =>
        whitespace
        parse_CompoundStep
    }
    eventHandler.endNonterminal("Step", e0)
  }

  private def parse_BindingList {
    eventHandler.startNonterminal("BindingList", e0)
    parse_PortBinding
    var c1 = true
    while (c1) {
      lookahead1W(17)               // Comment | WhiteSpace | ',' | ';' | ']'
      if (l1 != 11) {               // ','
        c1 = false
      }
      else {
        consume(11)                 // ','
        lookahead1W(2)              // Comment | WhiteSpace | AnyName
        whitespace
        parse_PortBinding
      }
    }
    eventHandler.endNonterminal("BindingList", e0)
  }

  private def parse_SourceBindingList {
    eventHandler.startNonterminal("SourceBindingList", e0)
    parse_BindingList
    eventHandler.endNonterminal("SourceBindingList", e0)
  }

  private def parse_ResultBindingList {
    eventHandler.startNonterminal("ResultBindingList", e0)
    parse_BindingList
    eventHandler.endNonterminal("ResultBindingList", e0)
  }

  private def parse_PortMap {
    eventHandler.startNonterminal("PortMap", e0)
    consume(16)                     // '['
    lookahead1W(15)                 // Comment | WhiteSpace | AnyName | ';' | ']'
    if (l1 == 6) {                  // AnyName
      whitespace
      parse_SourceBindingList
    }
    if (l1 == 14) {                 // ';'
      consume(14)                   // ';'
      lookahead1W(12)               // Comment | WhiteSpace | AnyName | ']'
      if (l1 == 6) {                // AnyName
        whitespace
        parse_ResultBindingList
      }
    }
    consume(17)                     // ']'
    eventHandler.endNonterminal("PortMap", e0)
  }

  private def parse_PortBinding {
    eventHandler.startNonterminal("PortBinding", e0)
    consume(6)                      // AnyName
    lookahead1W(4)                  // Comment | WhiteSpace | ':'
    consume(12)                     // ':'
    lookahead1W(11)                 // Comment | WhiteSpace | StringLiteral | AnyName
    l1 match {
      case 6 =>                       // AnyName
        consume(6)                    // AnyName
      case _ =>
        consume(3)                    // StringLiteral
    }
    eventHandler.endNonterminal("PortBinding", e0)
  }

  private def parse_AtomicStep {
    eventHandler.startNonterminal("AtomicStep", e0)
    consume(7)                      // StepName
    lookahead1W(24)                 // Comment | WhiteSpace | ARR | StepName | '$' | '(' | '[' | 'choose' | 'for-each' |
    // 'group' | 'try' | 'until' | 'while' | '}'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    eventHandler.endNonterminal("AtomicStep", e0)
  }

  private def parse_CompoundStep {
    eventHandler.startNonterminal("CompoundStep", e0)
    l1 match {
      case 21 =>                      // 'group'
        parse_Group
      case 20 =>                      // 'for-each'
        parse_ForEach
      case 27 =>                      // 'while'
        parse_While
      case 25 =>                      // 'until'
        parse_Until
      case 19 =>                      // 'choose'
        parse_Choose
      case _ =>
        parse_Try
    }
    eventHandler.endNonterminal("CompoundStep", e0)
  }

  private def parse_CompoundBody {
    eventHandler.startNonterminal("CompoundBody", e0)
    consume(28)                     // '{'
    var c1 = true
    while (c1) {
      lookahead1W(21)               // Comment | WhiteSpace | StepName | '$' | '[' | 'choose' | 'for-each' | 'group' |
      // 'try' | 'until' | 'while'
      l1 match {
        case 8 =>                     // '$'
          whitespace
          parse_VarBinding
        case _ =>
          whitespace
          parse_Cut
      }
      lookahead1W(22)               // Comment | WhiteSpace | StepName | '$' | '[' | 'choose' | 'for-each' | 'group' |
      // 'try' | 'until' | 'while' | '}'
      if (l1 == 29) {               // '}'
        c1 = false
      }
    }
    consume(29)                     // '}'
    eventHandler.endNonterminal("CompoundBody", e0)
  }

  private def parse_Group {
    eventHandler.startNonterminal("Group", e0)
    consume(21)                     // 'group'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("Group", e0)
  }

  private def parse_ForEach {
    eventHandler.startNonterminal("ForEach", e0)
    consume(20)                     // 'for-each'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("ForEach", e0)
  }

  private def parse_While {
    eventHandler.startNonterminal("While", e0)
    consume(27)                     // 'while'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("While", e0)
  }

  private def parse_Until {
    eventHandler.startNonterminal("Until", e0)
    consume(25)                     // 'until'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("Until", e0)
  }

  private def parse_Choose {
    eventHandler.startNonterminal("Choose", e0)
    consume(19)                     // 'choose'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    consume(28)                     // '{'
    var c1 = true
    while (c1) {
      lookahead1W(18)               // Comment | WhiteSpace | 'otherwise' | 'when' | '}'
      if (l1 != 26) {               // 'when'
        c1 = false
      }
      else {
        whitespace
        parse_When
      }
    }
    if (l1 == 22) {                 // 'otherwise'
      whitespace
      parse_Otherwise
    }
    lookahead1W(10)                 // Comment | WhiteSpace | '}'
    consume(29)                     // '}'
    eventHandler.endNonterminal("Choose", e0)
  }

  private def parse_When {
    eventHandler.startNonterminal("When", e0)
    consume(26)                     // 'when'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("When", e0)
  }

  private def parse_Otherwise {
    eventHandler.startNonterminal("Otherwise", e0)
    consume(22)                     // 'otherwise'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("Otherwise", e0)
  }

  private def parse_Try {
    eventHandler.startNonterminal("Try", e0)
    consume(24)                     // 'try'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    var c1 = true
    while (c1) {
      lookahead1W(7)                // Comment | WhiteSpace | 'catch'
      whitespace
      parse_Catch
      lookahead1W(25)               // Comment | WhiteSpace | ARR | StepName | '$' | '[' | 'catch' | 'choose' |
      // 'for-each' | 'group' | 'try' | 'until' | 'while' | '}'
      if (l1 != 18) {               // 'catch'
        c1 = false
      }
    }
    eventHandler.endNonterminal("Try", e0)
  }

  private def parse_Catch {
    eventHandler.startNonterminal("Catch", e0)
    consume(18)                     // 'catch'
    lookahead1W(16)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(14)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(9)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("Catch", e0)
  }

  private def parse_Opts {
    eventHandler.startNonterminal("Opts", e0)
    consume(9)                      // '('
    lookahead1W(2)                  // Comment | WhiteSpace | AnyName
    whitespace
    parse_OptionBinding
    var c1 = true
    while (c1) {
      lookahead1W(13)               // Comment | WhiteSpace | ')' | ','
      if (l1 != 11) {               // ','
        c1 = false
      }
      else {
        consume(11)                 // ','
        lookahead1W(2)              // Comment | WhiteSpace | AnyName
        whitespace
        parse_OptionBinding
      }
    }
    consume(10)                     // ')'
    eventHandler.endNonterminal("Opts", e0)
  }

  private def parse_OptionBinding {
    eventHandler.startNonterminal("OptionBinding", e0)
    consume(6)                      // AnyName
    lookahead1W(6)                  // Comment | WhiteSpace | '='
    consume(15)                     // '='
    lookahead1W(0)                  // Comment | WhiteSpace | StringLiteral
    consume(3)                      // StringLiteral
    eventHandler.endNonterminal("OptionBinding", e0)
  }

  private def parse_VarBinding {
    eventHandler.startNonterminal("VarBinding", e0)
    consume(8)                      // '$'
    lookahead1W(2)                  // Comment | WhiteSpace | AnyName
    consume(6)                      // AnyName
    lookahead1W(5)                  // Comment | WhiteSpace | ':='
    consume(13)                     // ':='
    lookahead1W(0)                  // Comment | WhiteSpace | StringLiteral
    consume(3)                      // StringLiteral
    eventHandler.endNonterminal("VarBinding", e0)
  }

  private def consume(t: Int) {
    if (l1 == t) {
      whitespace
      eventHandler.terminal(TplParser.TOKEN(l1), b1, e1)
      b0 = b1; e0 = e1; l1 = 0
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

  def getErrorMessage(e: TplParser.ParseException) = {
    val tokenSet = TplParser.getExpectedTokenSet(e)
    val found = TplParser.getOffendingToken(e)
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
    throw new TplParser.ParseException(b, e, s, l, t)
  }

  private def matcher(tokenSetId: Int) = {
    begin = end
    var current = end
    var result = TplParser.INITIAL(tokenSetId)
    var state = 0
    var code = result & 127

    while (code != 0) {
      var charclass = -1
      var c0 = if (current < size) input(current) else 0
      current += 1
      if (c0 < 0x80) {
        charclass = TplParser.MAP0(c0)
      }
      else if (c0 < 0xd800) {
        val c1 = c0 >> 4
        charclass = TplParser.MAP1((c0 & 15) + TplParser.MAP1((c1 & 31) + TplParser.MAP1(c1 >> 5)))
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
          if (TplParser.MAP2(m) > c0) hi = m - 1
          else if (TplParser.MAP2(6 + m) < c0) lo = m + 1
          else charclass = TplParser.MAP2(12 + m)
          if (lo > hi) charclass = 0 else m = (hi + lo) >> 1
        }
      }

      state = code
      val i0 = (charclass << 7) + code - 1
      code = TplParser.TRANSITION((i0 & 7) + TplParser.TRANSITION(i0 >> 3))

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
  var b0 = 0
  var e0 = 0
  var l1 = 0
  var b1 = 0
  var e1 = 0
  var eventHandler: TplParser.EventHandler = null
}

object TplParser {

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
    while (i < 30) {
      var j = i
      val i0 = (i >> 5) * 109 + s - 1
      var f = EXPECTED((i0 & 15) + EXPECTED(i0 >> 4))
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
    /*   0 */ 44, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7,
    /*  35 */ 6, 8, 6, 6, 9, 10, 11, 12, 6, 13, 14, 15, 16, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 17, 18, 6, 19, 20,
    /*  63 */ 6, 6, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
    /*  90 */ 21, 22, 6, 23, 6, 21, 6, 24, 21, 25, 21, 26, 27, 28, 29, 30, 21, 21, 31, 21, 32, 33, 34, 21, 35, 36, 37,
    /* 117 */ 38, 21, 39, 21, 40, 21, 41, 6, 42, 6, 6
  )

  private final val MAP1 = Array(
    /*   0 */ 108, 124, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 156, 182, 182, 182, 182,
    /*  21 */ 182, 215, 216, 214, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215,
    /*  42 */ 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215,
    /*  63 */ 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215,
    /*  84 */ 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215,
    /* 105 */ 215, 215, 215, 248, 262, 278, 401, 323, 294, 340, 356, 378, 378, 378, 370, 324, 316, 324, 316, 324, 324,
    /* 126 */ 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 395, 395, 395, 395, 395, 395, 395,
    /* 147 */ 309, 324, 324, 324, 324, 324, 324, 324, 324, 421, 378, 378, 379, 377, 378, 378, 324, 324, 324, 324, 324,
    /* 168 */ 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 417, 378, 378, 378, 378, 378, 378, 378,
    /* 189 */ 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378, 378,
    /* 210 */ 378, 378, 378, 378, 323, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324,
    /* 231 */ 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 324, 378, 44, 1, 1, 1, 1, 1, 1,
    /* 255 */ 1, 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7, 6, 8, 6, 6, 9, 10, 11, 12,
    /* 289 */ 6, 13, 14, 15, 16, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 22, 6, 23, 6, 21, 21, 21, 21, 21, 21, 21,
    /* 316 */ 21, 21, 21, 21, 21, 21, 21, 6, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 6, 24, 21,
    /* 343 */ 25, 21, 26, 27, 28, 29, 30, 21, 21, 31, 21, 32, 33, 34, 21, 35, 36, 37, 38, 21, 39, 21, 40, 21, 41, 6, 42,
    /* 370 */ 6, 6, 6, 6, 6, 6, 6, 15, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 15, 15, 15, 15, 15, 15, 15, 15,
    /* 402 */ 15, 15, 15, 15, 15, 15, 15, 15, 15, 17, 18, 6, 19, 20, 6, 6, 6, 43, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
    /* 433 */ 21, 21, 6, 6
  )

  private final val MAP2 = Array(
    /*  0 */ 57344, 63744, 64976, 65008, 65536, 983040, 63743, 64975, 65007, 65533, 983039, 1114111, 6, 21, 6, 21, 21,
    /* 17 */ 6
  )

  private final val INITIAL = Array(
    /*  0 */ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26
  )

  private final val TRANSITION = Array(
    /*    0 */ 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734,
    /*   21 */ 734, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 720, 720, 720, 743, 734, 734, 1269, 734, 1231,
    /*   41 */ 734, 734, 734, 734, 734, 734, 734, 720, 720, 720, 725, 734, 734, 1231, 734, 1231, 734, 734, 734, 734,
    /*   61 */ 734, 734, 734, 720, 720, 720, 761, 734, 734, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 771,
    /*   81 */ 771, 771, 777, 734, 734, 1231, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 748, 734,
    /*  101 */ 734, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 733, 730, 734, 941, 734, 735, 1269, 734, 1231,
    /*  121 */ 734, 734, 734, 734, 734, 734, 734, 1523, 734, 815, 790, 734, 734, 1269, 734, 1231, 734, 734, 734, 734,
    /*  141 */ 734, 734, 734, 803, 800, 734, 795, 734, 734, 812, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 734, 734,
    /*  162 */ 823, 832, 734, 734, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 734, 848, 734, 748, 734, 734,
    /*  182 */ 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 734, 734, 734, 946, 734, 734, 845, 734, 856, 734,
    /*  202 */ 734, 734, 734, 734, 734, 734, 734, 859, 867, 748, 734, 734, 1269, 734, 1231, 734, 734, 734, 734, 734,
    /*  222 */ 734, 734, 734, 734, 804, 879, 1435, 1244, 1311, 901, 1433, 900, 1436, 1056, 1058, 1482, 734, 734, 734,
    /*  241 */ 734, 734, 782, 1435, 1244, 1311, 901, 1433, 901, 1436, 1056, 1058, 1482, 734, 734, 910, 910, 910, 916,
    /*  260 */ 734, 734, 1269, 734, 936, 734, 734, 734, 734, 734, 734, 734, 1527, 734, 734, 748, 734, 734, 1269, 734,
    /*  280 */ 1231, 734, 734, 734, 734, 734, 734, 734, 734, 1497, 954, 748, 734, 734, 1269, 734, 1231, 734, 734, 734,
    /*  300 */ 734, 734, 734, 734, 1233, 734, 734, 753, 734, 734, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734,
    /*  320 */ 734, 734, 734, 748, 734, 1272, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 964, 959, 1436, 1025,
    /*  340 */ 1435, 1244, 1311, 901, 1433, 901, 1436, 1056, 1058, 1482, 734, 734, 734, 763, 977, 983, 734, 734, 1269,
    /*  359 */ 734, 1231, 734, 734, 734, 734, 734, 734, 734, 734, 871, 996, 748, 734, 734, 1269, 734, 1231, 734, 734,
    /*  379 */ 734, 734, 734, 734, 734, 964, 959, 1436, 1025, 1014, 1283, 1311, 901, 1433, 901, 1436, 1126, 1058, 1482,
    /*  398 */ 734, 734, 1296, 959, 1404, 1033, 1435, 1244, 1311, 901, 1426, 1471, 1436, 1056, 1020, 1482, 734, 734,
    /*  416 */ 964, 959, 1436, 1025, 1435, 1244, 1342, 902, 1394, 1052, 988, 1066, 1254, 1399, 734, 734, 964, 959, 1165,
    /*  435 */ 1087, 1435, 1244, 1311, 901, 1433, 901, 1436, 1056, 1058, 1482, 734, 734, 964, 959, 1372, 1102, 1435,
    /*  453 */ 1244, 1311, 901, 1433, 901, 1436, 1056, 1058, 1482, 734, 734, 964, 959, 1436, 1025, 837, 1079, 1387,
    /*  471 */ 1122, 1433, 901, 1134, 1146, 1058, 1160, 734, 734, 964, 959, 1436, 1025, 1173, 1185, 1311, 1359, 1433,
    /*  489 */ 1211, 1436, 1177, 1224, 1482, 734, 734, 964, 959, 1436, 1025, 1435, 1244, 1311, 901, 1433, 1216, 1241,
    /*  507 */ 1252, 1058, 1482, 734, 734, 964, 959, 1436, 1025, 1435, 1262, 1311, 901, 1073, 1044, 1436, 1056, 1152,
    /*  525 */ 1482, 734, 734, 964, 959, 922, 928, 1314, 1244, 1311, 1280, 1349, 901, 1436, 1056, 1058, 1482, 734, 734,
    /*  544 */ 964, 1291, 1114, 1304, 1435, 1244, 1419, 1322, 1433, 901, 969, 1056, 1058, 1482, 734, 734, 964, 959,
    /*  562 */ 1436, 1025, 1446, 1335, 1311, 1357, 1433, 901, 1094, 1056, 1058, 1482, 734, 734, 964, 959, 1436, 1025,
    /*  580 */ 1435, 1244, 1311, 901, 1433, 901, 1001, 1056, 1058, 1367, 734, 734, 964, 959, 1203, 1380, 1040, 1412,
    /*  598 */ 1461, 1006, 1444, 901, 1436, 1056, 1058, 1482, 734, 734, 964, 959, 1510, 1454, 1435, 1244, 1311, 901,
    /*  616 */ 1433, 1469, 1436, 1056, 1058, 1482, 734, 734, 964, 959, 886, 892, 1435, 1244, 1311, 901, 1433, 901, 1436,
    /*  635 */ 1138, 1479, 1482, 734, 734, 964, 959, 1436, 1025, 1435, 1244, 1311, 1327, 1433, 901, 1436, 1056, 1058,
    /*  653 */ 1482, 734, 734, 734, 1490, 1496, 748, 734, 734, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 734,
    /*  673 */ 1109, 1192, 1198, 734, 734, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 734, 734, 824, 1505, 734,
    /*  693 */ 734, 1269, 734, 1231, 734, 734, 734, 734, 734, 734, 734, 1518, 734, 734, 734, 734, 734, 734, 734, 734,
    /*  713 */ 734, 734, 734, 734, 734, 734, 734, 384, 384, 384, 384, 384, 384, 384, 384, 28, 29, 0, 0, 0, 28, 0, 0, 0,
    /*  737 */ 0, 0, 0, 0, 0, 28, 384, 384, 0, 28, 29, 0, 0, 0, 28, 29, 0, 0, 0, 28, 29, 0, 0, 1792, 384, 384, 0, 0, 0,
    /*  766 */ 0, 0, 0, 2176, 0, 411, 411, 411, 411, 411, 411, 411, 411, 0, 28, 29, 0, 0, 0, 28, 29, 0, 927, 0, 1152,
    /*  791 */ 1152, 0, 28, 29, 0, 0, 0, 28, 561, 0, 0, 0, 29, 0, 0, 0, 0, 0, 0, 0, 46, 29, 50, 307, 0, 0, 0, 0, 0,
    /*  820 */ 1152, 1152, 1152, 1280, 0, 0, 0, 0, 0, 0, 0, 640, 1280, 0, 0, 28, 29, 0, 0, 0, 55, 1061, 1081, 1061,
    /*  844 */ 1061, 0, 66, 307, 0, 0, 0, 0, 0, 1408, 0, 0, 0, 66, 0, 0, 0, 0, 0, 0, 1536, 0, 0, 0, 1536, 0, 0, 0, 0, 0,
    /*  874 */ 0, 2304, 0, 0, 2304, 46, 46, 0, 28, 29, 0, 927, 0, 0, 36, 1069, 1069, 1069, 1069, 1069, 0, 28, 29, 0,
    /*  898 */ 927, 0, 1110, 1061, 1061, 1061, 1061, 1061, 1061, 1061, 1061, 1102, 30, 30, 30, 30, 30, 30, 30, 30, 0,
    /*  919 */ 28, 29, 307, 0, 0, 35, 1065, 1065, 1065, 1065, 1065, 0, 28, 29, 0, 927, 0, 0, 256, 0, 0, 0, 0, 0, 0, 560,
    /*  945 */ 29, 0, 0, 0, 28, 29, 50, 0, 0, 0, 1920, 0, 0, 0, 0, 0, 0, 927, 927, 0, 0, 927, 0, 0, 0, 0, 0, 1061, 1061,
    /*  974 */ 1061, 2853, 1061, 2176, 0, 0, 0, 2176, 2176, 2176, 2176, 0, 28, 29, 0, 0, 0, 1061, 1061, 1120, 1061,
    /*  995 */ 1061, 0, 2304, 0, 0, 0, 0, 0, 0, 1061, 1119, 1061, 1061, 1061, 1061, 1061, 1061, 1101, 1061, 52, 0, 0, 0,
    /* 1018 */ 1061, 1080, 1061, 1061, 0, 0, 1130, 1061, 1061, 0, 28, 29, 0, 927, 0, 1062, 1071, 0, 28, 29, 0, 927, 0,
    /* 1041 */ 0, 54, 0, 1061, 1061, 1061, 1061, 1061, 37, 1061, 1061, 1061, 1061, 1112, 1113, 1061, 1061, 1061, 1061,
    /* 1060 */ 0, 0, 1061, 1061, 1061, 0, 1061, 1061, 3621, 1061, 0, 0, 2597, 1061, 50, 0, 0, 0, 3456, 1061, 1061, 1061,
    /* 1082 */ 1061, 1088, 0, 1081, 0, 1063, 1063, 0, 28, 29, 0, 927, 0, 0, 94, 1061, 1061, 1061, 1061, 1121, 1064,
    /* 1103 */ 1064, 0, 28, 29, 0, 927, 0, 0, 3840, 0, 0, 0, 0, 0, 1066, 1066, 1066, 1066, 1066, 1061, 1061, 1061, 1099,
    /* 1126 */ 1061, 1061, 1061, 1061, 0, 0, 1061, 1125, 2432, 0, 0, 37, 1061, 1061, 1061, 1061, 0, 100, 1061, 1061,
    /* 1146 */ 1061, 1061, 1061, 2469, 0, 0, 1061, 1061, 104, 0, 1061, 1061, 1132, 0, 0, 2725, 1061, 1061, 0, 0, 0, 0,
    /* 1168 */ 1063, 1063, 1063, 1063, 1063, 0, 53, 0, 0, 1061, 1061, 1061, 1061, 99, 0, 1061, 1061, 1061, 1085, 1061,
    /* 1188 */ 1061, 1061, 0, 1061, 0, 0, 3840, 0, 0, 0, 3840, 3840, 0, 28, 29, 0, 0, 0, 1067, 1067, 1067, 1067, 1067,
    /* 1211 */ 1061, 1061, 1061, 1061, 1114, 1061, 1061, 1061, 1061, 1061, 1061, 1115, 1061, 1061, 1127, 0, 105, 1061,
    /* 1229 */ 1131, 1061, 0, 50, 0, 0, 0, 0, 0, 0, 2048, 0, 0, 93, 0, 1061, 1061, 1061, 1061, 1061, 0, 1061, 0, 1122,
    /* 1253 */ 3365, 1061, 1061, 0, 0, 1061, 1061, 1061, 3072, 1061, 1061, 1061, 1087, 1061, 0, 1061, 0, 50, 307, 0, 0,
    /* 1274 */ 0, 0, 0, 640, 0, 0, 1096, 1061, 1098, 1061, 1061, 1061, 1061, 1061, 0, 1089, 0, 34, 0, 0, 927, 927, 0, 0,
    /* 1298 */ 927, 0, 0, 0, 0, 33, 1066, 1066, 0, 28, 29, 0, 927, 0, 50, 307, 0, 0, 0, 0, 1061, 1061, 1082, 1061, 1061,
    /* 1323 */ 1061, 1061, 1061, 1100, 1061, 1061, 1061, 1061, 1061, 3237, 1061, 1061, 1061, 1061, 1086, 1061, 1061, 0,
    /* 1341 */ 1061, 0, 50, 307, 0, 0, 0, 70, 1061, 50, 0, 0, 0, 0, 1061, 1109, 1061, 1097, 1061, 1061, 1061, 1061,
    /* 1363 */ 1061, 1061, 1061, 1103, 109, 1061, 1132, 1061, 0, 0, 0, 0, 1064, 1064, 1064, 1064, 1064, 1067, 1067, 0,
    /* 1383 */ 28, 29, 0, 927, 0, 50, 307, 0, 0, 69, 0, 1061, 50, 0, 82, 83, 0, 1061, 1061, 37, 2944, 0, 0, 0, 1062,
    /* 1408 */ 1062, 1062, 1062, 1062, 1084, 1061, 1061, 1061, 1061, 0, 1061, 0, 50, 307, 0, 68, 0, 0, 1061, 50, 81, 0,
    /* 1430 */ 0, 0, 1108, 1061, 50, 0, 0, 0, 0, 1061, 1061, 1061, 1061, 1061, 1104, 50, 0, 0, 0, 0, 1061, 1061, 1061,
    /* 1453 */ 1083, 1068, 1068, 0, 28, 29, 0, 927, 0, 50, 307, 67, 0, 0, 0, 1095, 1061, 1111, 1061, 1061, 1061, 1061,
    /* 1475 */ 1061, 1061, 1061, 1116, 1126, 1061, 0, 0, 1061, 1061, 1061, 0, 0, 0, 0, 0, 3712, 0, 0, 0, 0, 3712, 0, 0,
    /* 1499 */ 0, 0, 0, 0, 0, 1920, 640, 640, 0, 28, 29, 0, 0, 0, 1068, 1068, 1068, 1068, 1068, 0, 768, 0, 0, 0, 0, 0,
    /* 1525 */ 0, 1152, 0, 0, 0, 0, 1664, 32, 0, 0
  )

  private final val EXPECTED = Array(
    /*   0 */ 7, 23, 42, 39, 58, 74, 90, 14, 38, 70, 262, 4102, 8198, 32774, 262150, 8388614, 268435462, 536870918, 78,
    /*  19 */ 131142, 3078, 268500998, 147526, 268501510, 149510, 608174086, 188219526, 188285062, 188285318, 725156230,
    /*  30 */ 725156246, 725156758, 725418390, 4, 8, 8, 2, 64, 8192, 8, 2, 2, 262144, 8388608, 4194304, 67108864, 128,
    /*  47 */ 524416, 1048704, 2097280, 128, 128, 16777344, 33554560, 134217856, 16, 786560, 8, 262272, 2, 262144,
    /*  61 */ 8388608, 4194304, 67108864, 128, 524416, 1048704, 2097280, 128, 128, 33554560, 128, 134217856, 262272,
    /*  74 */ 262144, 8388608, 4194304, 128, 524416, 1048704, 2097280, 128, 128, 33554560, 134217856, 262272, 8388608,
    /*  87 */ 4194304, 524416, 1048704, 128, 128, 8388608, 4194304, 1048704, 128, 128, 8388608, 4194304, 1048704, 128,
    /* 101 */ 128, 4194304
  )

  private final val TOKEN = Array(
    "(0)",
    "Comment",
    "WhiteSpace",
    "StringLiteral",
    "ARR",
    "EOF",
    "AnyName",
    "StepName",
    "'$'",
    "'('",
    "')'",
    "','",
    "':'",
    "':='",
    "';'",
    "'='",
    "'['",
    "']'",
    "'catch'",
    "'choose'",
    "'for-each'",
    "'group'",
    "'otherwise'",
    "'pipeline'",
    "'try'",
    "'until'",
    "'when'",
    "'while'",
    "'{'",
    "'}'"
  )
}

// End
