// This file was generated on Fri Oct 7, 2016 20:00 (UTC-05) by REx v5.41 which is Copyright (c) 1979-2016 by Gunther Rademacher <grd@gmx.net>
// REx command line: calc.ebnf -backtrack -java

package com.xmlcalabash.calc;

import java.util.Arrays;

public class Calc {
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

    public Calc(CharSequence string, EventHandler t)
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
        ex = -1;
        memo.clear();
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

    public void parse_input()
    {
        eventHandler.startNonterminal("input", e0);
        parse_expr();
        lookahead1(0);                  // eof
        consume(1);                     // eof
        eventHandler.endNonterminal("input", e0);
    }

    private void parse_expr()
    {
        eventHandler.startNonterminal("expr", e0);
        parse_term();
        lookahead1(3);                  // eof | ')' | '+'
        if (l1 == 6)                    // '+'
        {
            consume(6);                   // '+'
            parse_term();
        }
        eventHandler.endNonterminal("expr", e0);
    }

    private void try_expr()
    {
        try_term();
        lookahead1(3);                  // eof | ')' | '+'
        if (l1 == 6)                    // '+'
        {
            consumeT(6);                  // '+'
            try_term();
        }
    }

    private void parse_term()
    {
        eventHandler.startNonterminal("term", e0);
        parse_signed_factor();
        lookahead1(5);                  // eof | ')' | '*' | '+'
        if (l1 == 5)                    // '*'
        {
            consume(5);                   // '*'
            parse_signed_factor();
        }
        eventHandler.endNonterminal("term", e0);
    }

    private void try_term()
    {
        try_signed_factor();
        lookahead1(5);                  // eof | ')' | '*' | '+'
        if (l1 == 5)                    // '*'
        {
            consumeT(5);                  // '*'
            try_signed_factor();
        }
    }

    private void parse_signed_factor()
    {
        eventHandler.startNonterminal("signed-factor", e0);
        lookahead1(4);                  // number | '(' | '-'
        switch (l1)
        {
            case 7:                         // '-'
                lookahead2(4);                // number | '(' | '-'
                switch (lk)
                {
                    case 39:                      // '-' number
                        lookahead3(5);              // eof | ')' | '*' | '+'
                        break;
                }
                break;
            default:
                lk = l1;
        }
        if (lk == 295                   // '-' number eof
                || lk == 1063                  // '-' number ')'
                || lk == 1319                  // '-' number '*'
                || lk == 1575)                 // '-' number '+'
        {
            lk = memoized(0, e0);
            if (lk == 0)
            {
                int b0A = b0; int e0A = e0; int l1A = l1;
                int b1A = b1; int e1A = e1; int l2A = l2;
                int b2A = b2; int e2A = e2; int l3A = l3;
                int b3A = b3; int e3A = e3;
                try
                {
                    consumeT(7);              // '-'
                    try_factor();
                    lk = -1;
                }
                catch (ParseException p1A)
                {
                    lk = -2;
                }
                b0 = b0A; e0 = e0A; l1 = l1A; if (l1 == 0) {end = e0A;} else {
                b1 = b1A; e1 = e1A; l2 = l2A; if (l2 == 0) {end = e1A;} else {
                    b2 = b2A; e2 = e2A; l3 = l3A; if (l3 == 0) {end = e2A;} else {
                        b3 = b3A; e3 = e3A; end = e3A; }}}
                memoize(0, e0, lk);
            }
        }
        switch (lk)
        {
            case -1:
            case 55:                        // '-' '('
            case 119:                       // '-' '-'
                consume(7);                   // '-'
                parse_factor();
                break;
            default:
                parse_factor();
        }
        eventHandler.endNonterminal("signed-factor", e0);
    }

    private void try_signed_factor()
    {
        lookahead1(4);                  // number | '(' | '-'
        switch (l1)
        {
            case 7:                         // '-'
                lookahead2(4);                // number | '(' | '-'
                switch (lk)
                {
                    case 39:                      // '-' number
                        lookahead3(5);              // eof | ')' | '*' | '+'
                        break;
                }
                break;
            default:
                lk = l1;
        }
        if (lk == 295                   // '-' number eof
                || lk == 1063                  // '-' number ')'
                || lk == 1319                  // '-' number '*'
                || lk == 1575)                 // '-' number '+'
        {
            lk = memoized(0, e0);
            if (lk == 0)
            {
                int b0A = b0; int e0A = e0; int l1A = l1;
                int b1A = b1; int e1A = e1; int l2A = l2;
                int b2A = b2; int e2A = e2; int l3A = l3;
                int b3A = b3; int e3A = e3;
                try
                {
                    consumeT(7);              // '-'
                    try_factor();
                    memoize(0, e0A, -1);
                    lk = -3;
                }
                catch (ParseException p1A)
                {
                    lk = -2;
                    b0 = b0A; e0 = e0A; l1 = l1A; if (l1 == 0) {end = e0A;} else {
                    b1 = b1A; e1 = e1A; l2 = l2A; if (l2 == 0) {end = e1A;} else {
                        b2 = b2A; e2 = e2A; l3 = l3A; if (l3 == 0) {end = e2A;} else {
                            b3 = b3A; e3 = e3A; end = e3A; }}}
                    memoize(0, e0A, -2);
                }
            }
        }
        switch (lk)
        {
            case -1:
            case 55:                        // '-' '('
            case 119:                       // '-' '-'
                consumeT(7);                  // '-'
                try_factor();
                break;
            case -3:
                break;
            default:
                try_factor();
        }
    }

    private void parse_factor()
    {
        eventHandler.startNonterminal("factor", e0);
        lookahead1(4);                  // number | '(' | '-'
        switch (l1)
        {
            case 3:                         // '('
                consume(3);                   // '('
                parse_expr();
                lookahead1(2);                // ')'
                consume(4);                   // ')'
                break;
            default:
                parse_signed_number();
        }
        eventHandler.endNonterminal("factor", e0);
    }

    private void try_factor()
    {
        lookahead1(4);                  // number | '(' | '-'
        switch (l1)
        {
            case 3:                         // '('
                consumeT(3);                  // '('
                try_expr();
                lookahead1(2);                // ')'
                consumeT(4);                  // ')'
                break;
            default:
                try_signed_number();
        }
    }

    private void parse_signed_number()
    {
        eventHandler.startNonterminal("signed-number", e0);
        switch (l1)
        {
            case 7:                         // '-'
                consume(7);                   // '-'
                lookahead1(1);                // number
                consume(2);                   // number
                break;
            default:
                consume(2);                   // number
        }
        eventHandler.endNonterminal("signed-number", e0);
    }

    private void try_signed_number()
    {
        switch (l1)
        {
            case 7:                         // '-'
                consumeT(7);                  // '-'
                lookahead1(1);                // number
                consumeT(2);                  // number
                break;
            default:
                consumeT(2);                  // number
        }
    }

    private void consume(int t)
    {
        if (l1 == t)
        {
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

    private void lookahead1(int set)
    {
        if (l1 == 0)
        {
            l1 = match(set);
            b1 = begin;
            e1 = end;
        }
    }

    private void lookahead2(int set)
    {
        if (l2 == 0)
        {
            l2 = match(set);
            b2 = begin;
            e2 = end;
        }
        lk = (l2 << 4) | l1;
    }

    private void lookahead3(int set)
    {
        if (l3 == 0)
        {
            l3 = match(set);
            b3 = begin;
            e3 = end;
        }
        lk |= l3 << 8;
    }

    private int error(int b, int e, int s, int l, int t)
    {
        if (e >= ex)
        {
            bx = b;
            ex = e;
            sx = s;
            lx = l;
            tx = t;
        }
        throw new ParseException(bx, ex, sx, lx, tx);
    }

    private void memoize(int i, int e, int v)
    {
        memo.put((e << 0) + i, v);
    }

    private int memoized(int i, int e)
    {
        Integer v = memo.get((e << 0) + i);
        return v == null ? 0 : v;
    }

    private int lk, b0, e0;
    private int l1, b1, e1;
    private int l2, b2, e2;
    private int l3, b3, e3;
    private int bx, ex, sx, lx, tx;
    private EventHandler eventHandler = null;
    private java.util.Map<Integer, Integer> memo = new java.util.HashMap<Integer, Integer>();
    private CharSequence input = null;
    private int size = 0;
    private int begin = 0;
    private int end = 0;

    private int match(int tokenSetId)
    {
        begin = end;
        int current = end;
        int result = INITIAL[tokenSetId];
        int state = 0;

        for (int code = result & 7; code != 0; )
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
                int c1 = c0 >> 5;
                charclass = MAP1[(c0 & 31) + MAP1[(c1 & 31) + MAP1[c1 >> 5]]];
            }
            else
            {
                charclass = 0;
            }

            state = code;
            int i0 = (charclass << 3) + code - 1;
            code = TRANSITION[(i0 & 1) + TRANSITION[i0 >> 1]];

            if (code > 7)
            {
                result = code;
                code &= 7;
                end = current;
            }
        }

        result >>= 3;
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

        if (end > size) end = size;
        return (result & 15) - 1;
    }

    private static String[] getTokenSet(int tokenSetId)
    {
        java.util.ArrayList<String> expected = new java.util.ArrayList<>();
        int s = tokenSetId < 0 ? - tokenSetId : INITIAL[tokenSetId] & 7;
        for (int i = 0; i < 8; i += 32)
        {
            int j = i;
            int i0 = (i >> 5) * 6 + s - 1;
            int f = EXPECTED[i0];
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

    private static final int[] MAP0 =
            {
    /*   0 */ 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    /*  35 */ 0, 0, 0, 0, 0, 1, 2, 3, 4, 0, 5, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    /*  70 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    /* 105 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
            };

    private static final int[] MAP1 =
            {
    /*   0 */ 54, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
    /*  26 */ 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
    /*  52 */ 56, 56, 88, 121, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89,
    /*  78 */ 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    /* 110 */ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 0, 5, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6,
    /* 145 */ 6, 6, 0, 0, 0, 0, 0, 0
            };

    private static final int[] INITIAL =
            {
    /* 0 */ 1, 2, 3, 4, 5, 6
            };

    private static final int[] TRANSITION =
            {
    /*  0 */ 33, 33, 33, 33, 33, 33, 32, 33, 33, 39, 38, 33, 33, 33, 44, 33, 33, 41, 41, 33, 33, 33, 43, 33, 36, 33, 37,
    /* 27 */ 33, 35, 34, 34, 33, 32, 0, 0, 16, 0, 26, 0, 40, 40, 0, 56, 64, 0, 48
            };

    private static final int[] EXPECTED =
            {
    /* 0 */ 2, 4, 16, 82, 140, 114
            };

    private static final String[] TOKEN =
            {
                    "(0)",
                    "eof",
                    "number",
                    "'('",
                    "')'",
                    "'*'",
                    "'+'",
                    "'-'"
            };
}

// End
