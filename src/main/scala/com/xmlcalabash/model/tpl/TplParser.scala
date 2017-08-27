package com.xmlcalabash.model.tpl

// This file was generated on Sun Aug 27, 2017 08:48 (UTC-05) by REx v5.45 which is Copyright (c) 1979-2017 by Gunther Rademacher <grd@gmx.net>
// REx command line: TplParser.ebnf -scala

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
    lookahead1W(6)                  // Comment | WhiteSpace | 'pipeline'
    consume(23)                     // 'pipeline'
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    consume(28)                     // '{'
    var c1 = true
    while (c1) {
      lookahead1W(25)               // Comment | WhiteSpace | StepName | '$' | '[' | 'choose' | 'for-each' | 'group' |
      // 'try' | 'until' | 'while'
      l1 match {
        case 8 =>                     // '$'
          whitespace
          parse_VarBinding
        case _ =>
          whitespace
          parse_Cut
      }
      lookahead1W(26)               // Comment | WhiteSpace | StepName | '$' | '[' | 'choose' | 'for-each' | 'group' |
      // 'try' | 'until' | 'while' | '}'
      if (l1 == 29) {               // '}'
        c1 = false
      }
    }
    consume(29)                     // '}'
    lookahead1W(0)                  // Comment | WhiteSpace | EOF
    consume(5)                      // EOF
    eventHandler.endNonterminal("Pipeline", e0)
  }

  private def parse_Cut {
    eventHandler.startNonterminal("Cut", e0)
    parse_Step
    var c1 = true
    while (c1) {
      lookahead1W(27)               // Comment | WhiteSpace | ARR | StepName | '$' | '[' | 'choose' | 'for-each' |
      // 'group' | 'try' | 'until' | 'while' | '}'
      if (l1 != 4) {                // ARR
        c1 = false
      }
      else {
        consume(4)                  // ARR
        lookahead1W(24)             // Comment | WhiteSpace | StepName | '[' | 'choose' | 'for-each' | 'group' | 'try' |
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
    lookahead1W(23)                 // Comment | WhiteSpace | StepName | 'choose' | 'for-each' | 'group' | 'try' |
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
      lookahead1W(21)               // Comment | WhiteSpace | ',' | ';' | ']'
      if (l1 != 11) {               // ','
        c1 = false
      }
      else {
        consume(11)                 // ','
        lookahead1W(1)              // Comment | WhiteSpace | AnyName
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
    lookahead1W(19)                 // Comment | WhiteSpace | AnyName | ';' | ']'
    if (l1 == 6) {                  // AnyName
      whitespace
      parse_SourceBindingList
    }
    if (l1 == 14) {                 // ';'
      consume(14)                   // ';'
      lookahead1W(13)               // Comment | WhiteSpace | AnyName | ']'
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
    lookahead1W(2)                  // Comment | WhiteSpace | ':'
    consume(12)                     // ':'
    lookahead1W(9)                  // Comment | WhiteSpace | StringLiteral | AnyName
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
    lookahead1W(28)                 // Comment | WhiteSpace | ARR | StepName | '$' | '(' | '[' | 'choose' | 'for-each' |
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
      lookahead1W(25)               // Comment | WhiteSpace | StepName | '$' | '[' | 'choose' | 'for-each' | 'group' |
      // 'try' | 'until' | 'while'
      l1 match {
        case 8 =>                     // '$'
          whitespace
          parse_VarBinding
        case _ =>
          whitespace
          parse_Cut
      }
      lookahead1W(26)               // Comment | WhiteSpace | StepName | '$' | '[' | 'choose' | 'for-each' | 'group' |
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
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("Group", e0)
  }

  private def parse_ForEach {
    eventHandler.startNonterminal("ForEach", e0)
    consume(20)                     // 'for-each'
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("ForEach", e0)
  }

  private def parse_While {
    eventHandler.startNonterminal("While", e0)
    consume(27)                     // 'while'
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("While", e0)
  }

  private def parse_Until {
    eventHandler.startNonterminal("Until", e0)
    consume(25)                     // 'until'
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("Until", e0)
  }

  private def parse_Choose {
    eventHandler.startNonterminal("Choose", e0)
    consume(19)                     // 'choose'
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    consume(28)                     // '{'
    var c1 = true
    while (c1) {
      lookahead1W(22)               // Comment | WhiteSpace | 'otherwise' | 'when' | '}'
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
    lookahead1W(8)                  // Comment | WhiteSpace | '}'
    consume(29)                     // '}'
    eventHandler.endNonterminal("Choose", e0)
  }

  private def parse_When {
    eventHandler.startNonterminal("When", e0)
    consume(26)                     // 'when'
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("When", e0)
  }

  private def parse_Otherwise {
    eventHandler.startNonterminal("Otherwise", e0)
    consume(22)                     // 'otherwise'
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("Otherwise", e0)
  }

  private def parse_Try {
    eventHandler.startNonterminal("Try", e0)
    consume(24)                     // 'try'
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    var c1 = true
    while (c1) {
      lookahead1W(5)                // Comment | WhiteSpace | 'catch'
      whitespace
      parse_Catch
      lookahead1W(29)               // Comment | WhiteSpace | ARR | StepName | '$' | '[' | 'catch' | 'choose' |
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
    lookahead1W(20)                 // Comment | WhiteSpace | '(' | '[' | '{'
    if (l1 == 9) {                  // '('
      whitespace
      parse_Opts
    }
    lookahead1W(16)                 // Comment | WhiteSpace | '[' | '{'
    if (l1 == 16) {                 // '['
      whitespace
      parse_PortMap
    }
    lookahead1W(7)                  // Comment | WhiteSpace | '{'
    whitespace
    parse_CompoundBody
    eventHandler.endNonterminal("Catch", e0)
  }

  private def parse_Opts {
    eventHandler.startNonterminal("Opts", e0)
    consume(9)                      // '('
    lookahead1W(12)                 // Comment | WhiteSpace | AnyName | ')'
    if (l1 == 6) {                  // AnyName
      whitespace
      parse_OptionBindings
    }
    consume(10)                     // ')'
    eventHandler.endNonterminal("Opts", e0)
  }

  private def parse_OptionBindings {
    eventHandler.startNonterminal("OptionBindings", e0)
    parse_OptionBinding
    var c1 = true
    while (c1) {
      lookahead1W(14)               // Comment | WhiteSpace | ')' | ','
      if (l1 != 11) {               // ','
        c1 = false
      }
      else {
        consume(11)                 // ','
        lookahead1W(1)              // Comment | WhiteSpace | AnyName
        whitespace
        parse_OptionBinding
      }
    }
    eventHandler.endNonterminal("OptionBindings", e0)
  }

  private def parse_OptionBinding {
    eventHandler.startNonterminal("OptionBinding", e0)
    consume(6)                      // AnyName
    lookahead1W(4)                  // Comment | WhiteSpace | '='
    consume(15)                     // '='
    lookahead1W(17)                 // Comment | WhiteSpace | StringLiteral | '$' | '['
    l1 match {
      case 16 =>                      // '['
        whitespace
        parse_StringArray
      case _ =>
        whitespace
        parse_VarOrString
    }
    eventHandler.endNonterminal("OptionBinding", e0)
  }

  private def parse_VarOrString {
    eventHandler.startNonterminal("VarOrString", e0)
    l1 match {
      case 8 =>                       // '$'
        parse_VarRef
      case _ =>
        consume(3)                    // StringLiteral
    }
    eventHandler.endNonterminal("VarOrString", e0)
  }

  private def parse_VarBinding {
    eventHandler.startNonterminal("VarBinding", e0)
    consume(8)                      // '$'
    lookahead1W(1)                  // Comment | WhiteSpace | AnyName
    consume(6)                      // AnyName
    lookahead1W(3)                  // Comment | WhiteSpace | ':='
    consume(13)                     // ':='
    lookahead1W(11)                 // Comment | WhiteSpace | StringLiteral | '['
    l1 match {
      case 3 =>                       // StringLiteral
        consume(3)                    // StringLiteral
      case _ =>
        whitespace
        parse_StringArray
    }
    eventHandler.endNonterminal("VarBinding", e0)
  }

  private def parse_VarRef {
    eventHandler.startNonterminal("VarRef", e0)
    consume(8)                      // '$'
    lookahead1W(1)                  // Comment | WhiteSpace | AnyName
    consume(6)                      // AnyName
    eventHandler.endNonterminal("VarRef", e0)
  }

  private def parse_StringArray {
    eventHandler.startNonterminal("StringArray", e0)
    consume(16)                     // '['
    lookahead1W(18)                 // Comment | WhiteSpace | StringLiteral | '$' | ']'
    if (l1 != 17) {                 // ']'
      whitespace
      parse_VarOrString
      var c1 = true
      while (c1) {
        lookahead1W(15)             // Comment | WhiteSpace | ',' | ']'
        if (l1 != 11) {             // ','
          c1 = false
        }
        else {
          consume(11)               // ','
          lookahead1W(10)           // Comment | WhiteSpace | StringLiteral | '$'
          whitespace
          parse_VarOrString
        }
      }
    }
    consume(17)                     // ']'
    eventHandler.endNonterminal("StringArray", e0)
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
      val i0 = (i >> 5) * 113 + s - 1
      var f = EXPECTED((i0 & 7) + EXPECTED(i0 >> 3))
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
    /*  0 */ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
    /* 29 */ 30
  )

  private final val TRANSITION = Array(
    /*    0 */ 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343,
    /*   17 */ 1343, 1343, 1343, 1343, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 720, 720, 720,
    /*   35 */ 722, 1444, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 720, 720, 720, 721, 1444,
    /*   53 */ 1343, 1163, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 720, 720, 720, 722, 1343, 1343, 957,
    /*   71 */ 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 730, 730, 730, 732, 1444, 1343, 1163, 1343, 1161,
    /*   89 */ 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1444, 1343, 957, 1343, 1161, 1343,
    /*  106 */ 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1232, 1158, 1343, 1088, 1343, 794, 1343, 1161, 1343, 1343,
    /*  123 */ 1343, 1343, 1343, 1343, 1343, 1343, 1322, 740, 751, 1444, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343,
    /*  141 */ 1343, 1343, 1343, 1343, 1440, 1340, 1343, 1362, 1343, 957, 1342, 1161, 1343, 1343, 1343, 1343, 1343,
    /*  158 */ 1343, 1343, 1343, 1343, 962, 962, 1444, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343,
    /*  176 */ 1343, 967, 1343, 1343, 1444, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343,
    /*  194 */ 1343, 1344, 1444, 1343, 868, 1343, 1325, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 804, 1488, 1343,
    /*  212 */ 1444, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 878, 817, 1307,
    /*  230 */ 952, 1476, 1273, 986, 1404, 1307, 1402, 1399, 1343, 1343, 1343, 1343, 1343, 1343, 817, 1307, 952, 1476,
    /*  248 */ 1273, 1477, 1404, 1307, 1402, 1399, 1343, 1343, 765, 765, 765, 767, 1444, 1343, 957, 1343, 743, 1343,
    /*  266 */ 1343, 1343, 1343, 1343, 1343, 1343, 1358, 1343, 1343, 1343, 1444, 1343, 957, 1343, 1161, 1343, 1343,
    /*  283 */ 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1033, 1343, 1444, 1343, 957, 1343, 1161, 1343, 1343, 1343,
    /*  300 */ 1343, 1343, 1343, 1343, 873, 1343, 1343, 1343, 775, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343,
    /*  318 */ 1343, 1343, 1343, 1343, 1343, 1343, 1444, 1343, 789, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343,
    /*  335 */ 1343, 802, 812, 1472, 853, 817, 1307, 952, 1476, 1273, 1477, 1404, 1307, 1402, 1399, 1343, 1343, 1343,
    /*  353 */ 1084, 829, 825, 1444, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1236, 1376,
    /*  371 */ 1343, 1444, 1343, 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 802, 812, 1472, 853, 837,
    /*  389 */ 851, 863, 1476, 1273, 1477, 1404, 1307, 1250, 1399, 1343, 1343, 886, 812, 757, 900, 817, 1307, 952, 1476,
    /*  408 */ 1105, 1270, 914, 1307, 1402, 926, 1343, 1343, 802, 812, 1472, 853, 817, 1307, 952, 1102, 1019, 937, 1404,
    /*  427 */ 945, 1396, 975, 999, 1343, 802, 812, 1039, 1008, 817, 1307, 952, 1476, 1273, 1477, 1404, 1307, 1402,
    /*  445 */ 1399, 1343, 1343, 802, 812, 781, 1027, 817, 1307, 952, 1476, 1273, 1477, 1404, 1307, 1402, 1399, 1343,
    /*  463 */ 1343, 802, 812, 1472, 853, 1047, 1055, 1065, 1459, 1273, 1477, 1285, 1057, 1402, 991, 1343, 1343, 802,
    /*  481 */ 812, 1472, 853, 1078, 1174, 952, 1476, 1096, 1477, 1113, 1307, 1125, 1133, 1343, 1343, 802, 812, 1472,
    /*  499 */ 853, 817, 1307, 952, 1476, 1273, 1477, 1144, 1205, 1402, 1399, 1343, 1343, 802, 812, 1472, 853, 817,
    /*  517 */ 1308, 952, 1476, 1273, 1171, 981, 1307, 929, 1182, 1343, 1343, 802, 812, 892, 1195, 817, 1213, 952, 1117,
    /*  536 */ 1273, 1201, 1404, 1307, 1402, 1399, 1343, 1343, 1225, 812, 1382, 1244, 817, 1307, 1151, 1476, 1264, 1477,
    /*  554 */ 1404, 1281, 1402, 1399, 1343, 1343, 802, 812, 1472, 853, 817, 1298, 952, 918, 1273, 1477, 855, 1293,
    /*  572 */ 1402, 1399, 1343, 1343, 802, 812, 1472, 853, 817, 1307, 952, 1476, 1273, 1477, 1404, 1306, 1402, 1187,
    /*  590 */ 1343, 1343, 802, 812, 843, 1316, 1333, 1217, 1352, 1014, 1370, 1477, 1404, 1307, 1402, 1399, 1343, 1343,
    /*  608 */ 802, 812, 1256, 1390, 817, 1307, 952, 1476, 1273, 1136, 1404, 1307, 1402, 1399, 1343, 1343, 802, 812,
    /*  626 */ 906, 1412, 817, 1307, 952, 1476, 1273, 1477, 1404, 1307, 1433, 1399, 1343, 1343, 802, 812, 1472, 853,
    /*  644 */ 817, 1307, 952, 1476, 1452, 1477, 1404, 1307, 1402, 1399, 1343, 1343, 1000, 1343, 1467, 1343, 1444, 1343,
    /*  662 */ 957, 1343, 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1423, 1425, 1418, 1444, 1343, 957, 1343,
    /*  680 */ 1161, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1070, 1444, 1343, 957, 1343, 1161,
    /*  697 */ 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1485, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343, 1343,
    /*  714 */ 1343, 1343, 1343, 1343, 1343, 1343, 384, 384, 384, 384, 384, 384, 384, 384, 0, 0, 415, 415, 415, 415,
    /*  734 */ 415, 415, 415, 415, 0, 0, 0, 1152, 1152, 0, 0, 0, 0, 0, 256, 0, 0, 0, 1152, 1152, 1152, 1152, 1152, 0, 0,
    /*  759 */ 0, 929, 0, 0, 0, 1066, 32, 32, 32, 32, 32, 32, 32, 32, 0, 309, 0, 1792, 0, 0, 37, 38, 0, 0, 0, 929, 0, 0,
    /*  787 */ 0, 1068, 0, 640, 0, 52, 309, 0, 0, 0, 52, 309, 0, 0, 37, 0, 929, 0, 0, 0, 0, 0, 0, 1536, 1536, 0, 929, 0,
    /*  815 */ 0, 929, 929, 0, 0, 0, 37, 38, 0, 0, 2176, 2176, 2176, 2176, 2176, 2176, 0, 0, 2176, 0, 0, 0, 929, 0, 54,
    /*  840 */ 0, 37, 38, 0, 0, 0, 929, 0, 0, 0, 1071, 1065, 1084, 1065, 1065, 1065, 1065, 1065, 1065, 0, 0, 98, 1065,
    /*  863 */ 1065, 0, 1093, 52, 309, 0, 0, 0, 70, 309, 0, 0, 0, 0, 2048, 0, 0, 0, 50, 50, 50, 0, 0, 0, 929, 0, 0, 0,
    /*  891 */ 35, 0, 0, 0, 929, 0, 0, 39, 1069, 1066, 1066, 1066, 1066, 1066, 1075, 0, 0, 0, 929, 0, 0, 40, 1073, 1065,
    /*  915 */ 1065, 1065, 1120, 0, 0, 0, 1065, 1065, 1101, 1065, 1065, 1134, 1065, 1065, 0, 0, 1065, 1065, 1065, 1065,
    /*  935 */ 108, 0, 87, 0, 1065, 1065, 1065, 1065, 1116, 1117, 1065, 1124, 1065, 1065, 1065, 1065, 3625, 1065, 0,
    /*  954 */ 1065, 52, 309, 0, 0, 0, 52, 309, 0, 0, 0, 0, 1280, 0, 0, 0, 0, 1408, 0, 1408, 0, 1065, 1065, 1065, 3072,
    /*  979 */ 0, 1065, 1065, 41, 1065, 1065, 0, 0, 0, 1065, 1065, 1114, 1065, 1065, 1065, 0, 0, 2729, 1065, 1065, 2944,
    /* 1000 */ 0, 0, 0, 0, 0, 0, 0, 3712, 1067, 1067, 1067, 1067, 1067, 1067, 0, 0, 0, 1099, 1065, 1065, 1065, 1065,
    /* 1022 */ 1106, 1065, 52, 0, 86, 1068, 1068, 1068, 1068, 1068, 1068, 0, 0, 0, 1920, 0, 1920, 0, 0, 0, 929, 0, 0, 0,
    /* 1046 */ 1067, 929, 0, 0, 0, 37, 38, 0, 59, 1065, 1085, 1065, 1065, 1065, 1065, 1065, 1065, 1065, 2473, 1092, 0,
    /* 1067 */ 1085, 52, 309, 0, 0, 0, 640, 640, 640, 0, 0, 929, 0, 0, 55, 37, 38, 0, 0, 0, 2176, 0, 0, 0, 0, 568, 38,
    /* 1094 */ 0, 0, 1065, 1065, 1065, 1107, 1065, 52, 0, 0, 74, 1065, 1065, 1065, 1065, 1065, 52, 85, 0, 1118, 1065,
    /* 1115 */ 1065, 1065, 0, 0, 0, 1065, 1100, 1065, 1102, 1065, 103, 0, 1065, 1065, 1065, 1131, 0, 109, 1065, 1135,
    /* 1135 */ 1065, 0, 0, 1065, 1065, 1065, 1115, 1065, 1065, 1065, 1065, 1119, 1065, 0, 97, 0, 1065, 0, 1065, 52, 309,
    /* 1156 */ 0, 72, 0, 37, 37, 0, 0, 0, 0, 0, 52, 0, 0, 0, 0, 0, 3456, 1065, 1065, 1065, 1065, 1065, 1065, 1089, 1065,
    /* 1181 */ 1065, 1065, 1065, 1136, 0, 0, 1065, 1065, 1065, 0, 113, 1065, 1136, 1065, 1069, 1069, 1069, 1069, 1069,
    /* 1200 */ 1069, 0, 0, 1065, 1113, 1065, 1065, 1065, 1065, 1126, 3369, 1065, 1065, 1065, 1065, 1086, 1065, 1065,
    /* 1218 */ 1065, 1065, 1065, 1088, 1065, 1065, 1065, 0, 929, 0, 0, 0, 0, 36, 0, 37, 37, 37, 0, 0, 0, 0, 0, 2304, 0,
    /* 1243 */ 2304, 1070, 1070, 1070, 1070, 1070, 1070, 0, 0, 1065, 1129, 1065, 1065, 0, 0, 0, 929, 0, 0, 0, 1072,
    /* 1264 */ 1104, 1065, 1065, 1065, 1065, 52, 0, 0, 1112, 1065, 1065, 1065, 1065, 1065, 52, 0, 0, 1065, 1065, 2857,
    /* 1284 */ 1065, 1065, 1065, 1065, 1065, 2432, 0, 0, 41, 1065, 1065, 1065, 1125, 1065, 1065, 1065, 1065, 1087, 1065,
    /* 1303 */ 1065, 1090, 1065, 1123, 1065, 1065, 1065, 1065, 1065, 1065, 1065, 1065, 1091, 1071, 1071, 1071, 1071,
    /* 1320 */ 1071, 1071, 0, 0, 1152, 0, 0, 0, 0, 0, 70, 0, 0, 929, 0, 0, 0, 37, 38, 58, 0, 38, 38, 0, 0, 0, 0, 0, 0,
    /* 1349 */ 0, 0, 52, 1065, 0, 1065, 52, 309, 71, 0, 0, 1664, 34, 0, 0, 0, 0, 37, 569, 0, 0, 1065, 1065, 1105, 1065,
    /* 1374 */ 1108, 52, 0, 0, 2304, 2304, 0, 2304, 0, 0, 0, 929, 0, 0, 0, 1070, 1072, 1072, 1072, 1072, 1072, 1072, 0,
    /* 1397 */ 0, 2601, 1065, 1065, 1065, 0, 0, 1065, 1065, 1065, 1065, 0, 0, 0, 1065, 1073, 1073, 1073, 1073, 1073,
    /* 1417 */ 1073, 0, 0, 3840, 3840, 3840, 3840, 0, 0, 0, 0, 0, 0, 0, 3840, 0, 0, 104, 1065, 1065, 1130, 1065, 0, 0,
    /* 1441 */ 38, 38, 38, 0, 0, 0, 0, 37, 38, 0, 0, 1065, 3241, 1065, 1065, 1065, 52, 0, 0, 73, 0, 1065, 1065, 1065,
    /* 1465 */ 1065, 1103, 3712, 0, 0, 0, 3712, 0, 0, 0, 929, 0, 0, 0, 1065, 1065, 1065, 1065, 1065, 1065, 768, 0, 0, 0,
    /* 1489 */ 0, 0, 0, 0, 1536, 0, 0
  )

  private final val EXPECTED = Array(
    /*   0 */ 15, 23, 31, 39, 47, 76, 61, 52, 81, 53, 69, 89, 97, 100, 53, 38, 70, 4102, 8198, 32774, 262150, 8388614,
    /*  22 */ 268435462, 536870918, 78, 270, 65550, 1094, 131142, 3078, 133126, 268500998, 65806, 131342, 147526,
    /*  35 */ 268501510, 149510, 608174086, 188219526, 188285062, 188285318, 725156230, 725156246, 725156758, 725418390,
    /*  45 */ 4, 2, 64, 8192, 262144, 8388608, 8, 8, 4194304, 67108864, 128, 524416, 1048704, 2097280, 128, 128,
    /*  61 */ 134217856, 16, 786560, 2, 2, 262144, 8388608, 8, 33554560, 128, 134217856, 262272, 262144, 8388608,
    /*  75 */ 4194304, 128, 524416, 1048704, 2097280, 128, 128, 16777344, 33554560, 134217856, 262272, 2, 262144,
    /*  88 */ 8388608, 524416, 1048704, 2097280, 128, 128, 33554560, 134217856, 262272, 8388608, 4194304, 524416,
    /* 100 */ 1048704, 128, 128, 8388608, 4194304, 1048704, 128, 128
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
