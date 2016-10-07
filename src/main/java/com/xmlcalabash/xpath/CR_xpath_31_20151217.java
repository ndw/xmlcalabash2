// This file was generated on Thu Oct 6, 2016 15:35 (UTC-05) by REx v5.41 which is Copyright (c) 1979-2016 by Gunther Rademacher <grd@gmx.net>
// REx command line: CR-xpath-31-20151217.ebnf -smaller -java -saxon -tree

package com.xmlcalabash.xpath;

import java.util.Arrays;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Builder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.lib.Initializer;
import net.sf.saxon.om.NoNamespaceName;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnySimpleType;
import net.sf.saxon.type.AnyType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.expr.parser.Location;

public class CR_xpath_31_20151217
{
  public static class ParseException extends RuntimeException
  {
    private static final long serialVersionUID = 1L;
    private int begin, end, offending, expected, state;

    public ParseException(int b, int e, int s, int o, int x)
    {
      begin = b;
      end = e;
      state = s;
      offending = o;
      expected = x;
    }

    @Override
    public String getMessage()
    {
      return offending < 0 ? "lexical analysis failed" : "syntax error";
    }

    public int getBegin() {return begin;}
    public int getEnd() {return end;}
    public int getState() {return state;}
    public int getOffending() {return offending;}
    public int getExpected() {return expected;}
  }

  public interface EventHandler
  {
    public void reset(CharSequence string);
    public void startNonterminal(String name, int begin);
    public void endNonterminal(String name, int end);
    public void terminal(String name, int begin, int end);
    public void whitespace(int begin, int end);
  }

  public static class TopDownTreeBuilder implements EventHandler
  {
    private CharSequence input = null;
    private Nonterminal[] stack = new Nonterminal[64];
    private int top = -1;

    @Override
    public void reset(CharSequence input)
    {
      this.input = input;
      top = -1;
    }

    @Override
    public void startNonterminal(String name, int begin)
    {
      Nonterminal nonterminal = new Nonterminal(name, begin, begin, new Symbol[0]);
      if (top >= 0) addChild(nonterminal);
      if (++top >= stack.length) stack = Arrays.copyOf(stack, stack.length << 1);
      stack[top] = nonterminal;
    }

    @Override
    public void endNonterminal(String name, int end)
    {
      stack[top].end = end;
      if (top > 0) --top;
    }

    @Override
    public void terminal(String name, int begin, int end)
    {
      addChild(new Terminal(name, begin, end));
    }

    @Override
    public void whitespace(int begin, int end)
    {
    }

    private void addChild(Symbol s)
    {
      Nonterminal current = stack[top];
      current.children = Arrays.copyOf(current.children, current.children.length + 1);
      current.children[current.children.length - 1] = s;
    }

    public void serialize(EventHandler e)
    {
      e.reset(input);
      stack[0].send(e);
    }
  }

  public static abstract class Symbol
  {
    public String name;
    public int begin;
    public int end;

    protected Symbol(String name, int begin, int end)
    {
      this.name = name;
      this.begin = begin;
      this.end = end;
    }

    public abstract void send(EventHandler e);
  }

  public static class Terminal extends Symbol
  {
    public Terminal(String name, int begin, int end)
    {
      super(name, begin, end);
    }

    @Override
    public void send(EventHandler e)
    {
      e.terminal(name, begin, end);
    }
  }

  public static class Nonterminal extends Symbol
  {
    public Symbol[] children;

    public Nonterminal(String name, int begin, int end, Symbol[] children)
    {
      super(name, begin, end);
      this.children = children;
    }

    @Override
    public void send(EventHandler e)
    {
      e.startNonterminal(name, begin);
      int pos = begin;
      for (Symbol c : children)
      {
        if (pos < c.begin) e.whitespace(pos, c.begin);
        c.send(e);
        pos = c.end;
      }
      if (pos < end) e.whitespace(pos, end);
      e.endNonterminal(name, end);
    }
  }

  public static class SaxonTreeBuilder implements EventHandler
  {
    private CharSequence input;
    private Builder builder;
    private AnyType anyType;

    public SaxonTreeBuilder(Builder b)
    {
      input = null;
      builder = b;
      anyType = AnyType.getInstance();
    }

    @Override
    public void reset(CharSequence string)
    {
      input = string;
    }

    @Override
    public void startNonterminal(String name, int begin)
    {
      try
      {
        builder.startElement(new NoNamespaceName(name), anyType, LOCATION, 0);
      }
      catch (XPathException e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void endNonterminal(String name, int end)
    {
      try
      {
        builder.endElement();
      }
      catch (XPathException e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void terminal(String name, int begin, int end)
    {
      if (name.charAt(0) == '\'')
      {
        name = "TOKEN";
      }
      startNonterminal(name, begin);
      characters(begin, end);
      endNonterminal(name, end);
    }

    @Override
    public void whitespace(int begin, int end)
    {
      characters(begin, end);
    }

    private void characters(int begin, int end)
    {
      if (begin < end)
      {
        try
        {
          builder.characters(input.subSequence(begin, end), LOCATION, 0);
        }
        catch (XPathException e)
        {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private static final Location LOCATION = new Location()
  {
    @Override public int getColumnNumber() {return -1;}
    @Override public int getLineNumber() {return -1;}
    @Override public String getPublicId() {return null;}
    @Override public String getSystemId() {return null;}
    @Override public Location saveLocation() {return this;}
  };

  public static class SaxonInitializer implements Initializer
  {
    @Override
    public void initialize(Configuration conf)
    {
      conf.registerExtensionFunction(new SaxonDefinition_XPath());
    }
  }

  public static Sequence parseXPath(XPathContext context, String input) throws XPathException
  {
    Builder builder = context.getController().makeBuilder();
    builder.open();
    CR_xpath_31_20151217 parser = new CR_xpath_31_20151217(input, new SaxonTreeBuilder(builder));
    try
    {
      parser.parse_XPath();
    }
    catch (ParseException pe)
    {
      buildError(parser, pe, builder);
    }
    return builder.getCurrentRoot();
  }

  public static class SaxonDefinition_XPath extends SaxonDefinition
  {
    @Override
    public String functionName() {return "parse-XPath";}
    @Override
    public Sequence execute(XPathContext context, String input) throws XPathException
    {
      return parseXPath(context, input);
    }
  }

  public static abstract class SaxonDefinition extends ExtensionFunctionDefinition
  {
    abstract String functionName();
    abstract Sequence execute(XPathContext context, String input) throws XPathException;

    @Override
    public StructuredQName getFunctionQName() {return new StructuredQName("p", "CR_xpath_31_20151217", functionName());}
    @Override
    public SequenceType[] getArgumentTypes() {return new SequenceType[] {SequenceType.SINGLE_STRING};}
    @Override
    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {return SequenceType.SINGLE_ELEMENT_NODE;}

    @Override
    public ExtensionFunctionCall makeCallExpression()
    {
      return new ExtensionFunctionCall()
      {
        @Override
        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException
        {
          return execute(context, arguments[0].iterate().next().getStringValue());
        }
      };
    }
  }

  private static void buildError(CR_xpath_31_20151217 parser, ParseException pe, Builder builder) throws XPathException
  {
    builder.close();
    builder.reset();
    builder.open();
    builder.startElement(new NoNamespaceName("ERROR"), AnyType.getInstance(), LOCATION, 0);
    AnySimpleType anySimpleType = AnySimpleType.getInstance();
    builder.attribute(new NoNamespaceName("b"), anySimpleType, Integer.toString(pe.getBegin() + 1), LOCATION, 0);
    builder.attribute(new NoNamespaceName("e"), anySimpleType, Integer.toString(pe.getEnd() + 1), LOCATION, 0);
    if (pe.getOffending() < 0)
    {
      builder.attribute(new NoNamespaceName("s"), anySimpleType, Integer.toString(pe.getState()), LOCATION, 0);
    }
    else
    {
      builder.attribute(new NoNamespaceName("o"), anySimpleType, Integer.toString(pe.getOffending()), LOCATION, 0);
      builder.attribute(new NoNamespaceName("x"), anySimpleType, Integer.toString(pe.getExpected()), LOCATION, 0);
    }
    builder.characters(parser.getErrorMessage(pe), LOCATION, 0);
    builder.endElement();
  }

  public CR_xpath_31_20151217(CharSequence string, EventHandler t)
  {
    initialize(string, t);
  }

  public void initialize(CharSequence string, EventHandler eh)
  {
    eventHandler = eh;
    input = string;
    size = input.length();
    reset(0, 0, 0);
  }

  public CharSequence getInput()
  {
    return input;
  }

  public int getTokenOffset()
  {
    return b0;
  }

  public int getTokenEnd()
  {
    return e0;
  }

  public final void reset(int l, int b, int e)
  {
            b0 = b; e0 = b;
    l1 = l; b1 = b; e1 = e;
    l2 = 0;
    l3 = 0;
    end = e;
    eventHandler.reset(input);
  }

  public void reset()
  {
    reset(0, 0, 0);
  }

  public static String getOffendingToken(ParseException e)
  {
    return e.getOffending() < 0 ? null : TOKEN[e.getOffending()];
  }

  public static String[] getExpectedTokenSet(ParseException e)
  {
    String[] expected;
    if (e.getExpected() < 0)
    {
      expected = getTokenSet(- e.getState());
    }
    else
    {
      expected = new String[]{TOKEN[e.getExpected()]};
    }
    return expected;
  }

  public String getErrorMessage(ParseException e)
  {
    String[] tokenSet = getExpectedTokenSet(e);
    String found = getOffendingToken(e);
    String prefix = input.subSequence(0, e.getBegin()).toString();
    int line = prefix.replaceAll("[^\n]", "").length() + 1;
    int column = prefix.length() - prefix.lastIndexOf('\n');
    int size = e.getEnd() - e.getBegin();
    return e.getMessage()
         + (found == null ? "" : ", found " + found)
         + "\nwhile expecting "
         + (tokenSet.length == 1 ? tokenSet[0] : java.util.Arrays.toString(tokenSet))
         + "\n"
         + (size == 0 || found != null ? "" : "after successfully scanning " + size + " characters beginning ")
         + "at line " + line + ", column " + column + ":\n..."
         + input.subSequence(e.getBegin(), Math.min(input.length(), e.getBegin() + 64))
         + "...";
  }

  public void parse_XPath()
  {
    eventHandler.startNonterminal("XPath", e0);
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_Expr();
    consume(11);                    // EOF
    eventHandler.endNonterminal("XPath", e0);
  }

  private void parse_ParamList()
  {
    eventHandler.startNonterminal("ParamList", e0);
    parse_Param();
    for (;;)
    {
      lookahead1W(17);              // S^WS | '(:' | ')' | ','
      if (l1 != 21)                 // ','
      {
        break;
      }
      consume(21);                  // ','
      lookahead1W(2);               // S^WS | '$' | '(:'
      whitespace();
      parse_Param();
    }
    eventHandler.endNonterminal("ParamList", e0);
  }

  private void parse_Param()
  {
    eventHandler.startNonterminal("Param", e0);
    consume(15);                    // '$'
    lookahead1W(42);                // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_EQName();
    lookahead1W(21);                // S^WS | '(:' | ')' | ',' | 'as'
    if (l1 == 47)                   // 'as'
    {
      whitespace();
      parse_TypeDeclaration();
    }
    eventHandler.endNonterminal("Param", e0);
  }

  private void parse_FunctionBody()
  {
    eventHandler.startNonterminal("FunctionBody", e0);
    parse_EnclosedExpr();
    eventHandler.endNonterminal("FunctionBody", e0);
  }

  private void parse_EnclosedExpr()
  {
    eventHandler.startNonterminal("EnclosedExpr", e0);
    consume(104);                   // '{'
    lookahead1W(56);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union' | '}'
    if (l1 != 107)                  // '}'
    {
      whitespace();
      parse_Expr();
    }
    consume(107);                   // '}'
    eventHandler.endNonterminal("EnclosedExpr", e0);
  }

  private void parse_Expr()
  {
    eventHandler.startNonterminal("Expr", e0);
    parse_ExprSingle();
    for (;;)
    {
      if (l1 != 21)                 // ','
      {
        break;
      }
      consume(21);                  // ','
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_ExprSingle();
    }
    eventHandler.endNonterminal("Expr", e0);
  }

  private void parse_ExprSingle()
  {
    eventHandler.startNonterminal("ExprSingle", e0);
    switch (l1)
    {
    case 70:                        // 'if'
      lookahead2W(34);              // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    case 61:                        // 'every'
    case 65:                        // 'for'
    case 77:                        // 'let'
    case 96:                        // 'some'
      lookahead2W(40);              // S^WS | EOF | '!' | '!=' | '#' | '$' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' |
                                    // '/' | '//' | ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' |
                                    // ']' | 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' |
                                    // 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' |
                                    // 'or' | 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 1985:                      // 'for' '$'
      parse_ForExpr();
      break;
    case 1997:                      // 'let' '$'
      parse_LetExpr();
      break;
    case 1981:                      // 'every' '$'
    case 2016:                      // 'some' '$'
      parse_QuantifiedExpr();
      break;
    case 2118:                      // 'if' '('
      parse_IfExpr();
      break;
    default:
      parse_OrExpr();
    }
    eventHandler.endNonterminal("ExprSingle", e0);
  }

  private void parse_ForExpr()
  {
    eventHandler.startNonterminal("ForExpr", e0);
    parse_SimpleForClause();
    consume(91);                    // 'return'
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ExprSingle();
    eventHandler.endNonterminal("ForExpr", e0);
  }

  private void parse_SimpleForClause()
  {
    eventHandler.startNonterminal("SimpleForClause", e0);
    consume(65);                    // 'for'
    lookahead1W(2);                 // S^WS | '$' | '(:'
    whitespace();
    parse_SimpleForBinding();
    for (;;)
    {
      if (l1 != 21)                 // ','
      {
        break;
      }
      consume(21);                  // ','
      lookahead1W(2);               // S^WS | '$' | '(:'
      whitespace();
      parse_SimpleForBinding();
    }
    eventHandler.endNonterminal("SimpleForClause", e0);
  }

  private void parse_SimpleForBinding()
  {
    eventHandler.startNonterminal("SimpleForBinding", e0);
    consume(15);                    // '$'
    lookahead1W(42);                // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_VarName();
    lookahead1W(10);                // S^WS | '(:' | 'in'
    consume(71);                    // 'in'
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ExprSingle();
    eventHandler.endNonterminal("SimpleForBinding", e0);
  }

  private void parse_LetExpr()
  {
    eventHandler.startNonterminal("LetExpr", e0);
    parse_SimpleLetClause();
    consume(91);                    // 'return'
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ExprSingle();
    eventHandler.endNonterminal("LetExpr", e0);
  }

  private void parse_SimpleLetClause()
  {
    eventHandler.startNonterminal("SimpleLetClause", e0);
    consume(77);                    // 'let'
    lookahead1W(2);                 // S^WS | '$' | '(:'
    whitespace();
    parse_SimpleLetBinding();
    for (;;)
    {
      if (l1 != 21)                 // ','
      {
        break;
      }
      consume(21);                  // ','
      lookahead1W(2);               // S^WS | '$' | '(:'
      whitespace();
      parse_SimpleLetBinding();
    }
    eventHandler.endNonterminal("SimpleLetClause", e0);
  }

  private void parse_SimpleLetBinding()
  {
    eventHandler.startNonterminal("SimpleLetBinding", e0);
    consume(15);                    // '$'
    lookahead1W(42);                // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_VarName();
    lookahead1W(8);                 // S^WS | '(:' | ':='
    consume(30);                    // ':='
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ExprSingle();
    eventHandler.endNonterminal("SimpleLetBinding", e0);
  }

  private void parse_QuantifiedExpr()
  {
    eventHandler.startNonterminal("QuantifiedExpr", e0);
    switch (l1)
    {
    case 96:                        // 'some'
      consume(96);                  // 'some'
      break;
    default:
      consume(61);                  // 'every'
    }
    lookahead1W(2);                 // S^WS | '$' | '(:'
    consume(15);                    // '$'
    lookahead1W(42);                // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_VarName();
    lookahead1W(10);                // S^WS | '(:' | 'in'
    consume(71);                    // 'in'
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ExprSingle();
    for (;;)
    {
      if (l1 != 21)                 // ','
      {
        break;
      }
      consume(21);                  // ','
      lookahead1W(2);               // S^WS | '$' | '(:'
      consume(15);                  // '$'
      lookahead1W(42);              // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_VarName();
      lookahead1W(10);              // S^WS | '(:' | 'in'
      consume(71);                  // 'in'
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_ExprSingle();
    }
    consume(92);                    // 'satisfies'
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ExprSingle();
    eventHandler.endNonterminal("QuantifiedExpr", e0);
  }

  private void parse_IfExpr()
  {
    eventHandler.startNonterminal("IfExpr", e0);
    consume(70);                    // 'if'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_Expr();
    consume(18);                    // ')'
    lookahead1W(12);                // S^WS | '(:' | 'then'
    consume(99);                    // 'then'
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ExprSingle();
    consume(58);                    // 'else'
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ExprSingle();
    eventHandler.endNonterminal("IfExpr", e0);
  }

  private void parse_OrExpr()
  {
    eventHandler.startNonterminal("OrExpr", e0);
    parse_AndExpr();
    for (;;)
    {
      if (l1 != 86)                 // 'or'
      {
        break;
      }
      consume(86);                  // 'or'
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_AndExpr();
    }
    eventHandler.endNonterminal("OrExpr", e0);
  }

  private void parse_AndExpr()
  {
    eventHandler.startNonterminal("AndExpr", e0);
    parse_ComparisonExpr();
    for (;;)
    {
      if (l1 != 45)                 // 'and'
      {
        break;
      }
      consume(45);                  // 'and'
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_ComparisonExpr();
    }
    eventHandler.endNonterminal("AndExpr", e0);
  }

  private void parse_ComparisonExpr()
  {
    eventHandler.startNonterminal("ComparisonExpr", e0);
    parse_StringConcatExpr();
    if (l1 != 11                    // EOF
     && l1 != 18                    // ')'
     && l1 != 21                    // ','
     && l1 != 27                    // ':'
     && l1 != 42                    // ']'
     && l1 != 45                    // 'and'
     && l1 != 58                    // 'else'
     && l1 != 86                    // 'or'
     && l1 != 91                    // 'return'
     && l1 != 92                    // 'satisfies'
     && l1 != 107)                  // '}'
    {
      switch (l1)
      {
      case 60:                      // 'eq'
      case 67:                      // 'ge'
      case 68:                      // 'gt'
      case 76:                      // 'le'
      case 78:                      // 'lt'
      case 83:                      // 'ne'
        whitespace();
        parse_ValueComp();
        break;
      case 32:                      // '<<'
      case 38:                      // '>>'
      case 74:                      // 'is'
        whitespace();
        parse_NodeComp();
        break;
      default:
        whitespace();
        parse_GeneralComp();
      }
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_StringConcatExpr();
    }
    eventHandler.endNonterminal("ComparisonExpr", e0);
  }

  private void parse_StringConcatExpr()
  {
    eventHandler.startNonterminal("StringConcatExpr", e0);
    parse_RangeExpr();
    for (;;)
    {
      if (l1 != 106)                // '||'
      {
        break;
      }
      consume(106);                 // '||'
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_RangeExpr();
    }
    eventHandler.endNonterminal("StringConcatExpr", e0);
  }

  private void parse_RangeExpr()
  {
    eventHandler.startNonterminal("RangeExpr", e0);
    parse_AdditiveExpr();
    if (l1 == 100)                  // 'to'
    {
      consume(100);                 // 'to'
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_AdditiveExpr();
    }
    eventHandler.endNonterminal("RangeExpr", e0);
  }

  private void parse_AdditiveExpr()
  {
    eventHandler.startNonterminal("AdditiveExpr", e0);
    parse_MultiplicativeExpr();
    for (;;)
    {
      if (l1 != 20                  // '+'
       && l1 != 22)                 // '-'
      {
        break;
      }
      switch (l1)
      {
      case 20:                      // '+'
        consume(20);                // '+'
        break;
      default:
        consume(22);                // '-'
      }
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_MultiplicativeExpr();
    }
    eventHandler.endNonterminal("AdditiveExpr", e0);
  }

  private void parse_MultiplicativeExpr()
  {
    eventHandler.startNonterminal("MultiplicativeExpr", e0);
    parse_UnionExpr();
    for (;;)
    {
      if (l1 != 19                  // '*'
       && l1 != 55                  // 'div'
       && l1 != 69                  // 'idiv'
       && l1 != 80)                 // 'mod'
      {
        break;
      }
      switch (l1)
      {
      case 19:                      // '*'
        consume(19);                // '*'
        break;
      case 55:                      // 'div'
        consume(55);                // 'div'
        break;
      case 69:                      // 'idiv'
        consume(69);                // 'idiv'
        break;
      default:
        consume(80);                // 'mod'
      }
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_UnionExpr();
    }
    eventHandler.endNonterminal("MultiplicativeExpr", e0);
  }

  private void parse_UnionExpr()
  {
    eventHandler.startNonterminal("UnionExpr", e0);
    parse_IntersectExceptExpr();
    for (;;)
    {
      if (l1 != 103                 // 'union'
       && l1 != 105)                // '|'
      {
        break;
      }
      switch (l1)
      {
      case 103:                     // 'union'
        consume(103);               // 'union'
        break;
      default:
        consume(105);               // '|'
      }
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_IntersectExceptExpr();
    }
    eventHandler.endNonterminal("UnionExpr", e0);
  }

  private void parse_IntersectExceptExpr()
  {
    eventHandler.startNonterminal("IntersectExceptExpr", e0);
    parse_InstanceofExpr();
    for (;;)
    {
      lookahead1W(25);              // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | ']' | 'and' | 'div' | 'else' | 'eq' | 'except' |
                                    // 'ge' | 'gt' | 'idiv' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' |
                                    // 'return' | 'satisfies' | 'to' | 'union' | '|' | '||' | '}'
      if (l1 != 62                  // 'except'
       && l1 != 73)                 // 'intersect'
      {
        break;
      }
      switch (l1)
      {
      case 73:                      // 'intersect'
        consume(73);                // 'intersect'
        break;
      default:
        consume(62);                // 'except'
      }
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_InstanceofExpr();
    }
    eventHandler.endNonterminal("IntersectExceptExpr", e0);
  }

  private void parse_InstanceofExpr()
  {
    eventHandler.startNonterminal("InstanceofExpr", e0);
    parse_TreatExpr();
    lookahead1W(26);                // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | ']' | 'and' | 'div' | 'else' | 'eq' | 'except' |
                                    // 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' |
                                    // 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'union' | '|' | '||' | '}'
    if (l1 == 72)                   // 'instance'
    {
      consume(72);                  // 'instance'
      lookahead1W(11);              // S^WS | '(:' | 'of'
      consume(85);                  // 'of'
      lookahead1W(44);              // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_SequenceType();
    }
    eventHandler.endNonterminal("InstanceofExpr", e0);
  }

  private void parse_TreatExpr()
  {
    eventHandler.startNonterminal("TreatExpr", e0);
    parse_CastableExpr();
    lookahead1W(27);                // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | ']' | 'and' | 'div' | 'else' | 'eq' | 'except' |
                                    // 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' |
                                    // 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' |
                                    // '}'
    if (l1 == 101)                  // 'treat'
    {
      consume(101);                 // 'treat'
      lookahead1W(9);               // S^WS | '(:' | 'as'
      consume(47);                  // 'as'
      lookahead1W(44);              // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_SequenceType();
    }
    eventHandler.endNonterminal("TreatExpr", e0);
  }

  private void parse_CastableExpr()
  {
    eventHandler.startNonterminal("CastableExpr", e0);
    parse_CastExpr();
    lookahead1W(29);                // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | ']' | 'and' | 'castable' | 'div' | 'else' |
                                    // 'eq' | 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' |
                                    // 'lt' | 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'treat' | 'union' |
                                    // '|' | '||' | '}'
    if (l1 == 50)                   // 'castable'
    {
      consume(50);                  // 'castable'
      lookahead1W(9);               // S^WS | '(:' | 'as'
      consume(47);                  // 'as'
      lookahead1W(42);              // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_SingleType();
    }
    eventHandler.endNonterminal("CastableExpr", e0);
  }

  private void parse_CastExpr()
  {
    eventHandler.startNonterminal("CastExpr", e0);
    parse_ArrowExpr();
    if (l1 == 49)                   // 'cast'
    {
      consume(49);                  // 'cast'
      lookahead1W(9);               // S^WS | '(:' | 'as'
      consume(47);                  // 'as'
      lookahead1W(42);              // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_SingleType();
    }
    eventHandler.endNonterminal("CastExpr", e0);
  }

  private void parse_ArrowExpr()
  {
    eventHandler.startNonterminal("ArrowExpr", e0);
    parse_UnaryExpr();
    for (;;)
    {
      lookahead1W(32);              // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '=>' | '>' | '>=' | '>>' | ']' | 'and' | 'cast' | 'castable' |
                                    // 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' | 'instance' |
                                    // 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' | 'satisfies' |
                                    // 'to' | 'treat' | 'union' | '|' | '||' | '}'
      if (l1 != 35)                 // '=>'
      {
        break;
      }
      consume(35);                  // '=>'
      lookahead1W(46);              // URIQualifiedName | QName^Token | S^WS | '$' | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_ArrowFunctionSpecifier();
      lookahead1W(3);               // S^WS | '(' | '(:'
      whitespace();
      parse_ArgumentList();
    }
    eventHandler.endNonterminal("ArrowExpr", e0);
  }

  private void parse_UnaryExpr()
  {
    eventHandler.startNonterminal("UnaryExpr", e0);
    for (;;)
    {
      lookahead1W(53);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      if (l1 != 20                  // '+'
       && l1 != 22)                 // '-'
      {
        break;
      }
      switch (l1)
      {
      case 22:                      // '-'
        consume(22);                // '-'
        break;
      default:
        consume(20);                // '+'
      }
    }
    whitespace();
    parse_ValueExpr();
    eventHandler.endNonterminal("UnaryExpr", e0);
  }

  private void parse_ValueExpr()
  {
    eventHandler.startNonterminal("ValueExpr", e0);
    parse_SimpleMapExpr();
    eventHandler.endNonterminal("ValueExpr", e0);
  }

  private void parse_GeneralComp()
  {
    eventHandler.startNonterminal("GeneralComp", e0);
    switch (l1)
    {
    case 34:                        // '='
      consume(34);                  // '='
      break;
    case 13:                        // '!='
      consume(13);                  // '!='
      break;
    case 31:                        // '<'
      consume(31);                  // '<'
      break;
    case 33:                        // '<='
      consume(33);                  // '<='
      break;
    case 36:                        // '>'
      consume(36);                  // '>'
      break;
    default:
      consume(37);                  // '>='
    }
    eventHandler.endNonterminal("GeneralComp", e0);
  }

  private void parse_ValueComp()
  {
    eventHandler.startNonterminal("ValueComp", e0);
    switch (l1)
    {
    case 60:                        // 'eq'
      consume(60);                  // 'eq'
      break;
    case 83:                        // 'ne'
      consume(83);                  // 'ne'
      break;
    case 78:                        // 'lt'
      consume(78);                  // 'lt'
      break;
    case 76:                        // 'le'
      consume(76);                  // 'le'
      break;
    case 68:                        // 'gt'
      consume(68);                  // 'gt'
      break;
    default:
      consume(67);                  // 'ge'
    }
    eventHandler.endNonterminal("ValueComp", e0);
  }

  private void parse_NodeComp()
  {
    eventHandler.startNonterminal("NodeComp", e0);
    switch (l1)
    {
    case 74:                        // 'is'
      consume(74);                  // 'is'
      break;
    case 32:                        // '<<'
      consume(32);                  // '<<'
      break;
    default:
      consume(38);                  // '>>'
    }
    eventHandler.endNonterminal("NodeComp", e0);
  }

  private void parse_SimpleMapExpr()
  {
    eventHandler.startNonterminal("SimpleMapExpr", e0);
    parse_PathExpr();
    for (;;)
    {
      if (l1 != 12)                 // '!'
      {
        break;
      }
      consume(12);                  // '!'
      lookahead1W(52);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '.' |
                                    // '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' | 'and' |
                                    // 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_PathExpr();
    }
    eventHandler.endNonterminal("SimpleMapExpr", e0);
  }

  private void parse_PathExpr()
  {
    eventHandler.startNonterminal("PathExpr", e0);
    switch (l1)
    {
    case 25:                        // '/'
      consume(25);                  // '/'
      lookahead1W(57);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | EOF | '!' | '!=' | '$' | '(' |
                                    // '(:' | ')' | '*' | '+' | ',' | '-' | '.' | '..' | ':' | '<' | '<<' | '<=' | '=' |
                                    // '=>' | '>' | '>=' | '>>' | '?' | '@' | '[' | ']' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union' | '|' |
                                    // '||' | '}'
      switch (l1)
      {
      case 11:                      // EOF
      case 12:                      // '!'
      case 13:                      // '!='
      case 18:                      // ')'
      case 19:                      // '*'
      case 20:                      // '+'
      case 21:                      // ','
      case 22:                      // '-'
      case 27:                      // ':'
      case 31:                      // '<'
      case 32:                      // '<<'
      case 33:                      // '<='
      case 34:                      // '='
      case 35:                      // '=>'
      case 36:                      // '>'
      case 37:                      // '>='
      case 38:                      // '>>'
      case 42:                      // ']'
      case 105:                     // '|'
      case 106:                     // '||'
      case 107:                     // '}'
        break;
      default:
        whitespace();
        parse_RelativePathExpr();
      }
      break;
    case 26:                        // '//'
      consume(26);                  // '//'
      lookahead1W(51);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '.' |
                                    // '..' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' | 'and' | 'array' |
                                    // 'attribute' | 'cast' | 'castable' | 'child' | 'comment' | 'descendant' |
                                    // 'descendant-or-self' | 'div' | 'document-node' | 'element' | 'else' |
                                    // 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_RelativePathExpr();
      break;
    default:
      parse_RelativePathExpr();
    }
    eventHandler.endNonterminal("PathExpr", e0);
  }

  private void parse_RelativePathExpr()
  {
    eventHandler.startNonterminal("RelativePathExpr", e0);
    parse_StepExpr();
    for (;;)
    {
      if (l1 != 25                  // '/'
       && l1 != 26)                 // '//'
      {
        break;
      }
      switch (l1)
      {
      case 25:                      // '/'
        consume(25);                // '/'
        break;
      default:
        consume(26);                // '//'
      }
      lookahead1W(51);              // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '.' |
                                    // '..' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' | 'and' | 'array' |
                                    // 'attribute' | 'cast' | 'castable' | 'child' | 'comment' | 'descendant' |
                                    // 'descendant-or-self' | 'div' | 'document-node' | 'element' | 'else' |
                                    // 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_StepExpr();
    }
    eventHandler.endNonterminal("RelativePathExpr", e0);
  }

  private void parse_StepExpr()
  {
    eventHandler.startNonterminal("StepExpr", e0);
    switch (l1)
    {
    case 66:                        // 'function'
      lookahead2W(34);              // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    case 46:                        // 'array'
    case 79:                        // 'map'
      lookahead2W(36);              // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' | 'cast' |
                                    // 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '{' | '|' | '||' | '}'
      break;
    case 43:                        // 'ancestor'
    case 44:                        // 'ancestor-or-self'
    case 51:                        // 'child'
    case 53:                        // 'descendant'
    case 54:                        // 'descendant-or-self'
    case 63:                        // 'following'
    case 64:                        // 'following-sibling'
    case 81:                        // 'namespace'
    case 87:                        // 'parent'
    case 88:                        // 'preceding'
    case 89:                        // 'preceding-sibling'
    case 95:                        // 'self'
      lookahead2W(41);              // S^WS | EOF | '!' | '!=' | '#' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' |
                                    // '//' | ':' | '::' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' |
                                    // ']' | 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' |
                                    // 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' |
                                    // 'or' | 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    case 5:                         // URIQualifiedName
    case 7:                         // QName^Token
    case 45:                        // 'and'
    case 49:                        // 'cast'
    case 50:                        // 'castable'
    case 55:                        // 'div'
    case 58:                        // 'else'
    case 60:                        // 'eq'
    case 61:                        // 'every'
    case 62:                        // 'except'
    case 65:                        // 'for'
    case 67:                        // 'ge'
    case 68:                        // 'gt'
    case 69:                        // 'idiv'
    case 72:                        // 'instance'
    case 73:                        // 'intersect'
    case 74:                        // 'is'
    case 76:                        // 'le'
    case 77:                        // 'let'
    case 78:                        // 'lt'
    case 80:                        // 'mod'
    case 83:                        // 'ne'
    case 86:                        // 'or'
    case 91:                        // 'return'
    case 92:                        // 'satisfies'
    case 96:                        // 'some'
    case 100:                       // 'to'
    case 101:                       // 'treat'
    case 103:                       // 'union'
      lookahead2W(37);              // S^WS | EOF | '!' | '!=' | '#' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' |
                                    // '//' | ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' |
                                    // 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' |
                                    // 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' |
                                    // 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 1:                         // IntegerLiteral
    case 2:                         // DecimalLiteral
    case 3:                         // DoubleLiteral
    case 4:                         // StringLiteral
    case 15:                        // '$'
    case 16:                        // '('
    case 23:                        // '.'
    case 39:                        // '?'
    case 41:                        // '['
    case 1797:                      // URIQualifiedName '#'
    case 1799:                      // QName^Token '#'
    case 1835:                      // 'ancestor' '#'
    case 1836:                      // 'ancestor-or-self' '#'
    case 1837:                      // 'and' '#'
    case 1841:                      // 'cast' '#'
    case 1842:                      // 'castable' '#'
    case 1843:                      // 'child' '#'
    case 1845:                      // 'descendant' '#'
    case 1846:                      // 'descendant-or-self' '#'
    case 1847:                      // 'div' '#'
    case 1850:                      // 'else' '#'
    case 1852:                      // 'eq' '#'
    case 1853:                      // 'every' '#'
    case 1854:                      // 'except' '#'
    case 1855:                      // 'following' '#'
    case 1856:                      // 'following-sibling' '#'
    case 1857:                      // 'for' '#'
    case 1859:                      // 'ge' '#'
    case 1860:                      // 'gt' '#'
    case 1861:                      // 'idiv' '#'
    case 1864:                      // 'instance' '#'
    case 1865:                      // 'intersect' '#'
    case 1866:                      // 'is' '#'
    case 1868:                      // 'le' '#'
    case 1869:                      // 'let' '#'
    case 1870:                      // 'lt' '#'
    case 1872:                      // 'mod' '#'
    case 1873:                      // 'namespace' '#'
    case 1875:                      // 'ne' '#'
    case 1878:                      // 'or' '#'
    case 1879:                      // 'parent' '#'
    case 1880:                      // 'preceding' '#'
    case 1881:                      // 'preceding-sibling' '#'
    case 1883:                      // 'return' '#'
    case 1884:                      // 'satisfies' '#'
    case 1887:                      // 'self' '#'
    case 1888:                      // 'some' '#'
    case 1892:                      // 'to' '#'
    case 1893:                      // 'treat' '#'
    case 1895:                      // 'union' '#'
    case 2053:                      // URIQualifiedName '('
    case 2055:                      // QName^Token '('
    case 2091:                      // 'ancestor' '('
    case 2092:                      // 'ancestor-or-self' '('
    case 2093:                      // 'and' '('
    case 2097:                      // 'cast' '('
    case 2098:                      // 'castable' '('
    case 2099:                      // 'child' '('
    case 2101:                      // 'descendant' '('
    case 2102:                      // 'descendant-or-self' '('
    case 2103:                      // 'div' '('
    case 2106:                      // 'else' '('
    case 2108:                      // 'eq' '('
    case 2109:                      // 'every' '('
    case 2110:                      // 'except' '('
    case 2111:                      // 'following' '('
    case 2112:                      // 'following-sibling' '('
    case 2113:                      // 'for' '('
    case 2114:                      // 'function' '('
    case 2115:                      // 'ge' '('
    case 2116:                      // 'gt' '('
    case 2117:                      // 'idiv' '('
    case 2120:                      // 'instance' '('
    case 2121:                      // 'intersect' '('
    case 2122:                      // 'is' '('
    case 2124:                      // 'le' '('
    case 2125:                      // 'let' '('
    case 2126:                      // 'lt' '('
    case 2128:                      // 'mod' '('
    case 2129:                      // 'namespace' '('
    case 2131:                      // 'ne' '('
    case 2134:                      // 'or' '('
    case 2135:                      // 'parent' '('
    case 2136:                      // 'preceding' '('
    case 2137:                      // 'preceding-sibling' '('
    case 2139:                      // 'return' '('
    case 2140:                      // 'satisfies' '('
    case 2143:                      // 'self' '('
    case 2144:                      // 'some' '('
    case 2148:                      // 'to' '('
    case 2149:                      // 'treat' '('
    case 2151:                      // 'union' '('
    case 13358:                     // 'array' '{'
    case 13391:                     // 'map' '{'
      parse_PostfixExpr();
      break;
    default:
      parse_AxisStep();
    }
    eventHandler.endNonterminal("StepExpr", e0);
  }

  private void parse_AxisStep()
  {
    eventHandler.startNonterminal("AxisStep", e0);
    switch (l1)
    {
    case 43:                        // 'ancestor'
    case 44:                        // 'ancestor-or-self'
    case 87:                        // 'parent'
    case 88:                        // 'preceding'
    case 89:                        // 'preceding-sibling'
      lookahead2W(35);              // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '::' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 24:                        // '..'
    case 3755:                      // 'ancestor' '::'
    case 3756:                      // 'ancestor-or-self' '::'
    case 3799:                      // 'parent' '::'
    case 3800:                      // 'preceding' '::'
    case 3801:                      // 'preceding-sibling' '::'
      parse_ReverseStep();
      break;
    default:
      parse_ForwardStep();
    }
    lookahead1W(33);                // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' | 'cast' |
                                    // 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    whitespace();
    parse_PredicateList();
    eventHandler.endNonterminal("AxisStep", e0);
  }

  private void parse_ForwardStep()
  {
    eventHandler.startNonterminal("ForwardStep", e0);
    switch (l1)
    {
    case 48:                        // 'attribute'
      lookahead2W(38);              // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '::' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' |
                                    // 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' |
                                    // 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' |
                                    // 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    case 51:                        // 'child'
    case 53:                        // 'descendant'
    case 54:                        // 'descendant-or-self'
    case 63:                        // 'following'
    case 64:                        // 'following-sibling'
    case 81:                        // 'namespace'
    case 95:                        // 'self'
      lookahead2W(35);              // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '::' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 3760:                      // 'attribute' '::'
    case 3763:                      // 'child' '::'
    case 3765:                      // 'descendant' '::'
    case 3766:                      // 'descendant-or-self' '::'
    case 3775:                      // 'following' '::'
    case 3776:                      // 'following-sibling' '::'
    case 3793:                      // 'namespace' '::'
    case 3807:                      // 'self' '::'
      parse_ForwardAxis();
      lookahead1W(43);              // URIQualifiedName | QName^Token | S^WS | Wildcard | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_NodeTest();
      break;
    default:
      parse_AbbrevForwardStep();
    }
    eventHandler.endNonterminal("ForwardStep", e0);
  }

  private void parse_ForwardAxis()
  {
    eventHandler.startNonterminal("ForwardAxis", e0);
    switch (l1)
    {
    case 51:                        // 'child'
      consume(51);                  // 'child'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 53:                        // 'descendant'
      consume(53);                  // 'descendant'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 48:                        // 'attribute'
      consume(48);                  // 'attribute'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 95:                        // 'self'
      consume(95);                  // 'self'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 54:                        // 'descendant-or-self'
      consume(54);                  // 'descendant-or-self'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 64:                        // 'following-sibling'
      consume(64);                  // 'following-sibling'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 63:                        // 'following'
      consume(63);                  // 'following'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    default:
      consume(81);                  // 'namespace'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
    }
    eventHandler.endNonterminal("ForwardAxis", e0);
  }

  private void parse_AbbrevForwardStep()
  {
    eventHandler.startNonterminal("AbbrevForwardStep", e0);
    if (l1 == 40)                   // '@'
    {
      consume(40);                  // '@'
    }
    lookahead1W(43);                // URIQualifiedName | QName^Token | S^WS | Wildcard | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_NodeTest();
    eventHandler.endNonterminal("AbbrevForwardStep", e0);
  }

  private void parse_ReverseStep()
  {
    eventHandler.startNonterminal("ReverseStep", e0);
    switch (l1)
    {
    case 24:                        // '..'
      parse_AbbrevReverseStep();
      break;
    default:
      parse_ReverseAxis();
      lookahead1W(43);              // URIQualifiedName | QName^Token | S^WS | Wildcard | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_NodeTest();
    }
    eventHandler.endNonterminal("ReverseStep", e0);
  }

  private void parse_ReverseAxis()
  {
    eventHandler.startNonterminal("ReverseAxis", e0);
    switch (l1)
    {
    case 87:                        // 'parent'
      consume(87);                  // 'parent'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 43:                        // 'ancestor'
      consume(43);                  // 'ancestor'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 89:                        // 'preceding-sibling'
      consume(89);                  // 'preceding-sibling'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    case 88:                        // 'preceding'
      consume(88);                  // 'preceding'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
      break;
    default:
      consume(44);                  // 'ancestor-or-self'
      lookahead1W(7);               // S^WS | '(:' | '::'
      consume(29);                  // '::'
    }
    eventHandler.endNonterminal("ReverseAxis", e0);
  }

  private void parse_AbbrevReverseStep()
  {
    eventHandler.startNonterminal("AbbrevReverseStep", e0);
    consume(24);                    // '..'
    eventHandler.endNonterminal("AbbrevReverseStep", e0);
  }

  private void parse_NodeTest()
  {
    eventHandler.startNonterminal("NodeTest", e0);
    switch (l1)
    {
    case 48:                        // 'attribute'
    case 52:                        // 'comment'
    case 56:                        // 'document-node'
    case 57:                        // 'element'
    case 82:                        // 'namespace-node'
    case 84:                        // 'node'
    case 90:                        // 'processing-instruction'
    case 93:                        // 'schema-attribute'
    case 94:                        // 'schema-element'
    case 98:                        // 'text'
      lookahead2W(34);              // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 2096:                      // 'attribute' '('
    case 2100:                      // 'comment' '('
    case 2104:                      // 'document-node' '('
    case 2105:                      // 'element' '('
    case 2130:                      // 'namespace-node' '('
    case 2132:                      // 'node' '('
    case 2138:                      // 'processing-instruction' '('
    case 2141:                      // 'schema-attribute' '('
    case 2142:                      // 'schema-element' '('
    case 2146:                      // 'text' '('
      parse_KindTest();
      break;
    default:
      parse_NameTest();
    }
    eventHandler.endNonterminal("NodeTest", e0);
  }

  private void parse_NameTest()
  {
    eventHandler.startNonterminal("NameTest", e0);
    switch (l1)
    {
    case 10:                        // Wildcard
      consume(10);                  // Wildcard
      break;
    default:
      parse_EQName();
    }
    eventHandler.endNonterminal("NameTest", e0);
  }

  private void parse_PostfixExpr()
  {
    eventHandler.startNonterminal("PostfixExpr", e0);
    parse_PrimaryExpr();
    for (;;)
    {
      lookahead1W(39);              // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '?' | '[' | ']' |
                                    // 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' |
                                    // 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' |
                                    // 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      if (l1 != 16                  // '('
       && l1 != 39                  // '?'
       && l1 != 41)                 // '['
      {
        break;
      }
      switch (l1)
      {
      case 41:                      // '['
        whitespace();
        parse_Predicate();
        break;
      case 16:                      // '('
        whitespace();
        parse_ArgumentList();
        break;
      default:
        whitespace();
        parse_Lookup();
      }
    }
    eventHandler.endNonterminal("PostfixExpr", e0);
  }

  private void parse_ArgumentList()
  {
    eventHandler.startNonterminal("ArgumentList", e0);
    consume(16);                    // '('
    lookahead1W(54);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | ')' | '+' |
                                    // '-' | '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18)                   // ')'
    {
      whitespace();
      parse_Argument();
      for (;;)
      {
        lookahead1W(17);            // S^WS | '(:' | ')' | ','
        if (l1 != 21)               // ','
        {
          break;
        }
        consume(21);                // ','
        lookahead1W(53);            // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace();
        parse_Argument();
      }
    }
    consume(18);                    // ')'
    eventHandler.endNonterminal("ArgumentList", e0);
  }

  private void parse_PredicateList()
  {
    eventHandler.startNonterminal("PredicateList", e0);
    for (;;)
    {
      lookahead1W(33);              // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' | 'cast' |
                                    // 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      if (l1 != 41)                 // '['
      {
        break;
      }
      whitespace();
      parse_Predicate();
    }
    eventHandler.endNonterminal("PredicateList", e0);
  }

  private void parse_Predicate()
  {
    eventHandler.startNonterminal("Predicate", e0);
    consume(41);                    // '['
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_Expr();
    consume(42);                    // ']'
    eventHandler.endNonterminal("Predicate", e0);
  }

  private void parse_Lookup()
  {
    eventHandler.startNonterminal("Lookup", e0);
    consume(39);                    // '?'
    lookahead1W(23);                // IntegerLiteral | NCName | S^WS | '(' | '(:' | '*'
    whitespace();
    parse_KeySpecifier();
    eventHandler.endNonterminal("Lookup", e0);
  }

  private void parse_KeySpecifier()
  {
    eventHandler.startNonterminal("KeySpecifier", e0);
    switch (l1)
    {
    case 6:                         // NCName
      consume(6);                   // NCName
      break;
    case 1:                         // IntegerLiteral
      consume(1);                   // IntegerLiteral
      break;
    case 16:                        // '('
      parse_ParenthesizedExpr();
      break;
    default:
      consume(19);                  // '*'
    }
    eventHandler.endNonterminal("KeySpecifier", e0);
  }

  private void parse_ArrowFunctionSpecifier()
  {
    eventHandler.startNonterminal("ArrowFunctionSpecifier", e0);
    switch (l1)
    {
    case 15:                        // '$'
      parse_VarRef();
      break;
    case 16:                        // '('
      parse_ParenthesizedExpr();
      break;
    default:
      parse_EQName();
    }
    eventHandler.endNonterminal("ArrowFunctionSpecifier", e0);
  }

  private void parse_PrimaryExpr()
  {
    eventHandler.startNonterminal("PrimaryExpr", e0);
    switch (l1)
    {
    case 5:                         // URIQualifiedName
    case 7:                         // QName^Token
    case 43:                        // 'ancestor'
    case 44:                        // 'ancestor-or-self'
    case 45:                        // 'and'
    case 49:                        // 'cast'
    case 50:                        // 'castable'
    case 51:                        // 'child'
    case 53:                        // 'descendant'
    case 54:                        // 'descendant-or-self'
    case 55:                        // 'div'
    case 58:                        // 'else'
    case 60:                        // 'eq'
    case 61:                        // 'every'
    case 62:                        // 'except'
    case 63:                        // 'following'
    case 64:                        // 'following-sibling'
    case 65:                        // 'for'
    case 67:                        // 'ge'
    case 68:                        // 'gt'
    case 69:                        // 'idiv'
    case 72:                        // 'instance'
    case 73:                        // 'intersect'
    case 74:                        // 'is'
    case 76:                        // 'le'
    case 77:                        // 'let'
    case 78:                        // 'lt'
    case 80:                        // 'mod'
    case 81:                        // 'namespace'
    case 83:                        // 'ne'
    case 86:                        // 'or'
    case 87:                        // 'parent'
    case 88:                        // 'preceding'
    case 89:                        // 'preceding-sibling'
    case 91:                        // 'return'
    case 92:                        // 'satisfies'
    case 95:                        // 'self'
    case 96:                        // 'some'
    case 100:                       // 'to'
    case 101:                       // 'treat'
    case 103:                       // 'union'
      lookahead2W(15);              // S^WS | '#' | '(' | '(:'
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 1:                         // IntegerLiteral
    case 2:                         // DecimalLiteral
    case 3:                         // DoubleLiteral
    case 4:                         // StringLiteral
      parse_Literal();
      break;
    case 15:                        // '$'
      parse_VarRef();
      break;
    case 16:                        // '('
      parse_ParenthesizedExpr();
      break;
    case 23:                        // '.'
      parse_ContextItemExpr();
      break;
    case 2053:                      // URIQualifiedName '('
    case 2055:                      // QName^Token '('
    case 2091:                      // 'ancestor' '('
    case 2092:                      // 'ancestor-or-self' '('
    case 2093:                      // 'and' '('
    case 2097:                      // 'cast' '('
    case 2098:                      // 'castable' '('
    case 2099:                      // 'child' '('
    case 2101:                      // 'descendant' '('
    case 2102:                      // 'descendant-or-self' '('
    case 2103:                      // 'div' '('
    case 2106:                      // 'else' '('
    case 2108:                      // 'eq' '('
    case 2109:                      // 'every' '('
    case 2110:                      // 'except' '('
    case 2111:                      // 'following' '('
    case 2112:                      // 'following-sibling' '('
    case 2113:                      // 'for' '('
    case 2115:                      // 'ge' '('
    case 2116:                      // 'gt' '('
    case 2117:                      // 'idiv' '('
    case 2120:                      // 'instance' '('
    case 2121:                      // 'intersect' '('
    case 2122:                      // 'is' '('
    case 2124:                      // 'le' '('
    case 2125:                      // 'let' '('
    case 2126:                      // 'lt' '('
    case 2128:                      // 'mod' '('
    case 2129:                      // 'namespace' '('
    case 2131:                      // 'ne' '('
    case 2134:                      // 'or' '('
    case 2135:                      // 'parent' '('
    case 2136:                      // 'preceding' '('
    case 2137:                      // 'preceding-sibling' '('
    case 2139:                      // 'return' '('
    case 2140:                      // 'satisfies' '('
    case 2143:                      // 'self' '('
    case 2144:                      // 'some' '('
    case 2148:                      // 'to' '('
    case 2149:                      // 'treat' '('
    case 2151:                      // 'union' '('
      parse_FunctionCall();
      break;
    case 79:                        // 'map'
      parse_MapConstructor();
      break;
    case 41:                        // '['
    case 46:                        // 'array'
      parse_ArrayConstructor();
      break;
    case 39:                        // '?'
      parse_UnaryLookup();
      break;
    default:
      parse_FunctionItemExpr();
    }
    eventHandler.endNonterminal("PrimaryExpr", e0);
  }

  private void parse_Literal()
  {
    eventHandler.startNonterminal("Literal", e0);
    switch (l1)
    {
    case 4:                         // StringLiteral
      consume(4);                   // StringLiteral
      break;
    default:
      parse_NumericLiteral();
    }
    eventHandler.endNonterminal("Literal", e0);
  }

  private void parse_NumericLiteral()
  {
    eventHandler.startNonterminal("NumericLiteral", e0);
    switch (l1)
    {
    case 1:                         // IntegerLiteral
      consume(1);                   // IntegerLiteral
      break;
    case 2:                         // DecimalLiteral
      consume(2);                   // DecimalLiteral
      break;
    default:
      consume(3);                   // DoubleLiteral
    }
    eventHandler.endNonterminal("NumericLiteral", e0);
  }

  private void parse_VarRef()
  {
    eventHandler.startNonterminal("VarRef", e0);
    consume(15);                    // '$'
    lookahead1W(42);                // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_VarName();
    eventHandler.endNonterminal("VarRef", e0);
  }

  private void parse_VarName()
  {
    eventHandler.startNonterminal("VarName", e0);
    parse_EQName();
    eventHandler.endNonterminal("VarName", e0);
  }

  private void parse_ParenthesizedExpr()
  {
    eventHandler.startNonterminal("ParenthesizedExpr", e0);
    consume(16);                    // '('
    lookahead1W(54);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | ')' | '+' |
                                    // '-' | '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18)                   // ')'
    {
      whitespace();
      parse_Expr();
    }
    consume(18);                    // ')'
    eventHandler.endNonterminal("ParenthesizedExpr", e0);
  }

  private void parse_ContextItemExpr()
  {
    eventHandler.startNonterminal("ContextItemExpr", e0);
    consume(23);                    // '.'
    eventHandler.endNonterminal("ContextItemExpr", e0);
  }

  private void parse_FunctionCall()
  {
    eventHandler.startNonterminal("FunctionCall", e0);
    parse_FunctionEQName();
    lookahead1W(3);                 // S^WS | '(' | '(:'
    whitespace();
    parse_ArgumentList();
    eventHandler.endNonterminal("FunctionCall", e0);
  }

  private void parse_Argument()
  {
    eventHandler.startNonterminal("Argument", e0);
    switch (l1)
    {
    case 39:                        // '?'
      lookahead2W(24);              // IntegerLiteral | NCName | S^WS | '(' | '(:' | ')' | '*' | ','
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 2343:                      // '?' ')'
    case 2727:                      // '?' ','
      parse_ArgumentPlaceholder();
      break;
    default:
      parse_ExprSingle();
    }
    eventHandler.endNonterminal("Argument", e0);
  }

  private void parse_ArgumentPlaceholder()
  {
    eventHandler.startNonterminal("ArgumentPlaceholder", e0);
    consume(39);                    // '?'
    eventHandler.endNonterminal("ArgumentPlaceholder", e0);
  }

  private void parse_FunctionItemExpr()
  {
    eventHandler.startNonterminal("FunctionItemExpr", e0);
    switch (l1)
    {
    case 66:                        // 'function'
      parse_InlineFunctionExpr();
      break;
    default:
      parse_NamedFunctionRef();
    }
    eventHandler.endNonterminal("FunctionItemExpr", e0);
  }

  private void parse_NamedFunctionRef()
  {
    eventHandler.startNonterminal("NamedFunctionRef", e0);
    parse_FunctionEQName();
    lookahead1W(1);                 // S^WS | '#' | '(:'
    consume(14);                    // '#'
    lookahead1W(0);                 // IntegerLiteral | S^WS | '(:'
    consume(1);                     // IntegerLiteral
    eventHandler.endNonterminal("NamedFunctionRef", e0);
  }

  private void parse_InlineFunctionExpr()
  {
    eventHandler.startNonterminal("InlineFunctionExpr", e0);
    consume(66);                    // 'function'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(16);                // S^WS | '$' | '(:' | ')'
    if (l1 == 15)                   // '$'
    {
      whitespace();
      parse_ParamList();
    }
    consume(18);                    // ')'
    lookahead1W(19);                // S^WS | '(:' | 'as' | '{'
    if (l1 == 47)                   // 'as'
    {
      consume(47);                  // 'as'
      lookahead1W(44);              // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace();
      parse_SequenceType();
    }
    lookahead1W(13);                // S^WS | '(:' | '{'
    whitespace();
    parse_FunctionBody();
    eventHandler.endNonterminal("InlineFunctionExpr", e0);
  }

  private void parse_MapConstructor()
  {
    eventHandler.startNonterminal("MapConstructor", e0);
    consume(79);                    // 'map'
    lookahead1W(13);                // S^WS | '(:' | '{'
    consume(104);                   // '{'
    lookahead1W(56);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union' | '}'
    if (l1 != 107)                  // '}'
    {
      whitespace();
      parse_MapConstructorEntry();
      for (;;)
      {
        if (l1 != 21)               // ','
        {
          break;
        }
        consume(21);                // ','
        lookahead1W(53);            // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace();
        parse_MapConstructorEntry();
      }
    }
    consume(107);                   // '}'
    eventHandler.endNonterminal("MapConstructor", e0);
  }

  private void parse_MapConstructorEntry()
  {
    eventHandler.startNonterminal("MapConstructorEntry", e0);
    parse_MapKeyExpr();
    consume(27);                    // ':'
    lookahead1W(53);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_MapValueExpr();
    eventHandler.endNonterminal("MapConstructorEntry", e0);
  }

  private void parse_MapKeyExpr()
  {
    eventHandler.startNonterminal("MapKeyExpr", e0);
    parse_ExprSingle();
    eventHandler.endNonterminal("MapKeyExpr", e0);
  }

  private void parse_MapValueExpr()
  {
    eventHandler.startNonterminal("MapValueExpr", e0);
    parse_ExprSingle();
    eventHandler.endNonterminal("MapValueExpr", e0);
  }

  private void parse_ArrayConstructor()
  {
    eventHandler.startNonterminal("ArrayConstructor", e0);
    switch (l1)
    {
    case 41:                        // '['
      parse_SquareArrayConstructor();
      break;
    default:
      parse_CurlyArrayConstructor();
    }
    eventHandler.endNonterminal("ArrayConstructor", e0);
  }

  private void parse_SquareArrayConstructor()
  {
    eventHandler.startNonterminal("SquareArrayConstructor", e0);
    consume(41);                    // '['
    lookahead1W(55);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | ']' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 42)                   // ']'
    {
      whitespace();
      parse_ExprSingle();
      for (;;)
      {
        if (l1 != 21)               // ','
        {
          break;
        }
        consume(21);                // ','
        lookahead1W(53);            // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace();
        parse_ExprSingle();
      }
    }
    consume(42);                    // ']'
    eventHandler.endNonterminal("SquareArrayConstructor", e0);
  }

  private void parse_CurlyArrayConstructor()
  {
    eventHandler.startNonterminal("CurlyArrayConstructor", e0);
    consume(46);                    // 'array'
    lookahead1W(13);                // S^WS | '(:' | '{'
    consume(104);                   // '{'
    lookahead1W(56);                // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union' | '}'
    if (l1 != 107)                  // '}'
    {
      whitespace();
      parse_Expr();
    }
    consume(107);                   // '}'
    eventHandler.endNonterminal("CurlyArrayConstructor", e0);
  }

  private void parse_UnaryLookup()
  {
    eventHandler.startNonterminal("UnaryLookup", e0);
    consume(39);                    // '?'
    lookahead1W(23);                // IntegerLiteral | NCName | S^WS | '(' | '(:' | '*'
    whitespace();
    parse_KeySpecifier();
    eventHandler.endNonterminal("UnaryLookup", e0);
  }

  private void parse_SingleType()
  {
    eventHandler.startNonterminal("SingleType", e0);
    parse_SimpleTypeName();
    lookahead1W(31);                // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | '?' | ']' | 'and' | 'castable' | 'div' |
                                    // 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' |
                                    // 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' |
                                    // 'treat' | 'union' | '|' | '||' | '}'
    if (l1 == 39)                   // '?'
    {
      consume(39);                  // '?'
    }
    eventHandler.endNonterminal("SingleType", e0);
  }

  private void parse_TypeDeclaration()
  {
    eventHandler.startNonterminal("TypeDeclaration", e0);
    consume(47);                    // 'as'
    lookahead1W(44);                // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_SequenceType();
    eventHandler.endNonterminal("TypeDeclaration", e0);
  }

  private void parse_SequenceType()
  {
    eventHandler.startNonterminal("SequenceType", e0);
    switch (l1)
    {
    case 59:                        // 'empty-sequence'
      lookahead2W(30);              // S^WS | EOF | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | '?' | ']' | 'and' | 'div' | 'else' | 'eq' |
                                    // 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' |
                                    // 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'union' | '{' | '|' |
                                    // '||' | '}'
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 2107:                      // 'empty-sequence' '('
      consume(59);                  // 'empty-sequence'
      lookahead1W(3);               // S^WS | '(' | '(:'
      consume(16);                  // '('
      lookahead1W(4);               // S^WS | '(:' | ')'
      consume(18);                  // ')'
      break;
    default:
      parse_ItemType();
      lookahead1W(28);              // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | '?' | ']' | 'and' | 'div' | 'else' | 'eq' |
                                    // 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' |
                                    // 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'union' | '{' | '|' |
                                    // '||' | '}'
      switch (l1)
      {
      case 19:                      // '*'
      case 20:                      // '+'
      case 39:                      // '?'
        whitespace();
        parse_OccurrenceIndicator();
        break;
      default:
        break;
      }
    }
    eventHandler.endNonterminal("SequenceType", e0);
  }

  private void parse_OccurrenceIndicator()
  {
    eventHandler.startNonterminal("OccurrenceIndicator", e0);
    switch (l1)
    {
    case 39:                        // '?'
      consume(39);                  // '?'
      break;
    case 19:                        // '*'
      consume(19);                  // '*'
      break;
    default:
      consume(20);                  // '+'
    }
    eventHandler.endNonterminal("OccurrenceIndicator", e0);
  }

  private void parse_ItemType()
  {
    eventHandler.startNonterminal("ItemType", e0);
    switch (l1)
    {
    case 46:                        // 'array'
    case 48:                        // 'attribute'
    case 52:                        // 'comment'
    case 56:                        // 'document-node'
    case 57:                        // 'element'
    case 66:                        // 'function'
    case 75:                        // 'item'
    case 79:                        // 'map'
    case 82:                        // 'namespace-node'
    case 84:                        // 'node'
    case 90:                        // 'processing-instruction'
    case 93:                        // 'schema-attribute'
    case 94:                        // 'schema-element'
    case 98:                        // 'text'
      lookahead2W(30);              // S^WS | EOF | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | '?' | ']' | 'and' | 'div' | 'else' | 'eq' |
                                    // 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' |
                                    // 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'union' | '{' | '|' |
                                    // '||' | '}'
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 2096:                      // 'attribute' '('
    case 2100:                      // 'comment' '('
    case 2104:                      // 'document-node' '('
    case 2105:                      // 'element' '('
    case 2130:                      // 'namespace-node' '('
    case 2132:                      // 'node' '('
    case 2138:                      // 'processing-instruction' '('
    case 2141:                      // 'schema-attribute' '('
    case 2142:                      // 'schema-element' '('
    case 2146:                      // 'text' '('
      parse_KindTest();
      break;
    case 2123:                      // 'item' '('
      consume(75);                  // 'item'
      lookahead1W(3);               // S^WS | '(' | '(:'
      consume(16);                  // '('
      lookahead1W(4);               // S^WS | '(:' | ')'
      consume(18);                  // ')'
      break;
    case 2114:                      // 'function' '('
      parse_FunctionTest();
      break;
    case 2127:                      // 'map' '('
      parse_MapTest();
      break;
    case 2094:                      // 'array' '('
      parse_ArrayTest();
      break;
    case 16:                        // '('
      parse_ParenthesizedItemType();
      break;
    default:
      parse_AtomicOrUnionType();
    }
    eventHandler.endNonterminal("ItemType", e0);
  }

  private void parse_AtomicOrUnionType()
  {
    eventHandler.startNonterminal("AtomicOrUnionType", e0);
    parse_EQName();
    eventHandler.endNonterminal("AtomicOrUnionType", e0);
  }

  private void parse_KindTest()
  {
    eventHandler.startNonterminal("KindTest", e0);
    switch (l1)
    {
    case 56:                        // 'document-node'
      parse_DocumentTest();
      break;
    case 57:                        // 'element'
      parse_ElementTest();
      break;
    case 48:                        // 'attribute'
      parse_AttributeTest();
      break;
    case 94:                        // 'schema-element'
      parse_SchemaElementTest();
      break;
    case 93:                        // 'schema-attribute'
      parse_SchemaAttributeTest();
      break;
    case 90:                        // 'processing-instruction'
      parse_PITest();
      break;
    case 52:                        // 'comment'
      parse_CommentTest();
      break;
    case 98:                        // 'text'
      parse_TextTest();
      break;
    case 82:                        // 'namespace-node'
      parse_NamespaceNodeTest();
      break;
    default:
      parse_AnyKindTest();
    }
    eventHandler.endNonterminal("KindTest", e0);
  }

  private void parse_AnyKindTest()
  {
    eventHandler.startNonterminal("AnyKindTest", e0);
    consume(84);                    // 'node'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("AnyKindTest", e0);
  }

  private void parse_DocumentTest()
  {
    eventHandler.startNonterminal("DocumentTest", e0);
    consume(56);                    // 'document-node'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(22);                // S^WS | '(:' | ')' | 'element' | 'schema-element'
    if (l1 != 18)                   // ')'
    {
      switch (l1)
      {
      case 57:                      // 'element'
        whitespace();
        parse_ElementTest();
        break;
      default:
        whitespace();
        parse_SchemaElementTest();
      }
    }
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("DocumentTest", e0);
  }

  private void parse_TextTest()
  {
    eventHandler.startNonterminal("TextTest", e0);
    consume(98);                    // 'text'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("TextTest", e0);
  }

  private void parse_CommentTest()
  {
    eventHandler.startNonterminal("CommentTest", e0);
    consume(52);                    // 'comment'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("CommentTest", e0);
  }

  private void parse_NamespaceNodeTest()
  {
    eventHandler.startNonterminal("NamespaceNodeTest", e0);
    consume(82);                    // 'namespace-node'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("NamespaceNodeTest", e0);
  }

  private void parse_PITest()
  {
    eventHandler.startNonterminal("PITest", e0);
    consume(90);                    // 'processing-instruction'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(20);                // StringLiteral | NCName | S^WS | '(:' | ')'
    if (l1 != 18)                   // ')'
    {
      switch (l1)
      {
      case 6:                       // NCName
        consume(6);                 // NCName
        break;
      default:
        consume(4);                 // StringLiteral
      }
    }
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("PITest", e0);
  }

  private void parse_AttributeTest()
  {
    eventHandler.startNonterminal("AttributeTest", e0);
    consume(48);                    // 'attribute'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(49);                // URIQualifiedName | QName^Token | S^WS | '(:' | ')' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18)                   // ')'
    {
      whitespace();
      parse_AttribNameOrWildcard();
      lookahead1W(17);              // S^WS | '(:' | ')' | ','
      if (l1 == 21)                 // ','
      {
        consume(21);                // ','
        lookahead1W(42);            // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace();
        parse_TypeName();
      }
    }
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("AttributeTest", e0);
  }

  private void parse_AttribNameOrWildcard()
  {
    eventHandler.startNonterminal("AttribNameOrWildcard", e0);
    switch (l1)
    {
    case 19:                        // '*'
      consume(19);                  // '*'
      break;
    default:
      parse_AttributeName();
    }
    eventHandler.endNonterminal("AttribNameOrWildcard", e0);
  }

  private void parse_SchemaAttributeTest()
  {
    eventHandler.startNonterminal("SchemaAttributeTest", e0);
    consume(93);                    // 'schema-attribute'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(42);                // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_AttributeDeclaration();
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("SchemaAttributeTest", e0);
  }

  private void parse_AttributeDeclaration()
  {
    eventHandler.startNonterminal("AttributeDeclaration", e0);
    parse_AttributeName();
    eventHandler.endNonterminal("AttributeDeclaration", e0);
  }

  private void parse_ElementTest()
  {
    eventHandler.startNonterminal("ElementTest", e0);
    consume(57);                    // 'element'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(49);                // URIQualifiedName | QName^Token | S^WS | '(:' | ')' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18)                   // ')'
    {
      whitespace();
      parse_ElementNameOrWildcard();
      lookahead1W(17);              // S^WS | '(:' | ')' | ','
      if (l1 == 21)                 // ','
      {
        consume(21);                // ','
        lookahead1W(42);            // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace();
        parse_TypeName();
        lookahead1W(18);            // S^WS | '(:' | ')' | '?'
        if (l1 == 39)               // '?'
        {
          consume(39);              // '?'
        }
      }
    }
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("ElementTest", e0);
  }

  private void parse_ElementNameOrWildcard()
  {
    eventHandler.startNonterminal("ElementNameOrWildcard", e0);
    switch (l1)
    {
    case 19:                        // '*'
      consume(19);                  // '*'
      break;
    default:
      parse_ElementName();
    }
    eventHandler.endNonterminal("ElementNameOrWildcard", e0);
  }

  private void parse_SchemaElementTest()
  {
    eventHandler.startNonterminal("SchemaElementTest", e0);
    consume(94);                    // 'schema-element'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(42);                // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ElementDeclaration();
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("SchemaElementTest", e0);
  }

  private void parse_ElementDeclaration()
  {
    eventHandler.startNonterminal("ElementDeclaration", e0);
    parse_ElementName();
    eventHandler.endNonterminal("ElementDeclaration", e0);
  }

  private void parse_AttributeName()
  {
    eventHandler.startNonterminal("AttributeName", e0);
    parse_EQName();
    eventHandler.endNonterminal("AttributeName", e0);
  }

  private void parse_ElementName()
  {
    eventHandler.startNonterminal("ElementName", e0);
    parse_EQName();
    eventHandler.endNonterminal("ElementName", e0);
  }

  private void parse_SimpleTypeName()
  {
    eventHandler.startNonterminal("SimpleTypeName", e0);
    parse_TypeName();
    eventHandler.endNonterminal("SimpleTypeName", e0);
  }

  private void parse_TypeName()
  {
    eventHandler.startNonterminal("TypeName", e0);
    parse_EQName();
    eventHandler.endNonterminal("TypeName", e0);
  }

  private void parse_FunctionTest()
  {
    eventHandler.startNonterminal("FunctionTest", e0);
    switch (l1)
    {
    case 66:                        // 'function'
      lookahead2W(3);               // S^WS | '(' | '(:'
      switch (lk)
      {
      case 2114:                    // 'function' '('
        lookahead3W(50);            // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | ')' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
        break;
      }
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 313410:                    // 'function' '(' '*'
      parse_AnyFunctionTest();
      break;
    default:
      parse_TypedFunctionTest();
    }
    eventHandler.endNonterminal("FunctionTest", e0);
  }

  private void parse_AnyFunctionTest()
  {
    eventHandler.startNonterminal("AnyFunctionTest", e0);
    consume(66);                    // 'function'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(5);                 // S^WS | '(:' | '*'
    consume(19);                    // '*'
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("AnyFunctionTest", e0);
  }

  private void parse_TypedFunctionTest()
  {
    eventHandler.startNonterminal("TypedFunctionTest", e0);
    consume(66);                    // 'function'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(47);                // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | ')' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18)                   // ')'
    {
      whitespace();
      parse_SequenceType();
      for (;;)
      {
        lookahead1W(17);            // S^WS | '(:' | ')' | ','
        if (l1 != 21)               // ','
        {
          break;
        }
        consume(21);                // ','
        lookahead1W(44);            // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace();
        parse_SequenceType();
      }
    }
    consume(18);                    // ')'
    lookahead1W(9);                 // S^WS | '(:' | 'as'
    consume(47);                    // 'as'
    lookahead1W(44);                // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_SequenceType();
    eventHandler.endNonterminal("TypedFunctionTest", e0);
  }

  private void parse_MapTest()
  {
    eventHandler.startNonterminal("MapTest", e0);
    switch (l1)
    {
    case 79:                        // 'map'
      lookahead2W(3);               // S^WS | '(' | '(:'
      switch (lk)
      {
      case 2127:                    // 'map' '('
        lookahead3W(45);            // URIQualifiedName | QName^Token | S^WS | '(:' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
        break;
      }
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 313423:                    // 'map' '(' '*'
      parse_AnyMapTest();
      break;
    default:
      parse_TypedMapTest();
    }
    eventHandler.endNonterminal("MapTest", e0);
  }

  private void parse_AnyMapTest()
  {
    eventHandler.startNonterminal("AnyMapTest", e0);
    consume(79);                    // 'map'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(5);                 // S^WS | '(:' | '*'
    consume(19);                    // '*'
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("AnyMapTest", e0);
  }

  private void parse_TypedMapTest()
  {
    eventHandler.startNonterminal("TypedMapTest", e0);
    consume(79);                    // 'map'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(42);                // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_AtomicOrUnionType();
    lookahead1W(6);                 // S^WS | '(:' | ','
    consume(21);                    // ','
    lookahead1W(44);                // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_SequenceType();
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("TypedMapTest", e0);
  }

  private void parse_ArrayTest()
  {
    eventHandler.startNonterminal("ArrayTest", e0);
    switch (l1)
    {
    case 46:                        // 'array'
      lookahead2W(3);               // S^WS | '(' | '(:'
      switch (lk)
      {
      case 2094:                    // 'array' '('
        lookahead3W(48);            // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
        break;
      }
      break;
    default:
      lk = l1;
    }
    switch (lk)
    {
    case 313390:                    // 'array' '(' '*'
      parse_AnyArrayTest();
      break;
    default:
      parse_TypedArrayTest();
    }
    eventHandler.endNonterminal("ArrayTest", e0);
  }

  private void parse_AnyArrayTest()
  {
    eventHandler.startNonterminal("AnyArrayTest", e0);
    consume(46);                    // 'array'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(5);                 // S^WS | '(:' | '*'
    consume(19);                    // '*'
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("AnyArrayTest", e0);
  }

  private void parse_TypedArrayTest()
  {
    eventHandler.startNonterminal("TypedArrayTest", e0);
    consume(46);                    // 'array'
    lookahead1W(3);                 // S^WS | '(' | '(:'
    consume(16);                    // '('
    lookahead1W(44);                // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_SequenceType();
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("TypedArrayTest", e0);
  }

  private void parse_ParenthesizedItemType()
  {
    eventHandler.startNonterminal("ParenthesizedItemType", e0);
    consume(16);                    // '('
    lookahead1W(44);                // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace();
    parse_ItemType();
    lookahead1W(4);                 // S^WS | '(:' | ')'
    consume(18);                    // ')'
    eventHandler.endNonterminal("ParenthesizedItemType", e0);
  }

  private void parse_FunctionEQName()
  {
    eventHandler.startNonterminal("FunctionEQName", e0);
    switch (l1)
    {
    case 5:                         // URIQualifiedName
      consume(5);                   // URIQualifiedName
      break;
    default:
      parse_FunctionName();
    }
    eventHandler.endNonterminal("FunctionEQName", e0);
  }

  private void parse_EQName()
  {
    eventHandler.startNonterminal("EQName", e0);
    switch (l1)
    {
    case 5:                         // URIQualifiedName
      consume(5);                   // URIQualifiedName
      break;
    default:
      parse_QName();
    }
    eventHandler.endNonterminal("EQName", e0);
  }

  private void try_Whitespace()
  {
    switch (l1)
    {
    case 8:                         // S^WS
      consumeT(8);                  // S^WS
      break;
    default:
      try_Comment();
    }
  }

  private void try_Comment()
  {
    consumeT(17);                   // '(:'
    for (;;)
    {
      lookahead1(14);               // CommentContents | '(:' | ':)'
      if (l1 == 28)                 // ':)'
      {
        break;
      }
      switch (l1)
      {
      case 9:                       // CommentContents
        consumeT(9);                // CommentContents
        break;
      default:
        try_Comment();
      }
    }
    consumeT(28);                   // ':)'
  }

  private void parse_FunctionName()
  {
    eventHandler.startNonterminal("FunctionName", e0);
    switch (l1)
    {
    case 7:                         // QName^Token
      consume(7);                   // QName^Token
      break;
    case 43:                        // 'ancestor'
      consume(43);                  // 'ancestor'
      break;
    case 44:                        // 'ancestor-or-self'
      consume(44);                  // 'ancestor-or-self'
      break;
    case 45:                        // 'and'
      consume(45);                  // 'and'
      break;
    case 49:                        // 'cast'
      consume(49);                  // 'cast'
      break;
    case 50:                        // 'castable'
      consume(50);                  // 'castable'
      break;
    case 51:                        // 'child'
      consume(51);                  // 'child'
      break;
    case 53:                        // 'descendant'
      consume(53);                  // 'descendant'
      break;
    case 54:                        // 'descendant-or-self'
      consume(54);                  // 'descendant-or-self'
      break;
    case 55:                        // 'div'
      consume(55);                  // 'div'
      break;
    case 58:                        // 'else'
      consume(58);                  // 'else'
      break;
    case 60:                        // 'eq'
      consume(60);                  // 'eq'
      break;
    case 61:                        // 'every'
      consume(61);                  // 'every'
      break;
    case 62:                        // 'except'
      consume(62);                  // 'except'
      break;
    case 63:                        // 'following'
      consume(63);                  // 'following'
      break;
    case 64:                        // 'following-sibling'
      consume(64);                  // 'following-sibling'
      break;
    case 65:                        // 'for'
      consume(65);                  // 'for'
      break;
    case 67:                        // 'ge'
      consume(67);                  // 'ge'
      break;
    case 68:                        // 'gt'
      consume(68);                  // 'gt'
      break;
    case 69:                        // 'idiv'
      consume(69);                  // 'idiv'
      break;
    case 72:                        // 'instance'
      consume(72);                  // 'instance'
      break;
    case 73:                        // 'intersect'
      consume(73);                  // 'intersect'
      break;
    case 74:                        // 'is'
      consume(74);                  // 'is'
      break;
    case 76:                        // 'le'
      consume(76);                  // 'le'
      break;
    case 77:                        // 'let'
      consume(77);                  // 'let'
      break;
    case 78:                        // 'lt'
      consume(78);                  // 'lt'
      break;
    case 80:                        // 'mod'
      consume(80);                  // 'mod'
      break;
    case 81:                        // 'namespace'
      consume(81);                  // 'namespace'
      break;
    case 83:                        // 'ne'
      consume(83);                  // 'ne'
      break;
    case 86:                        // 'or'
      consume(86);                  // 'or'
      break;
    case 87:                        // 'parent'
      consume(87);                  // 'parent'
      break;
    case 88:                        // 'preceding'
      consume(88);                  // 'preceding'
      break;
    case 89:                        // 'preceding-sibling'
      consume(89);                  // 'preceding-sibling'
      break;
    case 91:                        // 'return'
      consume(91);                  // 'return'
      break;
    case 92:                        // 'satisfies'
      consume(92);                  // 'satisfies'
      break;
    case 95:                        // 'self'
      consume(95);                  // 'self'
      break;
    case 96:                        // 'some'
      consume(96);                  // 'some'
      break;
    case 100:                       // 'to'
      consume(100);                 // 'to'
      break;
    case 101:                       // 'treat'
      consume(101);                 // 'treat'
      break;
    default:
      consume(103);                 // 'union'
    }
    eventHandler.endNonterminal("FunctionName", e0);
  }

  private void parse_QName()
  {
    eventHandler.startNonterminal("QName", e0);
    switch (l1)
    {
    case 46:                        // 'array'
      consume(46);                  // 'array'
      break;
    case 48:                        // 'attribute'
      consume(48);                  // 'attribute'
      break;
    case 52:                        // 'comment'
      consume(52);                  // 'comment'
      break;
    case 56:                        // 'document-node'
      consume(56);                  // 'document-node'
      break;
    case 57:                        // 'element'
      consume(57);                  // 'element'
      break;
    case 59:                        // 'empty-sequence'
      consume(59);                  // 'empty-sequence'
      break;
    case 66:                        // 'function'
      consume(66);                  // 'function'
      break;
    case 70:                        // 'if'
      consume(70);                  // 'if'
      break;
    case 75:                        // 'item'
      consume(75);                  // 'item'
      break;
    case 79:                        // 'map'
      consume(79);                  // 'map'
      break;
    case 82:                        // 'namespace-node'
      consume(82);                  // 'namespace-node'
      break;
    case 84:                        // 'node'
      consume(84);                  // 'node'
      break;
    case 90:                        // 'processing-instruction'
      consume(90);                  // 'processing-instruction'
      break;
    case 93:                        // 'schema-attribute'
      consume(93);                  // 'schema-attribute'
      break;
    case 94:                        // 'schema-element'
      consume(94);                  // 'schema-element'
      break;
    case 97:                        // 'switch'
      consume(97);                  // 'switch'
      break;
    case 98:                        // 'text'
      consume(98);                  // 'text'
      break;
    case 102:                       // 'typeswitch'
      consume(102);                 // 'typeswitch'
      break;
    default:
      parse_FunctionName();
    }
    eventHandler.endNonterminal("QName", e0);
  }

  private void consume(int t)
  {
    if (l1 == t)
    {
      whitespace();
      eventHandler.terminal(TOKEN[l1], b1, e1);
      b0 = b1; e0 = e1; l1 = l2; if (l1 != 0) {
      b1 = b2; e1 = e2; l2 = l3; if (l2 != 0) {
      b2 = b3; e2 = e3; l3 = 0; }}
    }
    else
    {
      error(b1, e1, 0, l1, t);
    }
  }

  private void consumeT(int t)
  {
    if (l1 == t)
    {
      b0 = b1; e0 = e1; l1 = l2; if (l1 != 0) {
      b1 = b2; e1 = e2; l2 = l3; if (l2 != 0) {
      b2 = b3; e2 = e3; l3 = 0; }}
    }
    else
    {
      error(b1, e1, 0, l1, t);
    }
  }

  private void skip(int code)
  {
    int b0W = b0; int e0W = e0; int l1W = l1;
    int b1W = b1; int e1W = e1; int l2W = l2;
    int b2W = b2; int e2W = e2;

    l1 = code; b1 = begin; e1 = end;
    l2 = 0;
    l3 = 0;

    try_Whitespace();

    b0 = b0W; e0 = e0W; l1 = l1W; if (l1 != 0) {
    b1 = b1W; e1 = e1W; l2 = l2W; if (l2 != 0) {
    b2 = b2W; e2 = e2W; }}
  }

  private void whitespace()
  {
    if (e0 != b1)
    {
      eventHandler.whitespace(e0, b1);
      e0 = b1;
    }
  }

  private int matchW(int set)
  {
    int code;
    for (;;)
    {
      code = match(set);
      if (code != 8)                // S^WS
      {
        if (code != 17)             // '(:'
        {
          break;
        }
        skip(code);
      }
    }
    return code;
  }

  private void lookahead1W(int set)
  {
    if (l1 == 0)
    {
      l1 = matchW(set);
      b1 = begin;
      e1 = end;
    }
  }

  private void lookahead2W(int set)
  {
    if (l2 == 0)
    {
      l2 = matchW(set);
      b2 = begin;
      e2 = end;
    }
    lk = (l2 << 7) | l1;
  }

  private void lookahead3W(int set)
  {
    if (l3 == 0)
    {
      l3 = matchW(set);
      b3 = begin;
      e3 = end;
    }
    lk |= l3 << 14;
  }

  private void lookahead1(int set)
  {
    if (l1 == 0)
    {
      l1 = match(set);
      b1 = begin;
      e1 = end;
    }
  }

  private int error(int b, int e, int s, int l, int t)
  {
    throw new ParseException(b, e, s, l, t);
  }

  private int lk, b0, e0;
  private int l1, b1, e1;
  private int l2, b2, e2;
  private int l3, b3, e3;
  private EventHandler eventHandler = null;
  private CharSequence input = null;
  private int size = 0;
  private int begin = 0;
  private int end = 0;

  private int match(int tokenSetId)
  {
    boolean nonbmp = false;
    begin = end;
    int current = end;
    int result = INITIAL[tokenSetId];
    int state = 0;

    for (int code = result & 1023; code != 0; )
    {
      int charclass;
      int c0 = current < size ? input.charAt(current) : 0;
      ++current;
      if (c0 < 0x80)
      {
        charclass = MAP0[c0];
      }
      else if (c0 < 0xd800)
      {
        int c1 = c0 >> 4;
        charclass = MAP1[(c0 & 15) + MAP1[(c1 & 31) + MAP1[c1 >> 5]]];
      }
      else
      {
        if (c0 < 0xdc00)
        {
          int c1 = current < size ? input.charAt(current) : 0;
          if (c1 >= 0xdc00 && c1 < 0xe000)
          {
            nonbmp = true;
            ++current;
            c0 = ((c0 & 0x3ff) << 10) + (c1 & 0x3ff) + 0x10000;
          }
          else
          {
            c0 = -1;
          }
        }

        int lo = 0, hi = 5;
        for (int m = 3; ; m = (hi + lo) >> 1)
        {
          if (MAP2[m] > c0) {hi = m - 1;}
          else if (MAP2[6 + m] < c0) {lo = m + 1;}
          else {charclass = MAP2[12 + m]; break;}
          if (lo > hi) {charclass = 0; break;}
        }
      }

      state = code;
      int i0 = 828 * charclass + code - 1;
      int i1 = i0 >> 2;
      code = TRANSITION[(i0 & 3) + TRANSITION[(i1 & 7) + TRANSITION[i1 >> 3]]];

      if (code > 1023)
      {
        result = code;
        code &= 1023;
        end = current;
      }
    }

    result >>= 10;
    if (result == 0)
    {
      end = current - 1;
      int c1 = end < size ? input.charAt(end) : 0;
      if (c1 >= 0xdc00 && c1 < 0xe000)
      {
        --end;
      }
      return error(begin, end, state, -1, -1);
    }
    else if (nonbmp)
    {
      for (int i = result >> 7; i > 0; --i)
      {
        --end;
        int c1 = end < size ? input.charAt(end) : 0;
        if (c1 >= 0xdc00 && c1 < 0xe000)
        {
          --end;
        }
      }
    }
    else
    {
      end -= result >> 7;
    }

    if (end > size) end = size;
    return (result & 127) - 1;
  }

  private static String[] getTokenSet(int tokenSetId)
  {
    java.util.ArrayList<String> expected = new java.util.ArrayList<>();
    int s = tokenSetId < 0 ? - tokenSetId : INITIAL[tokenSetId] & 1023;
    for (int i = 0; i < 108; i += 32)
    {
      int j = i;
      int i0 = (i >> 5) * 828 + s - 1;
      int i1 = i0 >> 1;
      int f = EXPECTED[(i0 & 1) + EXPECTED[(i1 & 3) + EXPECTED[i1 >> 2]]];
      for ( ; f != 0; f >>>= 1, ++j)
      {
        if ((f & 1) != 0)
        {
          expected.add(TOKEN[j]);
        }
      }
    }
    return expected.toArray(new String[]{});
  }

  private static final int[] MAP0 = new int[128];
  static
  {
    final String s1[] =
    {
      /*   0 */ "55, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2",
      /*  34 */ "3, 4, 5, 6, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 17, 6, 18, 19",
      /*  62 */ "20, 21, 22, 23, 23, 23, 23, 24, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 25, 23, 23, 23, 23, 23",
      /*  87 */ "23, 23, 23, 23, 26, 6, 27, 6, 23, 6, 28, 29, 30, 31, 32, 33, 34, 35, 36, 23, 23, 37, 38, 39, 40, 41",
      /* 113 */ "42, 43, 44, 45, 46, 47, 48, 49, 50, 23, 51, 52, 53, 6, 6"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 128; ++i) {MAP0[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] MAP1 = new int[455];
  static
  {
    final String s1[] =
    {
      /*   0 */ "108, 124, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 156, 181, 181, 181",
      /*  20 */ "181, 181, 214, 215, 213, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214",
      /*  40 */ "214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214",
      /*  60 */ "214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214",
      /*  80 */ "214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214",
      /* 100 */ "214, 214, 214, 214, 214, 214, 214, 214, 247, 261, 277, 293, 309, 331, 370, 386, 422, 422, 422, 414",
      /* 120 */ "354, 346, 354, 346, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354",
      /* 140 */ "439, 439, 439, 439, 439, 439, 439, 315, 354, 354, 354, 354, 354, 354, 354, 354, 400, 422, 422, 423",
      /* 160 */ "421, 422, 422, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354",
      /* 180 */ "354, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422",
      /* 200 */ "422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 353, 354, 354, 354, 354, 354, 354",
      /* 220 */ "354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354",
      /* 240 */ "354, 354, 354, 354, 354, 354, 422, 55, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0",
      /* 269 */ "0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 16, 16, 16",
      /* 299 */ "16, 16, 16, 16, 17, 6, 18, 19, 20, 21, 22, 23, 23, 23, 23, 24, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23",
      /* 325 */ "23, 23, 23, 23, 6, 23, 23, 25, 23, 23, 23, 23, 23, 23, 23, 23, 23, 26, 6, 27, 6, 23, 23, 23, 23, 23",
      /* 351 */ "23, 23, 6, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 6, 28, 29, 30, 31, 32, 33",
      /* 377 */ "34, 35, 36, 23, 23, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 23, 51, 52, 53, 6, 6, 6",
      /* 403 */ "6, 6, 6, 6, 6, 6, 6, 6, 6, 23, 23, 6, 6, 6, 6, 6, 6, 6, 54, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6",
      /* 436 */ "6, 6, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 455; ++i) {MAP1[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] MAP2 = new int[18];
  static
  {
    final String s1[] =
    {
      /*  0 */ "57344, 63744, 64976, 65008, 65536, 983040, 63743, 64975, 65007, 65533, 983039, 1114111, 6, 23, 6, 23",
      /* 16 */ "23, 6"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 18; ++i) {MAP2[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] INITIAL = new int[58];
  static
  {
    final String s1[] =
    {
      /*  0 */ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28",
      /* 28 */ "29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54",
      /* 54 */ "55, 56, 57, 58"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 58; ++i) {INITIAL[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] TRANSITION = new int[7908];
  static
  {
    final String s1[] =
    {
      /*    0 */ "1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489",
      /*   17 */ "1489, 1489, 1489, 1489, 1489, 1489, 1489, 1489, 1490, 1449, 1452, 2443, 2451, 1460, 1530, 1592, 1600",
      /*   34 */ "1608, 1616, 1624, 1632, 1640, 1648, 1656, 1664, 1672, 1680, 1688, 1696, 1704, 1712, 1720, 1728, 1736",
      /*   51 */ "1744, 1470, 1478, 2444, 2452, 1461, 1531, 1593, 1601, 1609, 1617, 1625, 1633, 1641, 1649, 1657, 1665",
      /*   68 */ "1673, 1681, 1689, 1697, 1705, 1713, 1721, 1729, 1737, 1745, 1486, 1498, 2445, 2453, 1524, 1532, 1594",
      /*   85 */ "1602, 1610, 1618, 1626, 1634, 1642, 1650, 1658, 1666, 1674, 1682, 1690, 1698, 1706, 1714, 1722, 1730",
      /*  102 */ "1738, 1544, 3047, 1820, 2446, 2454, 1525, 1533, 1595, 1603, 1611, 1619, 1627, 1635, 1643, 1651, 1659",
      /*  119 */ "1667, 1675, 1683, 1691, 1699, 1707, 1715, 1723, 1731, 1739, 1552, 3862, 1560, 2447, 2455, 1526, 1534",
      /*  136 */ "1596, 1604, 1612, 1620, 1628, 1636, 1644, 1652, 1660, 1668, 1676, 1684, 1692, 1700, 1708, 1716, 1724",
      /*  153 */ "1732, 1740, 1748, 1489, 1581, 2448, 2456, 1527, 1535, 1597, 1605, 1613, 1621, 1629, 1637, 1645, 1653",
      /*  170 */ "1661, 1669, 1677, 1685, 1693, 1701, 1709, 1717, 1725, 1733, 1741, 1574, 4723, 1815, 2449, 2410, 1528",
      /*  187 */ "1590, 1598, 1606, 1614, 1622, 1630, 1638, 1646, 1654, 1662, 1670, 1678, 1686, 1694, 1702, 1710, 1718",
      /*  204 */ "1726, 1734, 1742, 1756, 1763, 1783, 2450, 1794, 1529, 1591, 1599, 1607, 1615, 1623, 1631, 1639, 1647",
      /*  221 */ "1655, 1663, 1671, 1679, 1687, 1695, 1703, 1711, 1719, 1727, 1735, 1743, 1802, 1809, 1828, 2451, 1836",
      /*  238 */ "1530, 1592, 1600, 1608, 1616, 1624, 1632, 1640, 1648, 1656, 1664, 1672, 1680, 1688, 1696, 1704, 1712",
      /*  255 */ "1720, 1728, 1736, 1848, 1856, 1864, 2444, 2452, 1461, 1531, 1872, 1601, 1609, 2122, 1625, 1633, 1641",
      /*  272 */ "1649, 1657, 1665, 1673, 1681, 1689, 1697, 1705, 1713, 1721, 1729, 1737, 1745, 2521, 4444, 2445, 2453",
      /*  289 */ "1959, 1532, 1594, 4892, 1610, 1618, 1626, 1634, 1642, 1650, 1658, 1666, 1674, 1682, 1690, 1698, 1706",
      /*  306 */ "1714, 1722, 1730, 1738, 1880, 1894, 3835, 2446, 2454, 1525, 1533, 1595, 1603, 1611, 1619, 1627, 1635",
      /*  323 */ "1643, 1651, 1659, 1667, 1675, 1683, 1691, 1699, 1707, 1715, 1723, 1731, 1739, 1747, 1916, 1942, 4086",
      /*  340 */ "1953, 1774, 2167, 1973, 4790, 2165, 2570, 2180, 3162, 3699, 4801, 1994, 4235, 4518, 2002, 2699, 2022",
      /*  357 */ "2030, 2050, 3224, 2069, 2077, 2085, 1786, 2094, 4087, 4318, 1775, 2167, 1974, 4377, 2166, 2571, 2181",
      /*  374 */ "3163, 3781, 4802, 3291, 2751, 3700, 2003, 4180, 2009, 4142, 4150, 3349, 2070, 2078, 2086, 2107, 1582",
      /*  391 */ "2115, 2457, 1528, 1590, 1598, 1606, 1614, 1622, 1630, 1638, 1646, 1654, 1662, 1670, 1678, 1686, 1694",
      /*  408 */ "1702, 1710, 1718, 1726, 1734, 1742, 2130, 2912, 3485, 4089, 2148, 2137, 2686, 4302, 2160, 2167, 2178",
      /*  425 */ "4269, 2167, 3783, 4804, 3293, 2753, 3702, 2005, 4182, 2011, 4144, 4152, 3351, 2072, 2080, 2189, 2197",
      /*  442 */ "2205, 2255, 2285, 1965, 2213, 2227, 2235, 2250, 2263, 2219, 2242, 2278, 4844, 2299, 2307, 2270, 4850",
      /*  459 */ "2315, 2322, 2330, 2337, 2344, 2352, 2360, 2380, 2388, 1505, 2452, 1461, 1531, 1593, 1601, 1609, 1617",
      /*  476 */ "1625, 1633, 1641, 1649, 1657, 1665, 1673, 1681, 1689, 1697, 1705, 1713, 1721, 1729, 1737, 1745, 2396",
      /*  493 */ "4111, 2404, 2453, 1959, 1532, 1594, 1602, 1610, 1618, 1626, 1634, 1642, 1650, 1658, 1666, 1674, 1682",
      /*  510 */ "1690, 1698, 1706, 1714, 1722, 1730, 1738, 1746, 1901, 1908, 4631, 2454, 1525, 1533, 1595, 1603, 1611",
      /*  527 */ "1619, 1627, 1635, 1643, 1651, 1659, 1667, 1675, 1683, 1691, 1699, 1707, 1715, 1723, 1731, 1739, 2418",
      /*  544 */ "3842, 2426, 2447, 2455, 1526, 1534, 1596, 1604, 1612, 1620, 1628, 1636, 1644, 1652, 1660, 1668, 1676",
      /*  561 */ "1684, 1692, 1700, 1708, 1716, 1724, 1732, 1740, 1748, 4381, 2440, 2448, 2456, 1527, 1535, 1597, 1605",
      /*  578 */ "1613, 1621, 1629, 1637, 1645, 1653, 1661, 1669, 1677, 1685, 1693, 1701, 1709, 1717, 1725, 1733, 1741",
      /*  595 */ "2465, 2614, 3484, 4088, 2627, 3504, 3165, 4302, 1980, 2167, 4299, 3787, 3164, 3782, 4803, 3292, 2752",
      /*  612 */ "3701, 2004, 4181, 2010, 4143, 4151, 3350, 2071, 2079, 2466, 2615, 3485, 4089, 3043, 3505, 3166, 4302",
      /*  629 */ "2477, 2167, 4300, 3788, 2167, 3783, 4804, 3293, 2753, 3702, 2005, 4182, 2011, 4144, 4152, 3351, 2072",
      /*  646 */ "2080, 2467, 2490, 3486, 4090, 3498, 3506, 3167, 4302, 1982, 2167, 4301, 3789, 2168, 1975, 4805, 3294",
      /*  663 */ "2754, 3703, 4679, 4183, 2012, 4145, 4153, 3352, 2073, 2081, 1886, 2498, 2444, 2452, 1461, 1531, 1593",
      /*  680 */ "1601, 1609, 1617, 1625, 1633, 1641, 1649, 1657, 1665, 1673, 1681, 1689, 1697, 1705, 1713, 1721, 1729",
      /*  697 */ "1737, 1745, 2506, 4822, 2445, 2453, 1959, 1532, 1594, 1602, 1610, 1618, 1626, 1634, 1642, 1650, 1658",
      /*  714 */ "1666, 1674, 1682, 1690, 1698, 1706, 1714, 1722, 1730, 1738, 2514, 2529, 2537, 2891, 2548, 3501, 2167",
      /*  731 */ "3233, 1977, 2566, 4746, 4129, 3554, 4017, 2579, 4006, 3324, 3309, 3914, 2594, 2007, 4140, 4148, 4156",
      /*  748 */ "3355, 2076, 2084, 2612, 3144, 4086, 2625, 3502, 2167, 3234, 1978, 1986, 3511, 3785, 3162, 3699, 3760",
      /*  765 */ "4757, 2750, 3699, 4333, 4179, 2008, 4141, 4149, 2635, 3356, 2077, 2654, 2663, 2671, 3150, 3989, 3524",
      /*  782 */ "2684, 2694, 1979, 2707, 2725, 2733, 3163, 2745, 2763, 3291, 3976, 4196, 2773, 2786, 3694, 4142, 2968",
      /*  799 */ "4561, 2070, 2042, 3366, 2794, 2540, 2802, 2815, 2838, 3735, 4711, 1980, 2167, 4299, 3787, 3007, 4581",
      /*  816 */ "4803, 2856, 2056, 4607, 2004, 4181, 2010, 4143, 2864, 2872, 2071, 2079, 4324, 2884, 4216, 2899, 2907",
      /*  833 */ "2920, 2935, 2955, 2976, 2984, 2992, 3000, 3017, 3036, 4169, 3898, 3055, 3098, 3069, 3077, 3092, 3106",
      /*  850 */ "3616, 3114, 2876, 2080, 2467, 3129, 3486, 3137, 3498, 3506, 3167, 4302, 1982, 2140, 4301, 3158, 2168",
      /*  867 */ "1975, 3176, 4485, 2754, 3703, 4679, 4183, 2012, 4145, 4153, 3352, 2037, 3184, 3195, 3203, 3487, 4091",
      /*  884 */ "3499, 3507, 3168, 1975, 1983, 3508, 4302, 3790, 2169, 1976, 4806, 1976, 2755, 3704, 4680, 3211, 3434",
      /*  901 */ "4146, 4154, 3353, 4693, 2082, 2469, 4566, 2152, 2826, 2555, 2167, 3232, 3884, 1984, 3509, 4302, 3791",
      /*  918 */ "2170, 1977, 4362, 2717, 2170, 3705, 4681, 2006, 4256, 4147, 4155, 3354, 2075, 2947, 3243, 3251, 2676",
      /*  935 */ "3259, 4878, 3267, 3279, 2848, 3287, 4296, 4584, 4273, 2604, 4800, 4480, 3302, 3317, 3339, 3218, 2007",
      /*  952 */ "4140, 2778, 3347, 3820, 2076, 3364, 3374, 3382, 3674, 3390, 3502, 3400, 4060, 3408, 2482, 3271, 3419",
      /*  969 */ "3162, 3699, 4801, 3290, 4029, 2713, 4333, 3597, 3429, 4141, 4149, 3927, 3442, 3460, 2654, 3475, 3483",
      /*  986 */ "4645, 3495, 2558, 3962, 1934, 3519, 3532, 3679, 3547, 4742, 3562, 3579, 3291, 2751, 3700, 2003, 4180",
      /* 1003 */ "2009, 3590, 3609, 3349, 2070, 2078, 3630, 3638, 3646, 3654, 3669, 1566, 3165, 4786, 4663, 2167, 4299",
      /* 1020 */ "3421, 4503, 3687, 3713, 3721, 3743, 3757, 2586, 3768, 3776, 3799, 3807, 3815, 3121, 3452, 2432, 3828",
      /* 1037 */ "1945, 3850, 3858, 3505, 1928, 3872, 3411, 2167, 3870, 3661, 2600, 3880, 4804, 3293, 3892, 3910, 2005",
      /* 1054 */ "4182, 4771, 2962, 3922, 3351, 2072, 3935, 2467, 3949, 3486, 4090, 3498, 3957, 3970, 3984, 3997, 2167",
      /* 1071 */ "4301, 4737, 4014, 4025, 3582, 3028, 2754, 3703, 4679, 4183, 2012, 4145, 4153, 3352, 2073, 2081, 2468",
      /* 1088 */ "2617, 4037, 4649, 3499, 3507, 3168, 1975, 1983, 3508, 4302, 3790, 2169, 1976, 4806, 1976, 2755, 3704",
      /* 1105 */ "4680, 4045, 2013, 4146, 4154, 3353, 2074, 2082, 4068, 4076, 4864, 4099, 3500, 4119, 2061, 3567, 4667",
      /* 1122 */ "3009, 4127, 4499, 4137, 4164, 4807, 2748, 2170, 4051, 4177, 2006, 2014, 4191, 3084, 3354, 3448, 2083",
      /* 1139 */ "4204, 4212, 4224, 2807, 4243, 4251, 3331, 1977, 1985, 3510, 3784, 4412, 4264, 4281, 3023, 4289, 4311",
      /* 1156 */ "4332, 3902, 4341, 2646, 2642, 4356, 4370, 2076, 3187, 4389, 4397, 4465, 4405, 4424, 3061, 4452, 4460",
      /* 1173 */ "4473, 4348, 4493, 4511, 4431, 3728, 4530, 4538, 2843, 4546, 4554, 4574, 4592, 4149, 3749, 4600, 3467",
      /* 1190 */ "2654, 4618, 4626, 2821, 4639, 3503, 2167, 3235, 4610, 4002, 4657, 4438, 3163, 3781, 4802, 3291, 3571",
      /* 1207 */ "4231, 2003, 4180, 4522, 4675, 4150, 3622, 4689, 2942, 2655, 2614, 3484, 4701, 3392, 1922, 3165, 4709",
      /* 1224 */ "2830, 4416, 2927, 3787, 3164, 3782, 4803, 3292, 2752, 3701, 2004, 4181, 2010, 4143, 4151, 3350, 2071",
      /* 1241 */ "2079, 2466, 2615, 3485, 3601, 4719, 3505, 3166, 4302, 1981, 2167, 4300, 3788, 2167, 3783, 2765, 4731",
      /* 1258 */ "4754, 3702, 2005, 4182, 2011, 4144, 4152, 3351, 2072, 2080, 2467, 2616, 4082, 4765, 3498, 3506, 4056",
      /* 1275 */ "4303, 1982, 2167, 4301, 3789, 2168, 1975, 4805, 3294, 2754, 3703, 4679, 4183, 2012, 4145, 4153, 3352",
      /* 1292 */ "2073, 2081, 2468, 2617, 3487, 3539, 3499, 3507, 3168, 1975, 1983, 3508, 4302, 2737, 4779, 4798, 4806",
      /* 1309 */ "1976, 2755, 3704, 4680, 2005, 2013, 4146, 4154, 3353, 2074, 2082, 4815, 1819, 2099, 1512, 1462, 1532",
      /* 1326 */ "2372, 1602, 1610, 1618, 1626, 1634, 1642, 1650, 1658, 1666, 1674, 1682, 1690, 1698, 1706, 1714, 1722",
      /* 1343 */ "1730, 1738, 1746, 4830, 4837, 2366, 2454, 1525, 1533, 1595, 1603, 1611, 1619, 1627, 1635, 1643, 1651",
      /* 1360 */ "1659, 1667, 1675, 1683, 1691, 1699, 1707, 1715, 1723, 1731, 1739, 1747, 4858, 4872, 2447, 2455, 1840",
      /* 1377 */ "1534, 4886, 1604, 1612, 1620, 1628, 1636, 1644, 1652, 1660, 1668, 1676, 1684, 1692, 1700, 1708, 1716",
      /* 1394 */ "1724, 1732, 1740, 1748, 1489, 4106, 4087, 2626, 1775, 2167, 1974, 1979, 2166, 2571, 3786, 3163, 3781",
      /* 1411 */ "4802, 3291, 2751, 3700, 2003, 4180, 2009, 4142, 4150, 3349, 2070, 2078, 3941, 4900, 1770, 2449, 1516",
      /* 1428 */ "2291, 1536, 1598, 1606, 1614, 1622, 1630, 1638, 1646, 1654, 1662, 1670, 1678, 1686, 1694, 1702, 1710",
      /* 1445 */ "1718, 1726, 1734, 1742, 4911, 4911, 4908, 4911, 4911, 4911, 4911, 4911, 4912, 4916, 6524, 6565, 4927",
      /* 1462 */ "6280, 4917, 6751, 4931, 4937, 4946, 4953, 4917, 4917, 6525, 4917, 4917, 7825, 7826, 5135, 5136, 5140",
      /* 1479 */ "4917, 4917, 4917, 7902, 4916, 6524, 7784, 6525, 4917, 5149, 4917, 4917, 4917, 4917, 4917, 4917, 4917",
      /* 1496 */ "4917, 4911, 4917, 5150, 5146, 5148, 4916, 6524, 5154, 4922, 5759, 4917, 4917, 4917, 4917, 6563, 4969",
      /* 1513 */ "4969, 7837, 4969, 4969, 4969, 6565, 4927, 4917, 4917, 6751, 4931, 7783, 4917, 6751, 4931, 4937, 4946",
      /* 1530 */ "4953, 5905, 4969, 4969, 5121, 4969, 4958, 4963, 4968, 4974, 4969, 4969, 4979, 4948, 5100, 5126, 4969",
      /* 1547 */ "5050, 5175, 4917, 4917, 6911, 5126, 4969, 5050, 6917, 4917, 4917, 6525, 5190, 5187, 5189, 4916, 6524",
      /* 1564 */ "7784, 4922, 4917, 4917, 4917, 5813, 5164, 5164, 5164, 6319, 5050, 4917, 4917, 4917, 6525, 4917, 5160",
      /* 1581 */ "4917, 4916, 6524, 7784, 4922, 4917, 4917, 4917, 4917, 4958, 4963, 4968, 4974, 4969, 4969, 4979, 5445",
      /* 1598 */ "4969, 4969, 5122, 5025, 4959, 4964, 5096, 4975, 4969, 7842, 4970, 4917, 7869, 6786, 4917, 4949, 7377",
      /* 1615 */ "4969, 5120, 4969, 5041, 5391, 5395, 4969, 4969, 4969, 7375, 4969, 5118, 4969, 5039, 4969, 5393, 4969",
      /* 1632 */ "4969, 4969, 6566, 4984, 4988, 4917, 4994, 5093, 4969, 5024, 5085, 5089, 4969, 5106, 5110, 4969, 5095",
      /* 1649 */ "5022, 4969, 5087, 5091, 4969, 5108, 6565, 4917, 5002, 5006, 7857, 5042, 4969, 4969, 4969, 5011, 5007",
      /* 1666 */ "7858, 5043, 4969, 4969, 4969, 5012, 5016, 4948, 4969, 5396, 4969, 5020, 5059, 4969, 4969, 5029, 7859",
      /* 1683 */ "5057, 5061, 4917, 6564, 5114, 4969, 4969, 4969, 5112, 5116, 4969, 4969, 4997, 5047, 5130, 5072, 4969",
      /* 1700 */ "4980, 5129, 5071, 4969, 4969, 5054, 7381, 5033, 5037, 7379, 5031, 5035, 6565, 7840, 4969, 5103, 7841",
      /* 1717 */ "4969, 5065, 4969, 4969, 4969, 4969, 6565, 4969, 4969, 4969, 4969, 4969, 5067, 4969, 4969, 5069, 4969",
      /* 1734 */ "7384, 5077, 7383, 5076, 7382, 4969, 4969, 4969, 5083, 5081, 5131, 5100, 5100, 5126, 4969, 5050, 4917",
      /* 1751 */ "4917, 4917, 6525, 4917, 4917, 5211, 5210, 5210, 5199, 5210, 5211, 5209, 5212, 5203, 5208, 5202, 5202",
      /* 1768 */ "5204, 5216, 4916, 4917, 4917, 4922, 4917, 4917, 4917, 4917, 4917, 5905, 5164, 5164, 5164, 6645, 5222",
      /* 1785 */ "4922, 4917, 4917, 4917, 4917, 4917, 4917, 7216, 5364, 4969, 6565, 4927, 7822, 4917, 6751, 4931, 4937",
      /* 1802 */ "5229, 4917, 6525, 5227, 5227, 5226, 5226, 5226, 5228, 7064, 5234, 7065, 7066, 4916, 6524, 5195, 4922",
      /* 1819 */ "4917, 4917, 4917, 4917, 4916, 6524, 7784, 4922, 4917, 5241, 4922, 4917, 4917, 4917, 4917, 4917, 6563",
      /* 1836 */ "6565, 4927, 6526, 4917, 6751, 4931, 4937, 4946, 4953, 5414, 4969, 4969, 5131, 5100, 5100, 5126, 4969",
      /* 1853 */ "5050, 4917, 5245, 4917, 6525, 4917, 7194, 5254, 5254, 5254, 5254, 5259, 5245, 5255, 5262, 5264, 4916",
      /* 1870 */ "6524, 7784, 4974, 4969, 4969, 4979, 7854, 4969, 4969, 5122, 5100, 5126, 4969, 5050, 4917, 6512, 4917",
      /* 1887 */ "6525, 4917, 4917, 4917, 4917, 5870, 5871, 6513, 6513, 5289, 5289, 5289, 5289, 5291, 4917, 4917, 5800",
      /* 1904 */ "5801, 5801, 5801, 5805, 4917, 4917, 5811, 4916, 6524, 7784, 4922, 5817, 4917, 5297, 5298, 5298, 5298",
      /* 1921 */ "5302, 4917, 4917, 4917, 5813, 5164, 6296, 5164, 5164, 5164, 7115, 5164, 5164, 5842, 5310, 6843, 5310",
      /* 1938 */ "5310, 5310, 5310, 6190, 5297, 5302, 4916, 6524, 7784, 5904, 4917, 4917, 7063, 7103, 7103, 5309, 5310",
      /* 1955 */ "5310, 5310, 5168, 4916, 6280, 4917, 6751, 4931, 4937, 4946, 4953, 5905, 7874, 7874, 5473, 7874, 5454",
      /* 1972 */ "5643, 5164, 5432, 5310, 5310, 5310, 5310, 5310, 5310, 5310, 5168, 4917, 4917, 4917, 4917, 5819, 5164",
      /* 1989 */ "5164, 5164, 5164, 5164, 5164, 7077, 5164, 5164, 5164, 5164, 5310, 5310, 5315, 5326, 4917, 5163, 5164",
      /* 2006 */ "5164, 5164, 5165, 5310, 5310, 5310, 5310, 5161, 5164, 5164, 5164, 5167, 5310, 5310, 5168, 7283, 7522",
      /* 2023 */ "7093, 5310, 5310, 5310, 5161, 5164, 5332, 5164, 5167, 7268, 5336, 5168, 5342, 7690, 5165, 5310, 5310",
      /* 2040 */ "6536, 6243, 5310, 5164, 5166, 5310, 7502, 7588, 5166, 6016, 5348, 7355, 5960, 5164, 5164, 5353, 5310",
      /* 2057 */ "5168, 4917, 5161, 7284, 5164, 5164, 5164, 5842, 7255, 5310, 5310, 7651, 5359, 5310, 5328, 5164, 5165",
      /* 2074 */ "5310, 5310, 5164, 5166, 5310, 5164, 5166, 5310, 5164, 5310, 5166, 6016, 6016, 4917, 4917, 4917, 6525",
      /* 2091 */ "4917, 4917, 4917, 5366, 4916, 6524, 7784, 5904, 4917, 4917, 4917, 4917, 4917, 6635, 4969, 4969, 4917",
      /* 2108 */ "5377, 5378, 5382, 4917, 4917, 5378, 5383, 4917, 5389, 4969, 4969, 4969, 4969, 6564, 4969, 5041, 5391",
      /* 2125 */ "5395, 4969, 4969, 4969, 7866, 5400, 4917, 4917, 6525, 4917, 4918, 5400, 4917, 4917, 5905, 5164, 5164",
      /* 2142 */ "5164, 5164, 5164, 5164, 6517, 5164, 5310, 5946, 5408, 6280, 4917, 4917, 4917, 4917, 4917, 5162, 6482",
      /* 2159 */ "5164, 5310, 5946, 5413, 4917, 4917, 4917, 5807, 5164, 5164, 5164, 5164, 5164, 5164, 5164, 5164, 5167",
      /* 2176 */ "5310, 5310, 5164, 5308, 5310, 5310, 5310, 5310, 5310, 5310, 6072, 4917, 4917, 4923, 5418, 7815, 4917",
      /* 2193 */ "4917, 5423, 5424, 5425, 5426, 5430, 4917, 4917, 4917, 5436, 5443, 6756, 5439, 4922, 4917, 4917, 4917",
      /* 2210 */ "4917, 4917, 7872, 5458, 5463, 7874, 7874, 5472, 5749, 5538, 5538, 7879, 4984, 4988, 4917, 5524, 5562",
      /* 2227 */ "5684, 5539, 5477, 5485, 5567, 5489, 5538, 5729, 5450, 4917, 7869, 6786, 4917, 4949, 5560, 7874, 5531",
      /* 2244 */ "5507, 5650, 7874, 5627, 5707, 7877, 5495, 7874, 5500, 5533, 5505, 7874, 7874, 7874, 7874, 5449, 5538",
      /* 2261 */ "5538, 5538, 6740, 5538, 5618, 5538, 5514, 5538, 5511, 5538, 5602, 5672, 5697, 5608, 4917, 7873, 5612",
      /* 2278 */ "5537, 5738, 5538, 5543, 5547, 5538, 5553, 7878, 4927, 6757, 4917, 6751, 4931, 4937, 4946, 4953, 4917",
      /* 2295 */ "4969, 4969, 5121, 4969, 5720, 5727, 5566, 5517, 5538, 5538, 5538, 5571, 5016, 7871, 7874, 5501, 7874",
      /* 2312 */ "5657, 5598, 7877, 5622, 5631, 5640, 7874, 5459, 5736, 5700, 5538, 5647, 5654, 5663, 5691, 5468, 5520",
      /* 2329 */ "5667, 7878, 5634, 7874, 5659, 5671, 5538, 5676, 7874, 7877, 5538, 7878, 7874, 7874, 7876, 5538, 5678",
      /* 2346 */ "7874, 7875, 5682, 5538, 5688, 5704, 5709, 5713, 5549, 7874, 7876, 5538, 5717, 5694, 5491, 5637, 5724",
      /* 2363 */ "5733, 5466, 5527, 4917, 4917, 4933, 4917, 6563, 4969, 4969, 4969, 4979, 4948, 4969, 4969, 5122, 5025",
      /* 2380 */ "4917, 6525, 4917, 4917, 5742, 5743, 5743, 5743, 5747, 4917, 4917, 4917, 5753, 4916, 6524, 7784, 6525",
      /* 2397 */ "4917, 4917, 5764, 5765, 5769, 5769, 5771, 5794, 4917, 4917, 4917, 4917, 6563, 4969, 4969, 6565, 4927",
      /* 2414 */ "6280, 5160, 6751, 4931, 5126, 4969, 5050, 4917, 4917, 4917, 6525, 7344, 5823, 5828, 4916, 6524, 7784",
      /* 2431 */ "4922, 4917, 4917, 4998, 6525, 4917, 5901, 7044, 7045, 5836, 4916, 6524, 7784, 4922, 4917, 4917, 4917",
      /* 2448 */ "4917, 4917, 6563, 4969, 4969, 4969, 4969, 6564, 4969, 4969, 4969, 6565, 4927, 6280, 4917, 6751, 4931",
      /* 2465 */ "5050, 4917, 4917, 4917, 6525, 4917, 5901, 5904, 4917, 4917, 4917, 6458, 5310, 5971, 4917, 4917, 4917",
      /* 2482 */ "4917, 5819, 5164, 6766, 5164, 6640, 5164, 5164, 4917, 5142, 5857, 5858, 5862, 5864, 4917, 6524, 5875",
      /* 2499 */ "4917, 7737, 5871, 5875, 4916, 6524, 7784, 6525, 4917, 4917, 5881, 5882, 5882, 5882, 5886, 5100, 5126",
      /* 2516 */ "4969, 5050, 4917, 4917, 4940, 6525, 4917, 4917, 5270, 5271, 5271, 5271, 5275, 4954, 5898, 5909, 5910",
      /* 2533 */ "5910, 5910, 5914, 5925, 5926, 5917, 5919, 4917, 6524, 7784, 5904, 4917, 5247, 4917, 4917, 7500, 5309",
      /* 2550 */ "5945, 5349, 5950, 5959, 4917, 6280, 5755, 4917, 4917, 4917, 4917, 4917, 5813, 5164, 6831, 5164, 4917",
      /* 2567 */ "5760, 5819, 6679, 5164, 5164, 5164, 5164, 5308, 5310, 5310, 5310, 5310, 5992, 5310, 5310, 5310, 5168",
      /* 2584 */ "5998, 4917, 5163, 5164, 6972, 7579, 5165, 5310, 7010, 6982, 6999, 5310, 5310, 7015, 5170, 5162, 5164",
      /* 2601 */ "5164, 5164, 7132, 5164, 5164, 5164, 5164, 6039, 5310, 5310, 5310, 5901, 5904, 4917, 4917, 4917, 6458",
      /* 2618 */ "5164, 5165, 5310, 5169, 4917, 6524, 7784, 5309, 5310, 5310, 5310, 5168, 4917, 6280, 4917, 4917, 4917",
      /* 2635 */ "5164, 6031, 6038, 7508, 6043, 5163, 5164, 5165, 7256, 7423, 5327, 5164, 5164, 5167, 5310, 5310, 5168",
      /* 2652 */ "5164, 7414, 6016, 6016, 4917, 4917, 4917, 6525, 4917, 5901, 5904, 5904, 5237, 6052, 6052, 6054, 6058",
      /* 2669 */ "6059, 6063, 6065, 4917, 6524, 7784, 6212, 4917, 4917, 4917, 4917, 5162, 6010, 5164, 5164, 6086, 5164",
      /* 2686 */ "5164, 5164, 5164, 5164, 5164, 5164, 5432, 5310, 6092, 5310, 6097, 5310, 6096, 5310, 5310, 5310, 5170",
      /* 2703 */ "7128, 7644, 5164, 5164, 5819, 5164, 6593, 5164, 5164, 6101, 5164, 5164, 5164, 7299, 5310, 5310, 5310",
      /* 2720 */ "5310, 6963, 5168, 4917, 5161, 6107, 5164, 5164, 5851, 5310, 6113, 5310, 6489, 5310, 5310, 6453, 5310",
      /* 2737 */ "5310, 5169, 4917, 4917, 4917, 5888, 7793, 5164, 5164, 6119, 5167, 5310, 5310, 5310, 5310, 5310, 5168",
      /* 2754 */ "4917, 5161, 5164, 5164, 5164, 5164, 5164, 5167, 5310, 5310, 7776, 5168, 4917, 4917, 5163, 5164, 5164",
      /* 2771 */ "7765, 5164, 7600, 5163, 5164, 5164, 6129, 5165, 5310, 5310, 5327, 7707, 6698, 5167, 6702, 7151, 5310",
      /* 2788 */ "5170, 5162, 5164, 5164, 5164, 7752, 6142, 6142, 6142, 6146, 6157, 6158, 6149, 6151, 5249, 5162, 5164",
      /* 2805 */ "7803, 5164, 5164, 5309, 5310, 6246, 5310, 5168, 4917, 6280, 5953, 5310, 5168, 4917, 6280, 5322, 4917",
      /* 2822 */ "4917, 5162, 5164, 7689, 5164, 5164, 5309, 6603, 5310, 5310, 5168, 4917, 4917, 7736, 4917, 5819, 5401",
      /* 2839 */ "4917, 4917, 5813, 6162, 5164, 5164, 5164, 5167, 7583, 5310, 5310, 5310, 5310, 7774, 6362, 4917, 4917",
      /* 2856 */ "7245, 5164, 5164, 5310, 5310, 5310, 5310, 7542, 5327, 6194, 5164, 6018, 5310, 5168, 5164, 6200, 5166",
      /* 2873 */ "7399, 5310, 5163, 5164, 5165, 5310, 5310, 6493, 6499, 7567, 5164, 6220, 6220, 6224, 6235, 6236, 6227",
      /* 2890 */ "6229, 4917, 4917, 5930, 5368, 5935, 6383, 5164, 5941, 5162, 5966, 6978, 7417, 6240, 5309, 5592, 6252",
      /* 2907 */ "6265, 6269, 6313, 6275, 7109, 4917, 4917, 4917, 4917, 5171, 5405, 5407, 5400, 4917, 5409, 5813, 5164",
      /* 2924 */ "5164, 6284, 6290, 5164, 5164, 5851, 5310, 5310, 5310, 5310, 7741, 6519, 5164, 5164, 6295, 5164, 6300",
      /* 2941 */ "5842, 5310, 5164, 7726, 7713, 5164, 5310, 5166, 6016, 6016, 4917, 4917, 4990, 6525, 5310, 6306, 5310",
      /* 2958 */ "5310, 7720, 5310, 6356, 5310, 5168, 5580, 5164, 5165, 7155, 5310, 5327, 5164, 5164, 5167, 5310, 5168",
      /* 2975 */ "7794, 5310, 6310, 5419, 5866, 7126, 4917, 5819, 6318, 5164, 6609, 6808, 5164, 6323, 6328, 5164, 6332",
      /* 2992 */ "6855, 6339, 5310, 5310, 6349, 6355, 6437, 6604, 6360, 5983, 6366, 6371, 7813, 4917, 4917, 5888, 6968",
      /* 3009 */ "5164, 5164, 5164, 5164, 5164, 5164, 5851, 7266, 5164, 6376, 5164, 5164, 5164, 6380, 5164, 5164, 5164",
      /* 3026 */ "7388, 7393, 5164, 5310, 5310, 5310, 5310, 7210, 5310, 5168, 5167, 6532, 6389, 5310, 5310, 6849, 6395",
      /* 3043 */ "5310, 5168, 6313, 6280, 4917, 4917, 4917, 4917, 4917, 5175, 5181, 4917, 5168, 6231, 5161, 5164, 5164",
      /* 3060 */ "7333, 5164, 5164, 5164, 7493, 5164, 5164, 7497, 5164, 5164, 6419, 5164, 6423, 6429, 7145, 6435, 7157",
      /* 3077 */ "5170, 6668, 5164, 5164, 6441, 5165, 6446, 5310, 5168, 7292, 5164, 7435, 5310, 5310, 5163, 6451, 5310",
      /* 3094 */ "6457, 5164, 5164, 7562, 5167, 5310, 5310, 6405, 5310, 5168, 6411, 6415, 5310, 6462, 7638, 5164, 5165",
      /* 3111 */ "7040, 5310, 7405, 6474, 5310, 7759, 6479, 5937, 7533, 6488, 5328, 5164, 5165, 5310, 5310, 6034, 5166",
      /* 3128 */ "7029, 4917, 7904, 6503, 6504, 6508, 6510, 4917, 6278, 5164, 6088, 5164, 5164, 5309, 5310, 5981, 5310",
      /* 3145 */ "5169, 4917, 6524, 7784, 5904, 4917, 4917, 5162, 5164, 5164, 5164, 7801, 5309, 6614, 5310, 5169, 4917",
      /* 3162 */ "4917, 4917, 5888, 5164, 5164, 5164, 5164, 5164, 5164, 5164, 5842, 5310, 5310, 5310, 4917, 6523, 5163",
      /* 3179 */ "5164, 5164, 5164, 5164, 7206, 5310, 6541, 6546, 5166, 6016, 6016, 4917, 4917, 4917, 7444, 4917, 4917",
      /* 3196 */ "6525, 4917, 5901, 6551, 6552, 6552, 6552, 6556, 6570, 6571, 6559, 6561, 4917, 6524, 7784, 5164, 6575",
      /* 3213 */ "6580, 5165, 5310, 6586, 7200, 5310, 5310, 5310, 6692, 5170, 5162, 5164, 5164, 5166, 5310, 5310, 5574",
      /* 3230 */ "5164, 5165, 6442, 5164, 5164, 5842, 5310, 5310, 5310, 5310, 5310, 5310, 5310, 4917, 5901, 6618, 6620",
      /* 3247 */ "6620, 6620, 6622, 6626, 6627, 6631, 6633, 4917, 6524, 7784, 5904, 5285, 5164, 5309, 5845, 5310, 5310",
      /* 3264 */ "5168, 4917, 6280, 6639, 5164, 5164, 6495, 5164, 5164, 5164, 5164, 5851, 6447, 5310, 5310, 6484, 6542",
      /* 3281 */ "5842, 5988, 5310, 5310, 5310, 7660, 4917, 6644, 5819, 5164, 5164, 5164, 5164, 5164, 5310, 5310, 5310",
      /* 3298 */ "5310, 5310, 5310, 5168, 5310, 6661, 5310, 5310, 5168, 4917, 6667, 5164, 5164, 6003, 5164, 5167, 5310",
      /* 3315 */ "5310, 7021, 5164, 6672, 7328, 6677, 6683, 5310, 6048, 5310, 5310, 5310, 6774, 5168, 4917, 5161, 5164",
      /* 3332 */ "5164, 5842, 6137, 7038, 7337, 5310, 6125, 6688, 7031, 4917, 5163, 5164, 5164, 7389, 5165, 6706, 5164",
      /* 3349 */ "5164, 5166, 5310, 5310, 5163, 5164, 5165, 5310, 5310, 5328, 5164, 5165, 5310, 5310, 5164, 5166, 6722",
      /* 3366 */ "6016, 4917, 4917, 4917, 6525, 4917, 5901, 6141, 5901, 6728, 6729, 6729, 6729, 6733, 6744, 6745, 6736",
      /* 3383 */ "6738, 4917, 6524, 7784, 6749, 4917, 6755, 5309, 5317, 5310, 5310, 5168, 4917, 6280, 4917, 7731, 4917",
      /* 3400 */ "5164, 6302, 5164, 5164, 5164, 5164, 5164, 6761, 5310, 5310, 6396, 5310, 5168, 4917, 4917, 4917, 7123",
      /* 3417 */ "5819, 5164, 6772, 5310, 5310, 5310, 5310, 5310, 5169, 4917, 4917, 7367, 6790, 5310, 5310, 5310, 7653",
      /* 3434 */ "5161, 5164, 5164, 6592, 5167, 5310, 7279, 5168, 7410, 6800, 6271, 5164, 6385, 5310, 5310, 5164, 7298",
      /* 3451 */ "7702, 5164, 5166, 5310, 5164, 5310, 5166, 6016, 7035, 5166, 5310, 6806, 7607, 5310, 5164, 5310, 5166",
      /* 3468 */ "5310, 5164, 5166, 5310, 5164, 5310, 7666, 6812, 6813, 6813, 6813, 6817, 6826, 6827, 6820, 6822, 4917",
      /* 3485 */ "6524, 7784, 5904, 4917, 4917, 4917, 4917, 4917, 5162, 5164, 5338, 5310, 5310, 5168, 4917, 6280, 4917",
      /* 3502 */ "4917, 4917, 4917, 4917, 4917, 5813, 5164, 5164, 5164, 5164, 5164, 5164, 5164, 5851, 5310, 5310, 5310",
      /* 3519 */ "5310, 5310, 6848, 5168, 5838, 4917, 4917, 4917, 4917, 5813, 6076, 5164, 6081, 5819, 5164, 6768, 6780",
      /* 3536 */ "5164, 5164, 6853, 5164, 5164, 6196, 5309, 5310, 5310, 5310, 6342, 6547, 5310, 5310, 5310, 5310, 5169",
      /* 3553 */ "4942, 4917, 4917, 6067, 5975, 6871, 5164, 5164, 7116, 6869, 5164, 5167, 5310, 6875, 5310, 5310, 5310",
      /* 3570 */ "5594, 5310, 5310, 5168, 4917, 5161, 7706, 5164, 5164, 6860, 5310, 5168, 4917, 4917, 5163, 5164, 5164",
      /* 3587 */ "5164, 7204, 5164, 5167, 5310, 5310, 7221, 5164, 5164, 6880, 5310, 5310, 5310, 6784, 5162, 5164, 5164",
      /* 3604 */ "5164, 7750, 5309, 5310, 5310, 5310, 6886, 5164, 5164, 5167, 5310, 5168, 5164, 5164, 6469, 5310, 6953",
      /* 3621 */ "5586, 5164, 5166, 5310, 5310, 5163, 5164, 7717, 5310, 6016, 4917, 4917, 4917, 6525, 4917, 5901, 6891",
      /* 3638 */ "6892, 6892, 6892, 6896, 6905, 6906, 6899, 6901, 4917, 6910, 7784, 5904, 5218, 5266, 4917, 5304, 6915",
      /* 3655 */ "5162, 6921, 6103, 5164, 6163, 5786, 5310, 5310, 5310, 7120, 4917, 4917, 4917, 5888, 6046, 5310, 6115",
      /* 3672 */ "4917, 6280, 4917, 4917, 4917, 5162, 6004, 5164, 5164, 5164, 5851, 5310, 6859, 6864, 5310, 5164, 6931",
      /* 3689 */ "5310, 5310, 5310, 5310, 5847, 5310, 5310, 5310, 7211, 5161, 5164, 5164, 5164, 5167, 5310, 5310, 5310",
      /* 3706 */ "5310, 5168, 4917, 5163, 5164, 5164, 5164, 5310, 6651, 5931, 6153, 5163, 7005, 6936, 7167, 5164, 6942",
      /* 3723 */ "5164, 5310, 6948, 6957, 6694, 5310, 5310, 5310, 7552, 7447, 4917, 7450, 5164, 5164, 6582, 6167, 5164",
      /* 3740 */ "5164, 5164, 6173, 6962, 5168, 4917, 5161, 5164, 6967, 5164, 5164, 5166, 5310, 5310, 7637, 5164, 5978",
      /* 3757 */ "5164, 5167, 6802, 5310, 5310, 5310, 5168, 4917, 4917, 6022, 6026, 6987, 5170, 5162, 6992, 5164, 7696",
      /* 3774 */ "5165, 7489, 5310, 7189, 5310, 5161, 7767, 5164, 5164, 5167, 5310, 5310, 5310, 5310, 5310, 5310, 5310",
      /* 3791 */ "5169, 4917, 4917, 4917, 5888, 5164, 5164, 5164, 6998, 5310, 5168, 5164, 7003, 5165, 5310, 7009, 5327",
      /* 3808 */ "5583, 5164, 5167, 7014, 7360, 5164, 5344, 7019, 5310, 7025, 5163, 5164, 5165, 5310, 5310, 5328, 6713",
      /* 3825 */ "5165, 7618, 6717, 7045, 7045, 7049, 7058, 7059, 7052, 7054, 4917, 4917, 6513, 4916, 6524, 7784, 4922",
      /* 3842 */ "4917, 4917, 5826, 4917, 7343, 4917, 4917, 7343, 5162, 6975, 7070, 7075, 7081, 5309, 7088, 5360, 7092",
      /* 3859 */ "7097, 4917, 6280, 4917, 4917, 4917, 4917, 4917, 5190, 6917, 6916, 6927, 5851, 5310, 5310, 5310, 5310",
      /* 3876 */ "5310, 5310, 6367, 5310, 5167, 5310, 5310, 6588, 5310, 5310, 5310, 5310, 7722, 5310, 5168, 4917, 5168",
      /* 3893 */ "4917, 5999, 5164, 5164, 7137, 5164, 5164, 5310, 6865, 5310, 5310, 5310, 5310, 6372, 5162, 5164, 5164",
      /* 3910 */ "7143, 5310, 6988, 5310, 5310, 5168, 4917, 5163, 6008, 5164, 5164, 6014, 7161, 7166, 6792, 7425, 5168",
      /* 3927 */ "5164, 5164, 5166, 5310, 5310, 5163, 6796, 5165, 5166, 5310, 5164, 5310, 5166, 6882, 6016, 4917, 4917",
      /* 3944 */ "4917, 4917, 4917, 4917, 7894, 4917, 5177, 7171, 7172, 7176, 7178, 4917, 6524, 4917, 5813, 5164, 5164",
      /* 3961 */ "6286, 5164, 5164, 5164, 6324, 5164, 5164, 6837, 5164, 7182, 5164, 5164, 5164, 6657, 5842, 5310, 5310",
      /* 3978 */ "5168, 4917, 7555, 5164, 5164, 7294, 7487, 5310, 5310, 5310, 7188, 5310, 5310, 5310, 6071, 4917, 6280",
      /* 3995 */ "4917, 5373, 7810, 4917, 4917, 4917, 4917, 5819, 5164, 5164, 7694, 5164, 5164, 5164, 5164, 7273, 5164",
      /* 4012 */ "5310, 5310, 5164, 7071, 5164, 5164, 5164, 5164, 5164, 5167, 5987, 5310, 5310, 5310, 5310, 7198, 5310",
      /* 4029 */ "5310, 5310, 5310, 5168, 4917, 7111, 6778, 5164, 5904, 4917, 7215, 4917, 4917, 4917, 5162, 6077, 7430",
      /* 4046 */ "5164, 5164, 5165, 5310, 7220, 5310, 5310, 5168, 4917, 7631, 5164, 5164, 5164, 6537, 5164, 5842, 5310",
      /* 4063 */ "5310, 5310, 7540, 5310, 5310, 6525, 4917, 5901, 7225, 7226, 7226, 7226, 7230, 7239, 7240, 7233, 7235",
      /* 4080 */ "4917, 6524, 7784, 5904, 4917, 7788, 4917, 4917, 4917, 5162, 5164, 5164, 5164, 5164, 5309, 5310, 5310",
      /* 4097 */ "5310, 5168, 6131, 6169, 6215, 5310, 5310, 6255, 6258, 4917, 4917, 6524, 7784, 5904, 4917, 4917, 4917",
      /* 4114 */ "5777, 5783, 6524, 7784, 5790, 7249, 5164, 5164, 7761, 5164, 5164, 5164, 6401, 5310, 6475, 5310, 5310",
      /* 4131 */ "5310, 5310, 5310, 5310, 6684, 5169, 5164, 7272, 7133, 5164, 5164, 5167, 5310, 5310, 5168, 5164, 5164",
      /* 4148 */ "5165, 5310, 5310, 5327, 5164, 5164, 5167, 5310, 5168, 5164, 5164, 5166, 5310, 5310, 5163, 5164, 5310",
      /* 4165 */ "6599, 5310, 7277, 5310, 5168, 4917, 4917, 5163, 5164, 6400, 5164, 5164, 7394, 5310, 5310, 5310, 5310",
      /* 4182 */ "5170, 5162, 5164, 5164, 5164, 5165, 5310, 5310, 5310, 5164, 7288, 5310, 7338, 5327, 5164, 5164, 5167",
      /* 4199 */ "5310, 5310, 6124, 5310, 5168, 4917, 6209, 7303, 7304, 7304, 7304, 7308, 7317, 7318, 7311, 7313, 5230",
      /* 4216 */ "6524, 7784, 5904, 4917, 6345, 6709, 4917, 4917, 5277, 4917, 4917, 5279, 5162, 5164, 6109, 5164, 5164",
      /* 4233 */ "6724, 5310, 5310, 5310, 5310, 5168, 5321, 5161, 5164, 5164, 4917, 7370, 4917, 4917, 4917, 5796, 7322",
      /* 4250 */ "6291, 7419, 7326, 5164, 5164, 7332, 5164, 5164, 5164, 6613, 5310, 5310, 6248, 5164, 6944, 5164, 7348",
      /* 4267 */ "7162, 7353, 5310, 5310, 5310, 6391, 4917, 4917, 4917, 5888, 6655, 5164, 5164, 5164, 5310, 7359, 7662",
      /* 4284 */ "5310, 7364, 7602, 4917, 5163, 5310, 6932, 6958, 5310, 5168, 4917, 5161, 5164, 5164, 6762, 5164, 5164",
      /* 4301 */ "5851, 5310, 5310, 5310, 5310, 5310, 5310, 5310, 5310, 6351, 7633, 5164, 7349, 5164, 5167, 5310, 7398",
      /* 4318 */ "5310, 5310, 5310, 7593, 5372, 6280, 4917, 4917, 4917, 6525, 4917, 6206, 6219, 6220, 7403, 5168, 4917",
      /* 4335 */ "5163, 5164, 5164, 5164, 5165, 5310, 7517, 5165, 5310, 5310, 5310, 7409, 5161, 5164, 5164, 6922, 7526",
      /* 4352 */ "5851, 6407, 5310, 6431, 5168, 7429, 5164, 5589, 5310, 5310, 5163, 5164, 5164, 5164, 5164, 5164, 6608",
      /* 4369 */ "5310, 7434, 5310, 7573, 6887, 5164, 5165, 7439, 5310, 5310, 5310, 7612, 4917, 4917, 4917, 4917, 4917",
      /* 4386 */ "4917, 7732, 5834, 5901, 7456, 7458, 7462, 7462, 7464, 7468, 7469, 7473, 7475, 4917, 6524, 7784, 5904",
      /* 4403 */ "4917, 6465, 6261, 5310, 7484, 5310, 5168, 4917, 6280, 4917, 4917, 7342, 5894, 5164, 5164, 5164, 5164",
      /* 4420 */ "6833, 5164, 5164, 5164, 4917, 5384, 4917, 5779, 5385, 5813, 6202, 5164, 5164, 6994, 5167, 5310, 5310",
      /* 4437 */ "6844, 5310, 5310, 5310, 7700, 5310, 5169, 4917, 4917, 5270, 5275, 4916, 6524, 7784, 4922, 5164, 5842",
      /* 4454 */ "6951, 5310, 5310, 5310, 6178, 6184, 5310, 5310, 7506, 5310, 5168, 4917, 4917, 4917, 5162, 7479, 7084",
      /* 4471 */ "5164, 5164, 5877, 7512, 5164, 7516, 5164, 7521, 6673, 5164, 5164, 7139, 5164, 5164, 5164, 5310, 5310",
      /* 4488 */ "5310, 5310, 5310, 6530, 5168, 5310, 7531, 5310, 5310, 5310, 7537, 5169, 4917, 5921, 4917, 5888, 5164",
      /* 4505 */ "5164, 5164, 5164, 5164, 6926, 5164, 4917, 6314, 5888, 5164, 5164, 5164, 7546, 5164, 5164, 7548, 5167",
      /* 4522 */ "5310, 5310, 5310, 5310, 5161, 7527, 5164, 5164, 5164, 7561, 7184, 5164, 5164, 7566, 5310, 6876, 5310",
      /* 4539 */ "7571, 5310, 6983, 4917, 5161, 7262, 7577, 5168, 4917, 7557, 7452, 5164, 5164, 7624, 7587, 7592, 5310",
      /* 4556 */ "5310, 7597, 5162, 5164, 7480, 5164, 5166, 6135, 5310, 5163, 5164, 5165, 5310, 5169, 4917, 5183, 7784",
      /* 4573 */ "5904, 7606, 5310, 5310, 7611, 7616, 5161, 7622, 5164, 5167, 6182, 5310, 5310, 5310, 5310, 5310, 6649",
      /* 4590 */ "5310, 5169, 5577, 7727, 5310, 5310, 7628, 5164, 5164, 5165, 5310, 5310, 7642, 5164, 7648, 5310, 7657",
      /* 4607 */ "5164, 5167, 6188, 5310, 5310, 5310, 5168, 4917, 4917, 5481, 4917, 7670, 7671, 7671, 7671, 7675, 7684",
      /* 4624 */ "7685, 7678, 7680, 4917, 6524, 7784, 5904, 4917, 4917, 4917, 5280, 6563, 4969, 4969, 4969, 5311, 5310",
      /* 4641 */ "5310, 5168, 4917, 6280, 4917, 4917, 5162, 6027, 5164, 5164, 5164, 5309, 5355, 5310, 5310, 5168, 6839",
      /* 4658 */ "5164, 5164, 5851, 5310, 6470, 5310, 5310, 5168, 7100, 4917, 4917, 4917, 5819, 6576, 5164, 5164, 7260",
      /* 4675 */ "5167, 7711, 5310, 5168, 5164, 5164, 5165, 5310, 5310, 5310, 5310, 5170, 5162, 5164, 7440, 5328, 5164",
      /* 4692 */ "5165, 5310, 5310, 5164, 5166, 5310, 6335, 5166, 6597, 4917, 5162, 6082, 5164, 5164, 5164, 5309, 5853",
      /* 4709 */ "5310, 5955, 5310, 5310, 5310, 5310, 5310, 5310, 7744, 6177, 5310, 7756, 4917, 6280, 4917, 4917, 4917",
      /* 4726 */ "4917, 4917, 5191, 5157, 5159, 5164, 7251, 5310, 5310, 5310, 7771, 5310, 5310, 5169, 4917, 7193, 4917",
      /* 4743 */ "5888, 5164, 6938, 5164, 5164, 5164, 5164, 5964, 5851, 5970, 5310, 7780, 4917, 5161, 5164, 5164, 5164",
      /* 4760 */ "5164, 5164, 6663, 5310, 5310, 6120, 5164, 5164, 5164, 5309, 5994, 5310, 5310, 5250, 5164, 5164, 5164",
      /* 4777 */ "7149, 5310, 7798, 5164, 5164, 5164, 5164, 5164, 6425, 5310, 5310, 5310, 7746, 5310, 5310, 5310, 5310",
      /* 4794 */ "7612, 5284, 4917, 4917, 6718, 7807, 5310, 5310, 5310, 5310, 5168, 4917, 4917, 5163, 5164, 5164, 5164",
      /* 4811 */ "5164, 5164, 5164, 5310, 7819, 7833, 4917, 4917, 7830, 4917, 7832, 4917, 4917, 7789, 5892, 4916, 6524",
      /* 4828 */ "7784, 4922, 4917, 4917, 7846, 7847, 7847, 7847, 7851, 4917, 4917, 7863, 4916, 6524, 7784, 4922, 4917",
      /* 4845 */ "5002, 5557, 5625, 5496, 7874, 7874, 7874, 7875, 5604, 5616, 5538, 5538, 5480, 4917, 7883, 7884, 7884",
      /* 4862 */ "7884, 7888, 4917, 4917, 5293, 4917, 7106, 5162, 7244, 5164, 4917, 7888, 4916, 6524, 7784, 4922, 4917",
      /* 4879 */ "4917, 5773, 4917, 4917, 7373, 5813, 5164, 4979, 5830, 4969, 4969, 5122, 5025, 4959, 4964, 5096, 4975",
      /* 4896 */ "4969, 7842, 4970, 5284, 7895, 7895, 7895, 7899, 4917, 4917, 4917, 7891, 9275, 9275, 10308, 9275",
      /* 4912 */ "9275, 9275, 9275, 0, 133120, 0, 0, 0, 0, 61, 138240, 0, 0, 0, 62, 133120, 180224, 204800, 219136",
      /* 4931 */ "200704, 201728, 0, 0, 0, 109568, 207872, 209920, 211968, 0, 64, 0, 0, 453, 0, 217088, 220160, 0, 0",
      /* 4950 */ "0, 139264, 0, 234496, 0, 0, 0, 64, 200704, 201728, 139264, 203776, 139264, 139264, 207872, 139264",
      /* 4966 */ "209920, 211968, 211968, 139264, 139264, 139264, 139264, 134144, 217088, 139264, 220160, 139264",
      /* 4978 */ "139264, 234496, 139264, 139264, 139264, 176128, 233472, 0, 0, 191488, 0, 202752, 0, 0, 65, 0, 0",
      /* 4995 */ "182272, 137216, 139264, 0, 0, 0, 66, 0, 237568, 0, 235520, 0, 139264, 179200, 139264, 139264, 139264",
      /* 5012 */ "235520, 139264, 237568, 0, 0, 195584, 0, 225280, 139264, 221184, 139264, 139264, 191488, 139264",
      /* 5026 */ "139264, 139264, 200704, 139264, 195584, 139264, 139264, 196608, 206848, 215040, 222208, 139264",
      /* 5038 */ "226304, 139264, 139264, 198656, 139264, 139264, 139264, 194560, 139264, 205824, 183296, 176128",
      /* 5050 */ "139264, 139264, 224256, 224256, 0, 206848, 226304, 139264, 139264, 225280, 139264, 139264, 231424",
      /* 5063 */ "139264, 190464, 139264, 236544, 0, 139264, 139264, 189440, 139264, 139264, 199680, 205824, 139264",
      /* 5076 */ "139264, 192512, 139264, 216064, 139264, 139264, 227328, 177152, 139264, 139264, 139264, 202752",
      /* 5088 */ "139264, 139264, 208896, 139264, 218112, 139264, 139264, 182272, 139264, 139264, 139264, 217088",
      /* 5100 */ "139264, 197632, 223232, 139264, 139264, 236544, 139264, 139264, 229376, 230400, 139264, 232448",
      /* 5112 */ "139264, 139264, 185344, 139264, 139264, 190464, 139264, 139264, 188416, 139264, 139264, 139264",
      /* 5124 */ "193536, 139264, 187392, 139264, 187392, 139264, 183296, 139264, 139264, 139264, 227328, 76, 13388",
      /* 5137 */ "13388, 13388, 13388, 13388, 13388, 0, 0, 100, 119, 71, 71, 71, 71, 0, 0, 0, 71, 10308, 10308, 5264",
      /* 5157 */ "72, 72, 72, 72, 0, 0, 0, 99, 99, 99, 99, 118, 118, 118, 0, 0, 0, 137, 0, 15360, 0, 0, 112, 131",
      /* 5181 */ "15360, 15360, 0, 0, 141, 10308, 16384, 16384, 16384, 16384, 0, 0, 0, 72, 10308, 10308, 71, 5265, 60",
      /* 5200 */ "60, 10309, 17468, 60, 17468, 17468, 17468, 17468, 17468, 17468, 60, 60, 60, 60, 17468, 60, 17468",
      /* 5217 */ "17468, 0, 0, 148, 0, 10382, 10382, 71, 72, 19456, 19456, 19456, 19456, 0, 0, 0, 138, 0, 19456, 19456",
      /* 5237 */ "0, 95, 0, 95, 10308, 29696, 71, 72, 0, 20480, 0, 0, 155, 0, 0, 0, 681, 20480, 20480, 20480, 20480",
      /* 5258 */ "11381, 20480, 20480, 0, 11381, 11381, 11381, 11381, 0, 0, 156, 0, 0, 21504, 21504, 21504, 21504",
      /* 5275 */ "21504, 21504, 0, 0, 157, 0, 0, 0, 36864, 367, 0, 0, 0, 149, 22528, 22528, 22528, 22528, 0, 0, 162, 0",
      /* 5297 */ "0, 23552, 23552, 23552, 23552, 23552, 23552, 0, 0, 166, 0, 11589, 0, 118, 118, 118, 118, 235, 118",
      /* 5316 */ "553, 118, 118, 229, 118, 569, 0, 0, 0, 269, 608, 118, 118, 0, 99, 99, 685, 99, 686, 687, 697, 698",
      /* 5338 */ "118, 118, 230, 118, 99, 705, 99, 99, 99, 753, 715, 118, 118, 118, 245, 99, 735, 118, 118, 231, 118",
      /* 5359 */ "777, 118, 118, 118, 246, 24712, 24712, 24712, 24712, 0, 0, 169, 0, 133384, 0, 0, 0, 272, 0, 26722",
      /* 5379 */ "26722, 26722, 26722, 26722, 26722, 0, 0, 0, 274, 0, 0, 27648, 139264, 139264, 210944, 212992, 214016",
      /* 5396 */ "139264, 139264, 139264, 195584, 61, 0, 0, 0, 275, 137, 137, 137, 137, 0, 0, 0, 280, 368, 0, 0, 0",
      /* 5417 */ "284, 63, 0, 0, 0, 371, 0, 28672, 28672, 28672, 28672, 28734, 28672, 28672, 28734, 0, 0, 219, 118, 0",
      /* 5437 */ "28672, 0, 18432, 10383, 71, 72, 133120, 30720, 0, 0, 219, 139264, 217, 139482, 139482, 139482",
      /* 5453 */ "134144, 200875, 201899, 139435, 203947, 212139, 139435, 139435, 139435, 176346, 217259, 139435",
      /* 5465 */ "220331, 139435, 139482, 139435, 139482, 181466, 139482, 234667, 139435, 139435, 139435, 193707",
      /* 5477 */ "201946, 139482, 203994, 139482, 0, 0, 0, 376, 208090, 139482, 210138, 212186, 139482, 220378, 139482",
      /* 5492 */ "139482, 139482, 227546, 188587, 139435, 139435, 139435, 194731, 198827, 139435, 139435, 139435",
      /* 5504 */ "195755, 214187, 139435, 139435, 139435, 202923, 139435, 211162, 213210, 214234, 139482, 139482",
      /* 5516 */ "198874, 139482, 139482, 194778, 139482, 139482, 196826, 207066, 0, 182272, 137216, 139435, 139482",
      /* 5529 */ "224427, 224474, 191659, 139435, 139435, 139435, 211115, 213163, 182490, 139482, 139482, 139482",
      /* 5541 */ "139482, 200922, 202970, 139482, 139482, 209114, 139482, 218330, 139482, 139482, 139482, 228570",
      /* 5553 */ "229594, 230618, 139482, 232666, 0, 139435, 179371, 139435, 178347, 139435, 139435, 182443, 139435",
      /* 5566 */ "184538, 139482, 139482, 139482, 217306, 235738, 139482, 237786, 0, 99, 768, 99, 99, 689, 99, 99, 706",
      /* 5583 */ "99, 99, 728, 99, 99, 748, 99, 99, 756, 118, 226, 118, 118, 354, 118, 225451, 139435, 139435, 231595",
      /* 5602 */ "139482, 195802, 139482, 139482, 185562, 139482, 139482, 231642, 139482, 190464, 185515, 139435",
      /* 5614 */ "139435, 190635, 139482, 190682, 139482, 139482, 188634, 139482, 205824, 183296, 176299, 139435",
      /* 5626 */ "184491, 139435, 139435, 229547, 230571, 183467, 139435, 139435, 139435, 186539, 139435, 139435",
      /* 5638 */ "197803, 223403, 139435, 199851, 205995, 139435, 208043, 139435, 210091, 0, 206848, 226304, 139435",
      /* 5651 */ "209067, 139435, 218283, 181419, 139435, 139435, 139435, 221355, 139435, 139435, 236715, 139482",
      /* 5663 */ "196779, 207019, 215211, 222379, 215258, 222426, 139482, 226522, 186586, 139482, 139482, 139482",
      /* 5675 */ "221402, 139482, 236762, 0, 139435, 139435, 189611, 139482, 189658, 139482, 139482, 193754, 139482",
      /* 5688 */ "139482, 228352, 139435, 139435, 226475, 139435, 139435, 227499, 177370, 139482, 139482, 225498",
      /* 5700 */ "139482, 139482, 199898, 206042, 192683, 139435, 216235, 139435, 232619, 139435, 139435, 228523",
      /* 5712 */ "139482, 139482, 192730, 139482, 216282, 177323, 139435, 139435, 139435, 235691, 139435, 237739",
      /* 5724 */ "139482, 197850, 223450, 139482, 179418, 139482, 139482, 139482, 234714, 187563, 139435, 187610",
      /* 5736 */ "139482, 183514, 139482, 139482, 191706, 139482, 0, 32845, 32845, 32845, 32845, 32845, 32845, 0, 0",
      /* 5751 */ "219, 139482, 0, 32845, 0, 0, 268, 0, 33792, 0, 0, 0, 380, 0, 35840, 35840, 35840, 35840, 35936",
      /* 5770 */ "35936, 35936, 35936, 0, 0, 273, 0, 0, 35936, 0, 0, 276, 277, 133120, 0, 31744, 0, 118, 118, 220",
      /* 5790 */ "138240, 0, 0, 14336, 34816, 38912, 0, 0, 279, 0, 0, 37966, 37966, 37966, 37966, 37966, 37966, 0, 0",
      /* 5809 */ "283, 0, 0, 37966, 0, 0, 283, 172, 0, 39936, 0, 0, 283, 383, 40960, 40960, 40960, 40960, 0, 40960",
      /* 5829 */ "40960, 0, 0, 326, 139264, 41984, 41984, 41984, 41984, 0, 0, 370, 0, 11589, 283, 219, 118, 227, 118",
      /* 5848 */ "118, 506, 118, 11589, 383, 118, 118, 232, 118, 100, 100, 100, 100, 119, 119, 119, 119, 119, 0, 0",
      /* 5868 */ "372, 373, 0, 43008, 43008, 43008, 43008, 43008, 43008, 0, 0, 379, 0, 0, 44032, 44032, 44032, 44032",
      /* 5886 */ "44032, 44032, 0, 0, 383, 99, 0, 44032, 0, 0, 383, 462, 73, 64, 0, 73, 0, 0, 73, 0, 0, 0, 172, 73, 79",
      /* 5911 */ "79, 79, 79, 79, 79, 101, 120, 120, 120, 120, 0, 0, 455, 456, 101, 101, 101, 101, 120, 164, 0, 0, 0",
      /* 5934 */ "520, 170, 0, 99, 99, 99, 776, 198, 200, 99, 204, 223, 118, 118, 118, 264, 247, 118, 251, 118, 238",
      /* 5955 */ "118, 118, 335, 118, 254, 118, 118, 0, 725, 99, 415, 99, 99, 179, 99, 419, 118, 118, 118, 265, 461",
      /* 5976 */ "461, 383, 99, 99, 775, 118, 239, 118, 118, 444, 118, 492, 118, 118, 118, 332, 118, 503, 118, 118",
      /* 5996 */ "233, 118, 517, 0, 0, 0, 574, 586, 99, 99, 99, 182, 99, 618, 99, 99, 180, 99, 99, 629, 99, 118, 99",
      /* 6019 */ "118, 118, 737, 523, 99, 99, 525, 526, 99, 99, 99, 183, 750, 99, 752, 99, 99, 799, 800, 754, 99, 118",
      /* 6041 */ "118, 491, 762, 118, 764, 118, 240, 118, 118, 600, 118, 97, 97, 97, 97, 102, 121, 102, 102, 102, 102",
      /* 6062 */ "121, 121, 121, 121, 121, 0, 0, 459, 0, 255, 118, 118, 0, 135168, 285, 99, 99, 99, 184, 294, 99, 99",
      /* 6084 */ "99, 185, 99, 299, 99, 99, 192, 99, 11589, 283, 219, 327, 341, 118, 118, 118, 336, 99, 398, 99, 99",
      /* 6105 */ "193, 99, 406, 407, 99, 99, 194, 99, 118, 424, 118, 118, 263, 0, 485, 99, 99, 99, 186, 602, 118, 118",
      /* 6127 */ "118, 346, 624, 625, 99, 99, 203, 205, 118, 759, 118, 118, 331, 118, 73, 80, 80, 80, 80, 80, 80, 103",
      /* 6149 */ "122, 122, 122, 122, 0, 0, 522, 0, 103, 103, 103, 103, 122, 286, 99, 99, 99, 216, 99, 311, 99, 99",
      /* 6171 */ "214, 99, 11589, 283, 219, 328, 353, 118, 118, 118, 347, 118, 493, 118, 118, 349, 118, 118, 596, 118",
      /* 6191 */ "118, 352, 118, 99, 727, 99, 99, 215, 99, 99, 751, 99, 99, 288, 99, 73, 0, 74, 73, 0, 75, 73, 0, 147",
      /* 6215 */ "0, 118, 118, 221, 73, 81, 81, 81, 81, 81, 81, 104, 123, 123, 123, 123, 0, 0, 570, 0, 104, 104, 104",
      /* 6238 */ "104, 123, 206, 209, 212, 99, 99, 803, 118, 241, 118, 118, 702, 0, 236, 118, 243, 118, 250, 252, 118",
      /* 6259 */ "261, 118, 0, 118, 118, 222, 248, 118, 118, 253, 256, 259, 118, 0, 785, 99, 266, 10308, 10308, 0, 140",
      /* 6280 */ "0, 10308, 10308, 0, 99, 295, 99, 99, 297, 99, 298, 99, 99, 99, 289, 313, 99, 99, 99, 293, 99, 322",
      /* 6302 */ "99, 99, 300, 99, 337, 118, 118, 340, 364, 118, 118, 265, 0, 0, 0, 460, 384, 99, 99, 99, 302, 401, 99",
      /* 6325 */ "99, 99, 310, 99, 403, 404, 405, 410, 99, 412, 99, 99, 810, 811, 11589, 383, 418, 118, 262, 118, 0",
      /* 6346 */ "153, 0, 158, 118, 427, 118, 118, 363, 118, 430, 118, 118, 118, 355, 438, 439, 118, 118, 366, 0, 446",
      /* 6367 */ "118, 118, 118, 356, 450, 118, 0, 0, 651, 467, 468, 99, 470, 99, 480, 481, 99, 176, 99, 99, 99, 791",
      /* 6389 */ "118, 497, 118, 118, 368, 368, 508, 118, 118, 118, 360, 529, 99, 99, 99, 312, 118, 603, 118, 118, 421",
      /* 6410 */ "118, 610, 0, 612, 613, 614, 99, 99, 617, 620, 99, 99, 623, 628, 630, 99, 118, 490, 118, 118, 634",
      /* 6431 */ "118, 118, 428, 118, 118, 640, 118, 118, 435, 118, 659, 99, 99, 99, 317, 667, 118, 118, 118, 422, 118",
      /* 6452 */ "673, 118, 118, 440, 441, 680, 0, 0, 99, 118, 118, 701, 118, 0, 154, 0, 159, 734, 118, 118, 118, 425",
      /* 6474 */ "758, 118, 118, 118, 429, 769, 99, 771, 99, 177, 99, 99, 320, 99, 780, 118, 118, 118, 432, 99, 798",
      /* 6495 */ "99, 99, 303, 99, 99, 802, 118, 804, 105, 105, 105, 105, 124, 124, 124, 124, 124, 0, 0, 22528, 0, 0",
      /* 6517 */ "99, 411, 99, 99, 306, 99, 521, 0, 0, 0, 10308, 0, 0, 118, 564, 118, 118, 494, 495, 797, 99, 99, 99",
      /* 6540 */ "321, 817, 99, 99, 99, 324, 819, 118, 118, 118, 436, 73, 82, 82, 82, 82, 82, 82, 106, 125, 125, 125",
      /* 6562 */ "125, 0, 0, 139264, 139264, 139264, 0, 135168, 106, 106, 106, 106, 125, 657, 99, 99, 99, 386, 99, 660",
      /* 6582 */ "99, 99, 309, 99, 118, 671, 118, 118, 501, 118, 688, 99, 99, 99, 390, 814, 815, 118, 118, 504, 118",
      /* 6603 */ "224, 118, 118, 118, 437, 544, 99, 99, 99, 393, 691, 118, 118, 118, 445, 73, 83, 93, 93, 93, 93, 107",
      /* 6625 */ "126, 107, 107, 107, 107, 126, 126, 126, 126, 126, 0, 0, 139264, 139436, 290, 99, 99, 99, 397, 377, 0",
      /* 6646 */ "0, 0, 10382, 118, 443, 118, 118, 515, 516, 99, 464, 99, 99, 323, 99, 118, 556, 118, 118, 547, 548",
      /* 6667 */ "571, 0, 0, 99, 653, 582, 99, 99, 99, 400, 99, 589, 99, 99, 385, 99, 591, 118, 118, 118, 449, 605",
      /* 6689 */ "118, 118, 607, 118, 644, 118, 118, 557, 118, 99, 731, 732, 733, 118, 739, 118, 741, 742, 743, 118, 0",
      /* 6710 */ "161, 0, 163, 99, 787, 99, 788, 794, 118, 118, 118, 498, 823, 824, 99, 118, 593, 118, 73, 84, 84, 84",
      /* 6732 */ "84, 84, 84, 108, 127, 127, 127, 127, 0, 0, 139482, 178394, 108, 108, 108, 108, 127, 73, 146, 0, 0",
      /* 6753 */ "193536, 0, 150, 0, 0, 0, 10383, 0, 318, 99, 99, 99, 409, 99, 388, 99, 99, 389, 99, 118, 431, 118",
      /* 6775 */ "118, 565, 118, 99, 576, 99, 99, 392, 99, 118, 649, 0, 0, 214016, 0, 99, 664, 99, 118, 736, 118, 99",
      /* 6797 */ "770, 99, 772, 118, 781, 118, 118, 597, 118, 99, 809, 99, 99, 396, 99, 73, 85, 85, 85, 85, 85, 85",
      /* 6819 */ "109, 128, 128, 128, 128, 0, 0, 109, 109, 109, 109, 128, 99, 291, 99, 99, 399, 99, 99, 319, 99, 99",
      /* 6841 */ "408, 99, 333, 118, 118, 118, 502, 361, 118, 118, 118, 507, 99, 402, 99, 99, 416, 99, 423, 118, 118",
      /* 6862 */ "118, 511, 426, 118, 118, 118, 551, 99, 484, 99, 99, 465, 99, 496, 118, 118, 118, 554, 99, 713, 99",
      /* 6883 */ "118, 825, 826, 723, 118, 0, 99, 786, 73, 86, 86, 86, 86, 86, 86, 110, 129, 129, 129, 129, 0, 0, 110",
      /* 6906 */ "110, 110, 110, 129, 139, 0, 0, 10308, 15360, 167, 0, 0, 0, 16384, 0, 173, 99, 99, 99, 413, 479, 99",
      /* 6928 */ "99, 99, 417, 488, 118, 118, 118, 558, 99, 530, 99, 99, 469, 99, 99, 541, 99, 99, 478, 99, 118, 549",
      /* 6950 */ "550, 118, 330, 118, 118, 744, 0, 552, 118, 118, 118, 562, 563, 118, 118, 118, 566, 579, 99, 99, 99",
      /* 6971 */ "466, 99, 621, 622, 99, 178, 181, 99, 189, 99, 196, 639, 118, 118, 118, 568, 643, 118, 118, 118, 601",
      /* 6992 */ "99, 654, 99, 99, 486, 99, 694, 118, 118, 118, 635, 99, 709, 99, 99, 527, 528, 719, 118, 118, 118",
      /* 7013 */ "638, 738, 118, 118, 118, 646, 99, 755, 118, 118, 604, 118, 118, 763, 118, 765, 805, 806, 118, 118",
      /* 7033 */ "609, 0, 827, 828, 99, 118, 334, 118, 118, 717, 118, 73, 87, 87, 87, 87, 87, 87, 111, 130, 130, 130",
      /* 7055 */ "130, 0, 0, 111, 111, 111, 111, 130, 160, 0, 0, 0, 19456, 0, 0, 187, 99, 99, 99, 473, 199, 202, 99",
      /* 7078 */ "99, 531, 99, 99, 210, 213, 99, 190, 195, 197, 225, 228, 118, 234, 249, 118, 118, 118, 669, 257, 260",
      /* 7099 */ "118, 0, 369, 0, 0, 165, 0, 0, 168, 0, 0, 267, 0, 0, 573, 99, 314, 99, 99, 99, 476, 118, 451, 0, 0",
      /* 7124 */ "378, 0, 0, 375, 0, 0, 652, 99, 474, 99, 99, 99, 482, 99, 583, 99, 99, 534, 99, 99, 592, 118, 118",
      /* 7147 */ "637, 118, 99, 692, 118, 118, 641, 642, 118, 716, 118, 118, 645, 647, 726, 99, 99, 99, 487, 730, 99",
      /* 7168 */ "99, 99, 535, 112, 112, 112, 112, 131, 131, 131, 131, 131, 0, 0, 99, 308, 99, 99, 538, 99, 350, 118",
      /* 7190 */ "118, 118, 675, 454, 0, 0, 0, 20480, 118, 500, 118, 118, 674, 118, 99, 537, 99, 99, 542, 99, 559, 118",
      /* 7212 */ "118, 118, 679, 151, 0, 0, 0, 24712, 670, 118, 118, 118, 703, 73, 88, 88, 88, 88, 88, 88, 113, 132",
      /* 7234 */ "132, 132, 132, 0, 0, 113, 113, 113, 113, 132, 174, 99, 99, 99, 539, 99, 287, 99, 99, 545, 99, 329",
      /* 7256 */ "118, 118, 118, 718, 99, 395, 99, 99, 577, 99, 118, 420, 118, 118, 696, 118, 477, 99, 99, 99, 543",
      /* 7277 */ "118, 509, 118, 118, 699, 118, 704, 99, 99, 99, 578, 712, 99, 99, 714, 99, 747, 99, 99, 584, 99, 801",
      /* 7299 */ "99, 118, 118, 594, 73, 89, 89, 89, 89, 89, 89, 114, 133, 133, 133, 133, 0, 0, 114, 114, 114, 114",
      /* 7321 */ "133, 281, 282, 283, 172, 99, 296, 99, 99, 587, 99, 304, 99, 99, 99, 585, 338, 118, 118, 118, 722",
      /* 7342 */ "457, 0, 0, 0, 40960, 0, 483, 99, 99, 99, 588, 99, 489, 118, 118, 721, 118, 505, 118, 118, 118, 745",
      /* 7364 */ "118, 514, 118, 0, 458, 0, 0, 271, 0, 0, 278, 0, 0, 139264, 178176, 139264, 139264, 181248, 139264",
      /* 7383 */ "139264, 139264, 228352, 139264, 139264, 536, 99, 99, 99, 627, 540, 99, 99, 99, 632, 599, 118, 118",
      /* 7401 */ "118, 761, 118, 606, 118, 118, 724, 99, 676, 118, 118, 118, 779, 708, 99, 710, 99, 201, 99, 99, 292",
      /* 7422 */ "99, 118, 720, 118, 118, 740, 118, 746, 99, 99, 99, 656, 773, 99, 99, 118, 757, 792, 118, 118, 118",
      /* 7443 */ "783, 67, 0, 10308, 0, 518, 0, 0, 524, 99, 99, 619, 99, 73, 90, 90, 94, 90, 94, 94, 94, 94, 94, 115",
      /* 7467 */ "134, 115, 115, 115, 115, 134, 134, 134, 134, 134, 0, 0, 175, 99, 99, 99, 658, 237, 242, 244, 118",
      /* 7488 */ "339, 118, 118, 668, 118, 305, 99, 99, 307, 99, 315, 316, 99, 207, 99, 99, 99, 818, 357, 358, 118",
      /* 7509 */ "118, 760, 118, 381, 382, 283, 383, 387, 99, 99, 99, 662, 394, 99, 99, 99, 666, 414, 99, 99, 99, 684",
      /* 7531 */ "118, 434, 118, 118, 778, 118, 118, 447, 448, 118, 342, 118, 118, 561, 118, 99, 475, 99, 99, 590, 99",
      /* 7552 */ "513, 118, 118, 0, 572, 0, 99, 616, 99, 532, 99, 99, 99, 690, 546, 118, 118, 118, 808, 118, 560, 118",
      /* 7574 */ "118, 782, 118, 99, 580, 99, 99, 626, 99, 595, 118, 118, 598, 633, 118, 118, 118, 820, 636, 118, 118",
      /* 7595 */ "118, 25600, 648, 0, 650, 0, 611, 0, 0, 519, 0, 663, 99, 99, 118, 813, 672, 118, 118, 118, 134144",
      /* 7616 */ "118, 677, 118, 118, 793, 118, 99, 682, 99, 99, 631, 118, 700, 118, 118, 0, 615, 99, 99, 581, 99, 766",
      /* 7638 */ "99, 99, 99, 707, 784, 0, 99, 99, 655, 99, 789, 790, 99, 118, 343, 118, 118, 678, 118, 118, 795, 796",
      /* 7660 */ "118, 345, 118, 118, 510, 118, 99, 821, 118, 822, 73, 91, 91, 91, 91, 91, 91, 116, 135, 135, 135, 135",
      /* 7682 */ "0, 0, 116, 116, 116, 116, 135, 188, 99, 99, 99, 711, 99, 391, 99, 99, 661, 99, 442, 118, 118, 118",
      /* 7704 */ "807, 118, 575, 99, 99, 99, 729, 118, 695, 118, 118, 816, 118, 99, 774, 99, 118, 348, 118, 118, 359",
      /* 7725 */ "118, 812, 99, 118, 118, 693, 270, 0, 0, 0, 41984, 374, 0, 0, 0, 43008, 433, 118, 118, 118, 351, 118",
      /* 7747 */ "118, 344, 118, 99, 211, 99, 99, 665, 118, 258, 118, 118, 0, 767, 99, 99, 301, 99, 99, 533, 99, 99",
      /* 7769 */ "683, 99, 555, 118, 118, 118, 362, 118, 118, 512, 118, 118, 567, 118, 0, 10308, 10308, 71, 72, 152, 0",
      /* 7790 */ "0, 0, 44032, 463, 99, 99, 99, 749, 99, 471, 472, 99, 208, 99, 99, 191, 99, 499, 118, 118, 118, 365",
      /* 7812 */ "118, 0, 452, 0, 0, 10310, 0, 0, 107520, 10308, 0, 10382, 10382, 0, 76, 76, 76, 76, 107520, 0, 107520",
      /* 7833 */ "0, 0, 0, 107520, 0, 139264, 139483, 139264, 186368, 139264, 139264, 139264, 234496, 0, 108636",
      /* 7848 */ "108636, 108636, 108636, 108636, 108636, 0, 0, 11264, 219, 139264, 184320, 139264, 139264, 139264",
      /* 7862 */ "221184, 0, 108636, 0, 0, 11264, 139264, 178176, 188416, 0, 0, 0, 139435, 139435, 139435, 139435",
      /* 7878 */ "139482, 139482, 139482, 0, 135168, 0, 110592, 110592, 110592, 110592, 110592, 110592, 0, 0, 12288, 0",
      /* 7894 */ "0, 12288, 12288, 12288, 12288, 12288, 12288, 0, 0, 13388, 0, 0, 105, 124"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 7908; ++i) {TRANSITION[i] = Integer.parseInt(s2[i]);}
  }

  private static final int[] EXPECTED = new int[1285];
  static
  {
    final String s1[] =
    {
      /*    0 */ "414, 418, 422, 426, 430, 434, 438, 442, 446, 450, 471, 471, 456, 828, 743, 480, 462, 466, 470, 471",
      /*   20 */ "471, 741, 828, 828, 828, 828, 828, 478, 480, 480, 480, 480, 481, 964, 471, 741, 828, 828, 828, 828",
      /*   40 */ "458, 480, 480, 480, 480, 832, 471, 714, 828, 828, 828, 828, 501, 480, 480, 480, 485, 825, 828, 828",
      /*   60 */ "828, 480, 480, 480, 485, 826, 828, 828, 501, 480, 490, 825, 828, 498, 480, 480, 486, 828, 498, 480",
      /*   80 */ "480, 826, 828, 501, 490, 828, 500, 502, 828, 501, 492, 829, 480, 827, 500, 491, 498, 480, 829, 496",
      /*  100 */ "831, 830, 506, 508, 976, 579, 515, 519, 522, 525, 529, 977, 778, 538, 471, 550, 557, 471, 856, 471",
      /*  120 */ "976, 916, 565, 471, 541, 571, 575, 471, 471, 471, 692, 602, 583, 471, 471, 471, 643, 592, 541, 601",
      /*  140 */ "885, 471, 471, 471, 600, 884, 585, 471, 471, 866, 713, 606, 610, 578, 471, 553, 609, 577, 471, 777",
      /*  160 */ "776, 881, 614, 471, 945, 623, 578, 777, 878, 632, 578, 560, 633, 471, 779, 631, 642, 637, 641, 471",
      /*  180 */ "647, 578, 561, 642, 843, 651, 844, 652, 951, 843, 656, 671, 670, 657, 664, 663, 670, 669, 668, 675",
      /*  200 */ "683, 544, 545, 546, 594, 596, 471, 471, 690, 658, 696, 697, 701, 702, 706, 691, 712, 718, 722, 471",
      /*  220 */ "729, 733, 725, 738, 690, 712, 747, 751, 471, 471, 686, 755, 759, 578, 471, 471, 763, 767, 771, 471",
      /*  240 */ "775, 783, 471, 471, 474, 792, 797, 471, 471, 473, 791, 796, 471, 801, 785, 471, 708, 806, 811, 471",
      /*  260 */ "707, 805, 810, 659, 785, 471, 815, 819, 471, 957, 836, 659, 842, 957, 836, 471, 958, 837, 841, 627",
      /*  280 */ "849, 472, 848, 853, 957, 871, 627, 860, 864, 870, 734, 875, 532, 626, 893, 891, 890, 898, 534, 533",
      /*  300 */ "890, 889, 897, 892, 533, 902, 903, 904, 908, 912, 914, 471, 920, 939, 924, 928, 931, 933, 965, 471",
      /*  320 */ "471, 787, 937, 471, 567, 471, 471, 943, 964, 471, 471, 949, 471, 471, 471, 471, 473, 955, 471, 471",
      /*  340 */ "471, 471, 452, 619, 471, 823, 471, 471, 471, 471, 511, 471, 471, 471, 471, 510, 962, 821, 471, 471",
      /*  360 */ "471, 472, 970, 471, 471, 471, 969, 821, 471, 471, 471, 974, 471, 471, 617, 471, 471, 471, 678, 471",
      /*  380 */ "471, 679, 471, 471, 588, 471, 587, 471, 471, 588, 471, 981, 471, 587, 471, 588, 586, 471, 982, 471",
      /*  400 */ "471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 986, 988, 990, 992, 994, 995",
      /*  420 */ "995, 997, 999, 1001, 1003, 1005, 1007, 1008, 1008, 1012, 1009, 1015, 1010, 1014, 1017, 1019, 1021",
      /*  437 */ "1023, 1025, 1027, 1029, 1032, 1030, 1115, 1034, 1131, 1038, 1253, 1074, 1063, 1047, 1173, 1038, 1038",
      /*  454 */ "1040, 1050, 1076, 1185, 1184, 1184, 1087, 1089, 1089, 1089, 1089, 1090, 1084, 1038, 1253, 1134, 1051",
      /*  471 */ "1038, 1038, 1038, 1038, 1039, 1041, 1070, 1180, 1088, 1089, 1089, 1089, 1089, 1082, 1089, 1181, 1038",
      /*  488 */ "1038, 1184, 1089, 1089, 1089, 1181, 1184, 1184, 1089, 1089, 1184, 1184, 1184, 1283, 1089, 1089, 1089",
      /*  505 */ "1085, 1184, 1089, 1283, 1283, 1038, 1038, 1040, 1211, 1183, 1218, 1113, 1094, 1095, 1097, 1099, 1101",
      /*  522 */ "1102, 1103, 1102, 1105, 1105, 1105, 1106, 1107, 1108, 1110, 1038, 1038, 1226, 1250, 1035, 1038, 1117",
      /*  539 */ "1247, 1119, 1038, 1038, 1227, 1038, 1038, 1240, 1038, 1038, 1240, 1038, 1260, 1264, 1038, 1038, 1245",
      /*  556 */ "1194, 1125, 1127, 1037, 1038, 1038, 1245, 1259, 1154, 1199, 1130, 1038, 1038, 1048, 1282, 1193, 1195",
      /*  573 */ "1229, 1198, 1242, 1143, 1035, 1037, 1038, 1038, 1038, 1092, 1243, 1224, 1036, 1038, 1038, 1038, 1047",
      /*  590 */ "1038, 1038, 1247, 1234, 1038, 1038, 1059, 1038, 1059, 1059, 1038, 1246, 1194, 1228, 1197, 1248, 1227",
      /*  607 */ "1245, 1194, 1228, 1197, 1249, 1078, 1035, 1249, 1156, 1036, 1038, 1044, 1046, 1066, 1060, 1038, 1229",
      /*  624 */ "1154, 1268, 1035, 1038, 1038, 1039, 1213, 1147, 1259, 1197, 1249, 1269, 1037, 1038, 1245, 1259, 1197",
      /*  641 */ "1249, 1079, 1038, 1038, 1038, 1113, 1146, 1150, 1153, 1155, 1158, 1155, 1037, 1038, 1038, 1154, 1244",
      /*  658 */ "1038, 1038, 1038, 1131, 1236, 1038, 1240, 1155, 1038, 1038, 1241, 1244, 1038, 1038, 1239, 1241, 1244",
      /*  675 */ "1038, 1240, 1244, 1038, 1044, 1047, 1038, 1038, 1038, 1239, 1160, 1038, 1049, 1062, 1046, 1038, 1162",
      /*  692 */ "1038, 1038, 1038, 1192, 1164, 1166, 1166, 1166, 1166, 1166, 1168, 1168, 1168, 1168, 1168, 1038, 1038",
      /*  709 */ "1038, 1210, 1070, 1038, 1131, 1038, 1038, 1038, 1065, 1072, 1170, 1151, 1160, 1252, 1038, 1172, 1038",
      /*  726 */ "1052, 1054, 1056, 1053, 1055, 1057, 1141, 1237, 1038, 1038, 1038, 1222, 1058, 1142, 1238, 1038, 1069",
      /*  743 */ "1184, 1184, 1180, 1081, 1062, 1133, 1176, 1194, 1140, 1143, 1038, 1191, 1179, 1255, 1257, 1195, 1229",
      /*  760 */ "1231, 1208, 1187, 1048, 1042, 1064, 1190, 1202, 1256, 1258, 1228, 1230, 1207, 1143, 1188, 1038, 1136",
      /*  777 */ "1038, 1038, 1038, 1112, 1038, 1038, 1133, 1204, 1252, 1071, 1038, 1038, 1067, 1086, 1070, 1254, 1174",
      /*  794 */ "1195, 1206, 1206, 1233, 1143, 1188, 1038, 1038, 1131, 1132, 1236, 1070, 1254, 1196, 1232, 1078, 1078",
      /*  811 */ "1187, 1037, 1038, 1038, 1039, 1213, 1263, 1232, 1078, 1187, 1038, 1038, 1085, 1132, 1038, 1038, 1085",
      /*  828 */ "1184, 1184, 1184, 1184, 1089, 1089, 1089, 1061, 1215, 1233, 1143, 1217, 1038, 1235, 1144, 1038, 1038",
      /*  845 */ "1038, 1239, 1158, 1213, 1263, 1233, 1187, 1038, 1136, 1223, 1071, 1038, 1124, 1126, 1128, 1263, 1233",
      /*  862 */ "1224, 1131, 1235, 1252, 1038, 1038, 1112, 1076, 1222, 1220, 1251, 1035, 1038, 1220, 1251, 1035, 1136",
      /*  879 */ "1038, 1038, 1147, 1194, 1139, 1197, 1248, 1250, 1156, 1036, 1200, 1131, 1038, 1039, 1267, 1200, 1131",
      /*  896 */ "1131, 1267, 1200, 1137, 1038, 1038, 1038, 1271, 1200, 1038, 1271, 1200, 1039, 1250, 1039, 1250, 1076",
      /*  913 */ "1076, 1077, 1077, 1038, 1038, 1112, 1173, 1261, 1038, 1038, 1114, 1273, 1273, 1122, 1122, 1275, 1122",
      /*  930 */ "1122, 1277, 1277, 1277, 1277, 1279, 1038, 1265, 1038, 1038, 1038, 1120, 1121, 1281, 1066, 1038, 1038",
      /*  947 */ "1148, 1259, 1068, 1132, 1038, 1038, 1177, 1155, 1064, 1183, 1038, 1038, 1210, 1262, 1215, 1183, 1038",
      /*  964 */ "1043, 1038, 1038, 1038, 1043, 1039, 1041, 1046, 1066, 1038, 1045, 1183, 1038, 1038, 1218, 1038, 1038",
      /*  981 */ "1038, 1182, 1038, 1038, 1038, 131330, 147712, 164096, 196864, 393472, 655616, 2228480, 537002240",
      /*  994 */ "1073873152, 131328, 131328, 268567040, 213248, 426240, 2490624, 393472, 131328, 393552, 2490624",
      /* 1005 */ "393472, 721218, 3080514, -2004997888, -2004997888, -1904330496, -1904248576, -2004932352",
      /* 1013 */ "-2004997888, -1367394048, -1904264960, -1367459584, -1904215808, -1367377664, 131488, 132512, 197024",
      /* 1022 */ "655776, 229792, 459168, 721312, 917920, 983456, 25396670, 126059966, 131302846, -1980252738",
      /* 1032 */ "131564990, 131302846, 2, 536870912, 1073741824, -2147483648, 0, 0, 1, 2, 4, 8, 0, 2, 32, 64, 0, 3, 4",
      /* 1051 */ "16, 0, 7, 24, 3936, 28672, 98304, 1966080, 4194304, 0, 8, 8, 16, 16, 32, 128, 0, 16, 128, 32, 256, 0",
      /* 1073 */ "24, 131584, 268435968, 0, 67108864, 67108864, 134217728, -2147483648, 1184, 1152, 12, 14, 0, 128",
      /* 1087 */ "1024, 1056, 1152, 1152, 16777228, 128, 32768, 0, 1417684087, 1417684087, 1417684215, 1417946231",
      /* 1099 */ "1417684215, 1417946359, 1418077311, 1418077823, 1418077823, 1418077951, -34816, -34816, -33920",
      /* 1108 */ "-33920, -32896, -33920, -32769, 0, 33554432, 0, 256, 131072, 3, 96, 1409286144, 0, 3728, 3760, 3760",
      /* 1124 */ "0, 96256, 1966080, 31457280, 2113929216, -2147483648, 268435456, 1073741824, 0, 32, 512, 16, 0",
      /* 1137 */ "1073741824, 1073741824, 262144, 524288, 4194304, 125829120, 134217728, 268435456, 256, 0, 262144",
      /* 1148 */ "6144, 16384, 6144, 65536, 524288, 262144, 6291456, 16777216, 134217728, 536870912, 65536, 6291456",
      /* 1160 */ "4194304, 134217728, 128, 2097152, 0, 407459384, 407459640, 407459640, -2097281, -2097281, 1568",
      /* 1171 */ "20480, 1824, 0, 8192, 32768, 1024, 4096, 6291456, 768, 1024, 1152, 0, 64, 128, 128, 160, 268435456",
      /* 1188 */ "1610612736, -2147483648, 64, 768, 0, 14336, 16384, 65536, 393216, 1048576, 6291456, 8388608",
      /* 1200 */ "67108864, 536870912, 1024, 2048, 65536, 134217728, 1048576, 8388608, 117440512, 134217728, 1, 4, 32",
      /* 1213 */ "4, 256, 393216, 8388608, 1610612736, 0, 32768, 393216, 50331648, 1, 512, 268435456, 536870912, 1",
      /* 1227 */ "262144, 393216, 524288, 1048576, 4194304, 8388608, 50331648, 67108864, 1073741824, 512, 134217728",
      /* 1238 */ "-268435456, 0, 4096, 4194304, 16777216, 100663296, 134217728, 0, 6144, 8192, 8388608, 16777216",
      /* 1250 */ "33554432, 67108864, 268435456, 0, 512, 2048, 12288, 16384, 32768, 65536, 262144, 8, 256, 512, 393216",
      /* 1265 */ "0, 48, 262144, 33554432, 134217728, 1073741824, 1, 33554432, 3984, 3760, 4016, 3760, 247, 247, 2295",
      /* 1280 */ "3831, 3, 116, 128, 1152"
    };
    String[] s2 = java.util.Arrays.toString(s1).replaceAll("[ \\[\\]]", "").split(",");
    for (int i = 0; i < 1285; ++i) {EXPECTED[i] = Integer.parseInt(s2[i]);}
  }

  private static final String[] TOKEN =
  {
    "(0)",
    "IntegerLiteral",
    "DecimalLiteral",
    "DoubleLiteral",
    "StringLiteral",
    "URIQualifiedName",
    "NCName",
    "QName",
    "S",
    "CommentContents",
    "Wildcard",
    "EOF",
    "'!'",
    "'!='",
    "'#'",
    "'$'",
    "'('",
    "'(:'",
    "')'",
    "'*'",
    "'+'",
    "','",
    "'-'",
    "'.'",
    "'..'",
    "'/'",
    "'//'",
    "':'",
    "':)'",
    "'::'",
    "':='",
    "'<'",
    "'<<'",
    "'<='",
    "'='",
    "'=>'",
    "'>'",
    "'>='",
    "'>>'",
    "'?'",
    "'@'",
    "'['",
    "']'",
    "'ancestor'",
    "'ancestor-or-self'",
    "'and'",
    "'array'",
    "'as'",
    "'attribute'",
    "'cast'",
    "'castable'",
    "'child'",
    "'comment'",
    "'descendant'",
    "'descendant-or-self'",
    "'div'",
    "'document-node'",
    "'element'",
    "'else'",
    "'empty-sequence'",
    "'eq'",
    "'every'",
    "'except'",
    "'following'",
    "'following-sibling'",
    "'for'",
    "'function'",
    "'ge'",
    "'gt'",
    "'idiv'",
    "'if'",
    "'in'",
    "'instance'",
    "'intersect'",
    "'is'",
    "'item'",
    "'le'",
    "'let'",
    "'lt'",
    "'map'",
    "'mod'",
    "'namespace'",
    "'namespace-node'",
    "'ne'",
    "'node'",
    "'of'",
    "'or'",
    "'parent'",
    "'preceding'",
    "'preceding-sibling'",
    "'processing-instruction'",
    "'return'",
    "'satisfies'",
    "'schema-attribute'",
    "'schema-element'",
    "'self'",
    "'some'",
    "'switch'",
    "'text'",
    "'then'",
    "'to'",
    "'treat'",
    "'typeswitch'",
    "'union'",
    "'{'",
    "'|'",
    "'||'",
    "'}'"
  };
}

// End
