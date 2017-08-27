package com.xmlcalabash.model.tpl


// This file was generated on Sat Aug 26, 2017 15:00 (UTC-05) by REx v5.45 which is Copyright (c) 1979-2017 by Gunther Rademacher <grd@gmx.net>
// REx command line: tpl.ebnf -tree -scala -backtrack

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
    /*   0 */ 43, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7,
    /*  35 */ 6, 8, 6, 6, 9, 10, 11, 12, 6, 13, 14, 15, 16, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 17, 18, 6, 19, 20,
    /*  63 */ 6, 6, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
    /*  90 */ 15, 21, 6, 22, 6, 15, 6, 23, 15, 24, 15, 25, 26, 27, 28, 29, 15, 15, 30, 15, 31, 32, 33, 15, 34, 35, 36,
    /* 117 */ 37, 15, 38, 15, 39, 15, 40, 6, 41, 6, 6
  )

  private final val MAP1 = Array(
    /*   0 */ 108, 124, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 156, 182, 182, 182, 182,
    /*  21 */ 182, 215, 216, 214, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215,
    /*  42 */ 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215,
    /*  63 */ 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215,
    /*  84 */ 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215, 215,
    /* 105 */ 215, 215, 215, 248, 262, 278, 294, 358, 329, 375, 391, 413, 413, 413, 405, 359, 351, 359, 351, 359, 359,
    /* 126 */ 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359,
    /* 147 */ 344, 359, 359, 359, 359, 359, 359, 359, 359, 313, 413, 413, 414, 412, 413, 413, 359, 359, 359, 359, 359,
    /* 168 */ 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 309, 413, 413, 413, 413, 413, 413, 413,
    /* 189 */ 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413, 413,
    /* 210 */ 413, 413, 413, 413, 358, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359,
    /* 231 */ 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 359, 413, 43, 1, 1, 1, 1, 1, 1,
    /* 255 */ 1, 1, 2, 3, 1, 4, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 6, 7, 6, 8, 6, 6, 9, 10, 11, 12,
    /* 289 */ 6, 13, 14, 15, 16, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 17, 18, 6, 19, 20, 6, 6, 42, 6, 6, 6, 6, 6, 6,
    /* 318 */ 6, 6, 6, 6, 6, 6, 6, 15, 15, 6, 6, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 21, 6, 22, 6, 15, 15, 15,
    /* 347 */ 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 6, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
    /* 374 */ 15, 6, 23, 15, 24, 15, 25, 26, 27, 28, 29, 15, 15, 30, 15, 31, 32, 33, 15, 34, 35, 36, 37, 15, 38, 15, 39,
    /* 401 */ 15, 40, 6, 41, 6, 6, 6, 6, 6, 6, 6, 15, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 15
  )

  private final val MAP2 = Array(
    /*  0 */ 57344, 63744, 64976, 65008, 65536, 983040, 63743, 64975, 65007, 65533, 983039, 1114111, 6, 15, 6, 15, 15,
    /* 17 */ 6
  )

  private final val INITIAL = Array(
    /*  0 */ 1, 898, 3, 4, 5, 6, 7, 8, 9, 906, 11, 12, 909, 910, 15, 16, 17, 18, 19, 916, 21, 22, 23, 1048, 1049, 1050,
    /* 26 */ 1051, 1052, 1053, 1054
  )

  private final val TRANSITION = Array(
    /*    0 */ 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406,
    /*   17 */ 1406, 1406, 1406, 1406, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 704, 704, 704,
    /*   35 */ 706, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 704, 704, 704, 705, 1426,
    /*   53 */ 1406, 1238, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 704, 704, 704, 706, 1406, 1406, 811,
    /*   71 */ 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 714, 714, 714, 716, 1426, 1406, 1238, 1406, 1236,
    /*   89 */ 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1426, 1406, 811, 1406, 1236, 1406,
    /*  106 */ 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1363, 1233, 1406, 1022, 1406, 1318, 1406, 1236, 1406, 1406,
    /*  123 */ 1406, 1406, 1406, 1406, 1406, 1406, 1252, 724, 735, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406,
    /*  141 */ 1406, 1406, 1406, 1406, 1422, 1403, 1406, 1273, 1406, 811, 1405, 1236, 1406, 1406, 1406, 1406, 1406,
    /*  158 */ 1406, 1406, 1406, 1406, 816, 816, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406,
    /*  176 */ 1406, 821, 1406, 1406, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406,
    /*  194 */ 1406, 1407, 1426, 1406, 801, 1406, 1255, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 751, 1472, 1406,
    /*  212 */ 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 749, 759, 1070, 1159, 764, 1327,
    /*  230 */ 1313, 1074, 1206, 990, 971, 1327, 969, 966, 1406, 1406, 749, 759, 1070, 857, 764, 1327, 1313, 1074, 1206,
    /*  249 */ 1075, 971, 1327, 969, 966, 1406, 1406, 772, 772, 772, 774, 1426, 1406, 811, 1406, 727, 1406, 1406, 1406,
    /*  268 */ 1406, 1406, 1406, 1406, 1269, 1406, 1406, 1406, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406,
    /*  285 */ 1406, 1406, 1406, 1406, 1406, 905, 1406, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406,
    /*  303 */ 1406, 806, 1406, 1406, 1406, 782, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406,
    /*  321 */ 1406, 1406, 1406, 1426, 1406, 796, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1018, 833,
    /*  339 */ 829, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1367, 1342, 1406, 1426,
    /*  357 */ 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 749, 759, 1070, 857, 841, 855, 867,
    /*  375 */ 1074, 1206, 1075, 971, 1327, 1107, 966, 1406, 1406, 880, 759, 1461, 899, 764, 1327, 1313, 1074, 1039,
    /*  393 */ 1203, 919, 1327, 969, 931, 1406, 1406, 749, 759, 1070, 857, 764, 1327, 1313, 1036, 1300, 942, 971, 950,
    /*  412 */ 963, 979, 1003, 1406, 749, 759, 741, 1012, 764, 1327, 1313, 1074, 1206, 1075, 971, 1327, 969, 966, 1406,
    /*  431 */ 1406, 749, 759, 1165, 1030, 764, 1327, 1313, 1074, 1206, 1075, 971, 1327, 969, 966, 1406, 1406, 749, 759,
    /*  450 */ 1070, 857, 1047, 1055, 1065, 1448, 1206, 1075, 1285, 1057, 969, 995, 1406, 1406, 749, 759, 1070, 857,
    /*  468 */ 1083, 1176, 1313, 1074, 1101, 1075, 1121, 1327, 1133, 1141, 1406, 1406, 749, 759, 1070, 857, 764, 1327,
    /*  486 */ 1313, 1074, 1206, 1075, 1152, 1093, 969, 966, 1406, 1406, 749, 759, 1070, 857, 764, 1328, 1313, 1074,
    /*  504 */ 1206, 1173, 985, 1327, 934, 1184, 1406, 1406, 749, 759, 847, 1197, 764, 1214, 1313, 1125, 1206, 1089,
    /*  522 */ 971, 1327, 969, 966, 1406, 1406, 1226, 759, 911, 1246, 764, 1327, 891, 1074, 1263, 1075, 971, 1281, 969,
    /*  541 */ 966, 1406, 1406, 749, 759, 1070, 857, 764, 1293, 1313, 923, 1206, 1075, 859, 1308, 969, 966, 1406, 1406,
    /*  560 */ 749, 759, 1070, 857, 764, 1327, 1313, 1074, 1206, 1075, 971, 1326, 969, 1189, 1406, 1406, 749, 759, 788,
    /*  579 */ 1336, 1356, 1218, 957, 886, 1375, 1075, 971, 1327, 969, 966, 1406, 1406, 749, 759, 1348, 1396, 764, 1327,
    /*  598 */ 1313, 1074, 1206, 1144, 971, 1327, 969, 966, 1406, 1406, 749, 759, 1113, 1415, 764, 1327, 1313, 1074,
    /*  616 */ 1206, 1075, 971, 1327, 1441, 966, 1406, 1406, 749, 759, 1070, 857, 764, 1327, 1313, 1074, 1434, 1075,
    /*  634 */ 971, 1327, 969, 966, 1406, 1406, 1004, 1406, 1456, 1406, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406,
    /*  652 */ 1406, 1406, 1406, 1406, 1406, 1386, 1388, 1381, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406,
    /*  669 */ 1406, 1406, 1406, 1406, 1406, 1406, 872, 1426, 1406, 811, 1406, 1236, 1406, 1406, 1406, 1406, 1406, 1406,
    /*  687 */ 1406, 1469, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406, 1406,
    /*  704 */ 384, 384, 384, 384, 384, 384, 384, 384, 0, 0, 415, 415, 415, 415, 415, 415, 415, 415, 0, 0, 0, 1152,
    /*  726 */ 1152, 0, 0, 0, 0, 0, 256, 0, 0, 0, 1152, 1152, 1152, 1152, 1152, 0, 0, 0, 929, 0, 0, 0, 1067, 0, 929, 0,
    /*  752 */ 0, 0, 0, 0, 0, 1536, 1536, 0, 929, 0, 0, 929, 929, 0, 0, 0, 37, 38, 0, 0, 32, 32, 32, 32, 32, 32, 32, 32,
    /*  780 */ 0, 309, 0, 1792, 0, 0, 37, 38, 0, 0, 0, 929, 0, 0, 0, 1071, 0, 640, 0, 52, 309, 0, 0, 0, 70, 309, 0, 0,
    /*  808 */ 0, 0, 2048, 0, 0, 0, 52, 309, 0, 0, 0, 0, 1280, 0, 0, 0, 0, 1408, 0, 1408, 0, 2176, 2176, 2176, 2176,
    /*  833 */ 2176, 2176, 0, 0, 2176, 0, 0, 0, 929, 0, 54, 0, 37, 38, 0, 0, 0, 929, 0, 0, 39, 1069, 1065, 1084, 1065,
    /*  858 */ 1065, 1065, 1065, 1065, 1065, 0, 0, 98, 1065, 1065, 1065, 1093, 52, 309, 0, 0, 0, 640, 640, 640, 0, 0, 0,
    /*  881 */ 929, 0, 0, 0, 35, 0, 0, 0, 1099, 1065, 1065, 1065, 1065, 52, 309, 0, 72, 0, 1066, 1066, 1066, 1066, 1066,
    /*  904 */ 1075, 0, 0, 0, 1920, 0, 1920, 0, 0, 0, 929, 0, 0, 0, 1070, 1065, 1065, 1065, 1120, 0, 0, 0, 1065, 1065,
    /*  928 */ 1101, 1065, 1065, 1134, 1065, 1065, 0, 0, 1065, 1065, 1065, 1065, 108, 0, 87, 0, 1065, 1065, 1065, 1065,
    /*  948 */ 1116, 1117, 1065, 1124, 1065, 1065, 1065, 1065, 3625, 1065, 1065, 1065, 52, 309, 71, 0, 0, 2601, 1065,
    /*  967 */ 1065, 1065, 0, 0, 1065, 1065, 1065, 1065, 0, 0, 0, 1065, 1065, 1065, 1065, 3072, 0, 1065, 1065, 41, 1065,
    /*  988 */ 1065, 0, 0, 0, 1065, 1065, 1114, 1065, 1065, 1065, 0, 0, 2729, 1065, 1065, 2944, 0, 0, 0, 0, 0, 0, 0,
    /* 1011 */ 3712, 1067, 1067, 1067, 1067, 1067, 1067, 0, 0, 0, 2176, 0, 0, 0, 0, 568, 38, 0, 0, 1068, 1068, 1068,
    /* 1033 */ 1068, 1068, 1068, 0, 0, 74, 1065, 1065, 1065, 1065, 1065, 52, 85, 0, 929, 0, 0, 0, 37, 38, 0, 59, 1065,
    /* 1056 */ 1085, 1065, 1065, 1065, 1065, 1065, 1065, 1065, 2473, 1092, 1065, 1085, 52, 309, 0, 0, 0, 929, 0, 0, 0,
    /* 1077 */ 1065, 1065, 1065, 1065, 1065, 1065, 929, 0, 0, 55, 37, 38, 0, 0, 1065, 1113, 1065, 1065, 1065, 1065,
    /* 1097 */ 1126, 3369, 1065, 1065, 1065, 1065, 1065, 1107, 1065, 52, 0, 0, 1065, 1129, 1065, 1065, 0, 0, 0, 929, 0,
    /* 1118 */ 0, 40, 1073, 1118, 1065, 1065, 1065, 0, 0, 0, 1065, 1100, 1065, 1102, 1065, 103, 0, 1065, 1065, 1065,
    /* 1138 */ 1131, 0, 109, 1065, 1135, 1065, 0, 0, 1065, 1065, 1065, 1115, 1065, 1065, 1065, 1065, 1119, 1065, 0, 97,
    /* 1158 */ 0, 1065, 1065, 1065, 1074, 1074, 1074, 0, 0, 0, 929, 0, 0, 0, 1068, 0, 3456, 1065, 1065, 1065, 1065,
    /* 1179 */ 1065, 1065, 1089, 1065, 1065, 1065, 1065, 1136, 0, 0, 1065, 1065, 1065, 0, 113, 1065, 1136, 1065, 1069,
    /* 1198 */ 1069, 1069, 1069, 1069, 1069, 0, 0, 1112, 1065, 1065, 1065, 1065, 1065, 52, 0, 0, 1065, 1065, 1086, 1065,
    /* 1218 */ 1065, 1065, 1065, 1065, 1088, 1065, 1065, 1065, 0, 929, 0, 0, 0, 0, 36, 0, 37, 37, 0, 0, 0, 0, 0, 52, 0,
    /* 1243 */ 0, 0, 0, 1070, 1070, 1070, 1070, 1070, 1070, 0, 0, 1152, 0, 0, 0, 0, 0, 70, 0, 0, 1104, 1065, 1065, 1065,
    /* 1267 */ 1065, 52, 0, 0, 1664, 34, 0, 0, 0, 0, 37, 569, 0, 0, 1065, 1065, 2857, 1065, 1065, 1065, 1065, 1065,
    /* 1289 */ 2432, 0, 0, 41, 1065, 1065, 1065, 1087, 1065, 1065, 1090, 1065, 1065, 1065, 1106, 1065, 52, 0, 86, 1065,
    /* 1309 */ 1065, 1065, 1125, 1065, 1065, 1065, 1065, 52, 309, 0, 0, 0, 52, 309, 0, 0, 37, 1123, 1065, 1065, 1065,
    /* 1330 */ 1065, 1065, 1065, 1065, 1065, 1091, 1071, 1071, 1071, 1071, 1071, 1071, 0, 0, 2304, 2304, 0, 2304, 0, 0,
    /* 1350 */ 0, 929, 0, 0, 0, 1072, 929, 0, 0, 0, 37, 38, 58, 0, 37, 37, 37, 0, 0, 0, 0, 0, 2304, 0, 2304, 1065, 1065,
    /* 1377 */ 1105, 1065, 1108, 52, 0, 0, 3840, 3840, 3840, 3840, 0, 0, 0, 0, 0, 0, 0, 3840, 0, 1072, 1072, 1072, 1072,
    /* 1400 */ 1072, 1072, 0, 0, 38, 38, 0, 0, 0, 0, 0, 0, 0, 0, 52, 1073, 1073, 1073, 1073, 1073, 1073, 0, 0, 38, 38,
    /* 1425 */ 38, 0, 0, 0, 0, 37, 38, 0, 0, 1065, 3241, 1065, 1065, 1065, 52, 0, 0, 104, 1065, 1065, 1130, 1065, 0, 0,
    /* 1449 */ 73, 0, 1065, 1065, 1065, 1065, 1103, 3712, 0, 0, 0, 3712, 0, 0, 0, 929, 0, 0, 0, 1066, 768, 0, 0, 0, 0,
    /* 1474 */ 0, 0, 0, 1536, 0, 0
  )

  private final val EXPECTED = Array(
    /*   0 */ 15, 23, 31, 39, 47, 76, 61, 52, 81, 53, 69, 89, 97, 100, 53, 38, 70, 4102, 8198, 32774, 262150, 8388614,
    /*  22 */ 268435462, 536870918, 78, 270, 65550, 1094, 131142, 3078, 133126, 268500998, 65806, 131342, 147526,
    /*  35 */ 268501510, 149510, 608174086, 188219526, 188285062, 188285318, 725156230, 725156246, 725156758, 725418390,
    /*  45 */ 4, 2, 64, 8192, 262144, 8388608, 8, 8, 4194304, 67108864, 128, 524416, 1048704, 2097280, 128, 128,
    /*  61 */ 134217856, 144, 786560, 2, 2, 262144, 8388608, 8, 33554560, 128, 134217856, 262272, 262144, 8388608,
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
