package net.oktawia.crazyae2addons.misc;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public final class TagMatcher {

    private static final class InvalidTagMatcherSyntaxException extends Exception {
        private InvalidTagMatcherSyntaxException(String s) { super(s); }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static Compiled compile(@Nullable String expr) {
        String washed = washExpression(expr == null ? "" : expr);
        if (washed.isBlank()) return Compiled.EMPTY;

        try {
            List<Token> tokens = tokenize(washed);
            Token[] rpn = toRpn(tokens);

            boolean needsTags = false;
            for (Token t : rpn) {
                if (t.type == TokenType.TAG) { needsTags = true; break; }
            }

            return new Compiled(rpn, true, needsTags);
        } catch (InvalidTagMatcherSyntaxException ex) {
            return Compiled.INVALID;
        }
    }

    /** Convenience overload — compiles inline (no caching of Compiled). */
    public static boolean doesItemMatch(@Nullable AEItemKey item, @Nullable String expr) {
        if (item == null || expr == null || expr.isBlank()) return false;
        Compiled c = compile(expr);
        return doesItemMatch(item, c);
    }

    /** Use when you have a pre-compiled expression (preferred in tight loops). */
    public static boolean doesItemMatch(@Nullable AEItemKey item, @Nullable Compiled compiled) {
        if (item == null || compiled == null || !compiled.isValid() || !compiled.needsTags()) return false;
        ItemTagCache cache = ItemTagCache.of(item);
        try {
            return evalRpn(compiled.rpn, cache.tags);
        } catch (InvalidTagMatcherSyntaxException e) {
            return false;
        }
    }

    /** Convenience overload — compiles inline (no caching of Compiled). */
    public static boolean doesFluidMatch(@Nullable AEFluidKey fluid, @Nullable String expr) {
        if (fluid == null || expr == null || expr.isBlank()) return false;
        Compiled c = compile(expr);
        return doesFluidMatch(fluid, c);
    }

    /** Use when you have a pre-compiled expression (preferred in tight loops). */
    public static boolean doesFluidMatch(@Nullable AEFluidKey fluid, @Nullable Compiled compiled) {
        if (fluid == null || compiled == null || !compiled.isValid() || !compiled.needsTags()) return false;
        FluidTagCache cache = FluidTagCache.of(fluid);
        try {
            return evalRpn(compiled.rpn, cache.tags);
        } catch (InvalidTagMatcherSyntaxException e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Compiled expression
    // -------------------------------------------------------------------------

    public static final class Compiled {

        final Token[] rpn;
        private final boolean valid;
        private final boolean needsTags;

        private Compiled(Token[] rpn, boolean valid, boolean needsTags) {
            this.rpn = rpn;
            this.valid = valid;
            this.needsTags = needsTags;
        }

        public boolean isValid()    { return valid; }
        public boolean needsTags()  { return needsTags; }

        public static final Compiled EMPTY   = new Compiled(new Token[0], true,  false);
        public static final Compiled INVALID = new Compiled(new Token[0], false, false);
    }

    // -------------------------------------------------------------------------
    // Tag caches (ThreadLocal IdentityHashMap — valid within one server tick)
    // -------------------------------------------------------------------------

    public static final class ItemTagCache {

        final Set<String> tags;

        private ItemTagCache(Set<String> tags) { this.tags = tags; }

        private static final ThreadLocal<Map<Item, Set<String>>> TL =
                ThreadLocal.withInitial(IdentityHashMap::new);

        @SuppressWarnings("deprecation")
        public static ItemTagCache of(AEItemKey key) {
            Item item = key.getItem();
            Map<Item, Set<String>> map = TL.get();
            Set<String> tags = map.get(item);
            if (tags == null) {
                Holder<Item> holder = item.builtInRegistryHolder();
                tags = holder.tags()
                        .map(tk -> tk.location().toString())
                        .collect(HashSet::new, Set::add, Set::addAll);
                map.put(item, tags);
            }
            return new ItemTagCache(tags);
        }

        public static void clearThreadLocal() { TL.remove(); }
    }

    public static final class FluidTagCache {

        final Set<String> tags;

        private FluidTagCache(Set<String> tags) { this.tags = tags; }

        private static final ThreadLocal<Map<Fluid, Set<String>>> TL =
                ThreadLocal.withInitial(IdentityHashMap::new);

        @SuppressWarnings("deprecation")
        public static FluidTagCache of(AEFluidKey key) {
            Fluid fluid = key.getFluid();
            Map<Fluid, Set<String>> map = TL.get();
            Set<String> tags = map.get(fluid);
            if (tags == null) {
                Holder<Fluid> holder = fluid.builtInRegistryHolder();
                tags = holder.tags()
                        .map(tk -> tk.location().toString())
                        .collect(HashSet::new, Set::add, Set::addAll);
                map.put(fluid, tags);
            }
            return new FluidTagCache(tags);
        }

        public static void clearThreadLocal() { TL.remove(); }
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private static String washExpression(String expression) {
        return expression.replace("&&", "&").replace("||", "|");
    }

    private enum TokenType { TAG, OPERATOR, LPAREN, RPAREN }

    private enum Operator {
        NOT("!", 3, true),
        AND("&", 2, false),
        XOR("^", 1, false),
        OR("|",  0, false);

        final String symbol;
        final int precedence;
        final boolean rightAssociative;

        Operator(String symbol, int precedence, boolean rightAssociative) {
            this.symbol = symbol;
            this.precedence = precedence;
            this.rightAssociative = rightAssociative;
        }

        static Operator fromSymbol(char symbol) {
            for (Operator op : values()) {
                if (op.symbol.charAt(0) == symbol) return op;
            }
            return null;
        }
    }

    private record Token(TokenType type, String tagPattern, boolean hasWildcard, Operator op) {

        static Token tag(String raw) {
            return new Token(TokenType.TAG, raw, raw.indexOf('*') >= 0, null);
        }
        static Token op(Operator op)  { return new Token(TokenType.OPERATOR, null, false, op); }
        static Token lparen()         { return new Token(TokenType.LPAREN,   null, false, null); }
        static Token rparen()         { return new Token(TokenType.RPAREN,   null, false, null); }
    }

    private static List<Token> tokenize(String expression) throws InvalidTagMatcherSyntaxException {
        List<Token> tokens = new ArrayList<>();
        StringBuilder currentTag = new StringBuilder();

        boolean expectingOperand = true;
        boolean lastIsTag = false;
        int lp = 0;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == '#') throw new InvalidTagMatcherSyntaxException(
                    "Character '#' is not allowed in tag expressions (pos " + i + ").");

            if (Character.isWhitespace(c)) continue;

            Operator op = Operator.fromSymbol(c);

            if (c == '(') {
                if (!expectingOperand) throw new InvalidTagMatcherSyntaxException("Unexpected '(' at position " + i);
                flushTag(currentTag, tokens);
                tokens.add(Token.lparen());
                lp++;
                lastIsTag = false;

            } else if (c == ')') {
                if (expectingOperand && lp <= 0) throw new InvalidTagMatcherSyntaxException("Unexpected ')' at position " + i);
                flushTag(currentTag, tokens);
                tokens.add(Token.rparen());
                expectingOperand = false;
                lp--;
                lastIsTag = false;

            } else if (op != null) {
                if (op == Operator.NOT && expectingOperand) {
                    flushTag(currentTag, tokens);
                    tokens.add(Token.op(op));
                } else if (op != Operator.NOT) {
                    if (lastIsTag || !expectingOperand) {
                        flushTag(currentTag, tokens);
                        tokens.add(Token.op(op));
                        expectingOperand = true;
                    }
                } else {
                    throw new InvalidTagMatcherSyntaxException("Unexpected operator '" + c + "' at position " + i);
                }
                lastIsTag = false;

            } else {
                if (!expectingOperand) throw new InvalidTagMatcherSyntaxException("Unexpected character '" + c + "' at position " + i);
                currentTag.append(c);
                lastIsTag = true;
            }
        }

        flushTag(currentTag, tokens);

        if (tokens.isEmpty()) throw new InvalidTagMatcherSyntaxException("Expression cannot be empty.");
        if (lp > 0) throw new InvalidTagMatcherSyntaxException("Missing ')' at the end of the expression.");

        Token last = tokens.get(tokens.size() - 1);
        if (expectingOperand && last.type != TokenType.TAG && last.type != TokenType.RPAREN) {
            throw new InvalidTagMatcherSyntaxException("Expression ended unexpectedly.");
        }

        return tokens;
    }

    private static void flushTag(StringBuilder currentTag, List<Token> tokens) {
        if (!currentTag.isEmpty()) {
            tokens.add(Token.tag(currentTag.toString()));
            currentTag.setLength(0);
        }
    }

    private static Token[] toRpn(List<Token> tokens) throws InvalidTagMatcherSyntaxException {
        ArrayList<Token> out = new ArrayList<>(tokens.size());
        Deque<Token> stack = new ArrayDeque<>();

        for (Token t : tokens) {
            switch (t.type) {
                case TAG -> out.add(t);

                case OPERATOR -> {
                    while (!stack.isEmpty() && stack.peek().type == TokenType.OPERATOR) {
                        Operator cur = t.op;
                        Operator top = stack.peek().op;
                        boolean shouldPop = (!cur.rightAssociative && cur.precedence <= top.precedence) ||
                                (cur.rightAssociative && cur.precedence < top.precedence);
                        if (shouldPop) out.add(stack.pop());
                        else break;
                    }
                    stack.push(t);
                }

                case LPAREN -> stack.push(t);

                case RPAREN -> {
                    boolean found = false;
                    while (!stack.isEmpty()) {
                        Token top = stack.peek();
                        if (top.type == TokenType.LPAREN) { stack.pop(); found = true; break; }
                        out.add(stack.pop());
                    }
                    if (!found) throw new InvalidTagMatcherSyntaxException("Mismatched parentheses.");
                }
            }
        }

        while (!stack.isEmpty()) {
            Token top = stack.pop();
            if (top.type == TokenType.LPAREN) throw new InvalidTagMatcherSyntaxException("Mismatched parentheses.");
            out.add(top);
        }

        Token[] rpn = out.toArray(Token[]::new);
        validateRpnStackDepth(rpn);
        return rpn;
    }

    private static void validateRpnStackDepth(Token[] rpn) throws InvalidTagMatcherSyntaxException {
        int sp = 0;
        for (Token t : rpn) {
            switch (t.type) {
                case TAG -> sp++;
                case OPERATOR -> {
                    int required = t.op == Operator.NOT ? 1 : 2;
                    sp -= required;
                    if (sp < 0) throw new InvalidTagMatcherSyntaxException("Unexpected operator " + t.op);
                    sp++;
                }
                case LPAREN, RPAREN -> throw new InvalidTagMatcherSyntaxException("Unexpected token: " + t.type);
            }
        }
        if (sp != 1) throw new InvalidTagMatcherSyntaxException("Depth at the end should equal 1");
    }

    private static boolean evalRpn(Token[] rpn, Set<String> actualTags) throws InvalidTagMatcherSyntaxException {
        if (rpn.length == 0) return false;

        boolean[] stack = new boolean[rpn.length];
        int sp = 0;

        for (Token t : rpn) {
            if (t.type == TokenType.TAG) {
                stack[sp++] = t.hasWildcard ? matchesAnyGlob(t.tagPattern, actualTags)
                                            : actualTags.contains(t.tagPattern);

            } else if (t.type == TokenType.OPERATOR) {
                Operator op = t.op;
                if (op == Operator.NOT) {
                    if (sp < 1) throw new InvalidTagMatcherSyntaxException("NOT needs 1 operand.");
                    stack[sp - 1] = !stack[sp - 1];
                } else {
                    if (sp < 2) throw new InvalidTagMatcherSyntaxException(op.symbol + " needs 2 operands.");
                    boolean right = stack[--sp];
                    boolean left  = stack[--sp];
                    stack[sp++] = switch (op) {
                        case AND -> left && right;
                        case OR  -> left || right;
                        case XOR -> left ^ right;
                        default  -> throw new InvalidTagMatcherSyntaxException("Unexpected op: " + op);
                    };
                }
            } else {
                throw new InvalidTagMatcherSyntaxException("Paren token in RPN (should not happen).");
            }
        }

        if (sp == 1) return stack[0];
        throw new InvalidTagMatcherSyntaxException("Invalid expression: stack size " + sp);
    }

    private static boolean matchesAnyGlob(String pattern, Set<String> tags) {
        if ("*".equals(pattern)) return true;
        for (String tag : tags) {
            if (globMatchStarOnly(pattern, tag)) return true;
        }
        return false;
    }

    private static boolean globMatchStarOnly(String pattern, String text) {
        int p = 0, t = 0, star = -1, match = 0;
        while (t < text.length()) {
            if (p < pattern.length() && pattern.charAt(p) == text.charAt(t)) { p++; t++; }
            else if (p < pattern.length() && pattern.charAt(p) == '*') { star = p++; match = t; }
            else if (star != -1) { p = star + 1; t = ++match; }
            else return false;
        }
        while (p < pattern.length() && pattern.charAt(p) == '*') p++;
        return p == pattern.length();
    }

    private TagMatcher() {}
}
