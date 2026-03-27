package net.oktawia.crazyae2addons.misc;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public final class TagMatcher {

    @SuppressWarnings("deprecation")
    public static boolean doesItemMatch(@Nullable AEItemKey item, String expr) {
        if (item == null || expr == null || expr.isBlank()) return false;

        try {
            Compiled compiled = compileInternal(washExpression(expr));

            Holder<Item> holder = item.getItem().builtInRegistryHolder();
            Set<String> tags = holder.tags()
                    .map(tk -> tk.location().toString())
                    .collect(HashSet::new, Set::add, Set::addAll);

            return evalRpn(compiled.rpn, tags);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public static boolean doesFluidMatch(@Nullable AEFluidKey fluid, String expr) {
        if (fluid == null || expr == null || expr.isBlank()) return false;

        try {
            Compiled compiled = compileInternal(washExpression(expr));

            Holder<Fluid> holder = fluid.getFluid().builtInRegistryHolder();
            Set<String> tags = holder.tags()
                    .map(tk -> tk.location().toString())
                    .collect(HashSet::new, Set::add, Set::addAll);

            return evalRpn(compiled.rpn, tags);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String washExpression(String expression) {
        return expression.replace("&&", "&").replace("||", "|");
    }

    private record Compiled(Token[] rpn) {}

    private static Compiled compileInternal(String expression) {
        List<Token> tokens = tokenize(expression);
        Token[] rpn = toRpn(tokens);
        return new Compiled(rpn);
    }

    private enum TokenType { TAG, OPERATOR, LPAREN, RPAREN }

    private enum Operator {
        NOT("!", 3, true),
        AND("&", 2, false),
        XOR("^", 1, false),
        OR("|", 0, false);

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
            boolean wc = raw.indexOf('*') >= 0;
            return new Token(TokenType.TAG, raw, wc, null);
        }

        static Token op(Operator op) {
            return new Token(TokenType.OPERATOR, null, false, op);
        }

        static Token lparen() {
            return new Token(TokenType.LPAREN, null, false, null);
        }

        static Token rparen() {
            return new Token(TokenType.RPAREN, null, false, null);
        }
    }

    private static List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder currentTag = new StringBuilder();

        boolean expectingOperand = true;
        boolean lastIsTag = false;
        int lp = 0;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (c == '#') {
                throw new IllegalArgumentException("Character '#' is not allowed in tag expressions (pos " + i + ").");
            }

            if (Character.isWhitespace(c)) continue;

            Operator op = Operator.fromSymbol(c);

            if (c == '(') {
                if (!expectingOperand) throw new IllegalArgumentException("Unexpected '(' at position " + i);
                flushTag(currentTag, tokens);
                tokens.add(Token.lparen());
                lp++;
                lastIsTag = false;

            } else if (c == ')') {
                if (expectingOperand && lp <= 0) throw new IllegalArgumentException("Unexpected ')' at position " + i);
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
                    throw new IllegalArgumentException("Unexpected operator '" + c + "' at position " + i);
                }
                lastIsTag = false;

            } else {
                if (!expectingOperand) throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + i);
                currentTag.append(c);
                lastIsTag = true;
            }
        }

        flushTag(currentTag, tokens);

        if (tokens.isEmpty()) throw new IllegalArgumentException("Expression cannot be empty.");
        if (lp > 0) throw new IllegalArgumentException("Missing ')' at the end of the expression.");

        Token last = tokens.get(tokens.size() - 1);
        if (expectingOperand && last.type != TokenType.TAG && last.type != TokenType.RPAREN) {
            throw new IllegalArgumentException("Expression ended unexpectedly.");
        }

        return tokens;
    }

    private static void flushTag(StringBuilder currentTag, List<Token> tokens) {
        if (!currentTag.isEmpty()) {
            tokens.add(Token.tag(currentTag.toString()));
            currentTag.setLength(0);
        }
    }

    private static Token[] toRpn(List<Token> tokens) {
        ArrayList<Token> out = new ArrayList<>(tokens.size());
        Deque<Token> stack = new ArrayDeque<>();

        for (Token t : tokens) {
            switch (t.type) {
                case TAG -> out.add(t);

                case OPERATOR -> {
                    while (!stack.isEmpty() && stack.peek().type == TokenType.OPERATOR) {
                        Operator cur = t.op;
                        Operator top = stack.peek().op;

                        boolean shouldPop =
                                (!cur.rightAssociative && cur.precedence <= top.precedence) ||
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
                        if (top.type == TokenType.LPAREN) {
                            stack.pop();
                            found = true;
                            break;
                        }
                        out.add(stack.pop());
                    }
                    if (!found) throw new IllegalArgumentException("Mismatched parentheses.");
                }
            }
        }

        while (!stack.isEmpty()) {
            Token top = stack.pop();
            if (top.type == TokenType.LPAREN) throw new IllegalArgumentException("Mismatched parentheses.");
            out.add(top);
        }

        return out.toArray(Token[]::new);
    }

    private static boolean evalRpn(Token[] rpn, Set<String> actualTags) {
        boolean[] stack = new boolean[rpn.length];
        int sp = 0;

        for (Token t : rpn) {
            if (t.type == TokenType.TAG) {
                boolean match;
                if (!t.hasWildcard) {
                    match = actualTags.contains(t.tagPattern);
                } else {
                    match = matchesAnyGlob(t.tagPattern, actualTags);
                }
                stack[sp++] = match;

            } else if (t.type == TokenType.OPERATOR) {
                Operator op = t.op;

                if (op == Operator.NOT) {
                    if (sp < 1) throw new IllegalArgumentException("NOT needs 1 operand.");
                    stack[sp - 1] = !stack[sp - 1];
                } else {
                    if (sp < 2) throw new IllegalArgumentException(op.symbol + " needs 2 operands.");
                    boolean right = stack[--sp];
                    boolean left = stack[--sp];
                    boolean res = switch (op) {
                        case AND -> left && right;
                        case OR  -> left || right;
                        case XOR -> left ^ right;
                        default -> throw new IllegalStateException("Unexpected op: " + op);
                    };
                    stack[sp++] = res;
                }

            } else {
                throw new IllegalStateException("Paren token in RPN (should not happen).");
            }
        }

        if (sp == 1) return stack[0];
        if (sp == 0 && rpn.length == 0) return false;
        throw new IllegalArgumentException("Invalid expression: stack size " + sp);
    }

    private static boolean matchesAnyGlob(String pattern, Set<String> tags) {
        if ("*".equals(pattern)) return true;

        for (String tag : tags) {
            if (pattern.equals(tag)) return true;
            if (globMatchStarOnly(pattern, tag)) return true;
        }
        return false;
    }

    private static boolean globMatchStarOnly(String pattern, String text) {
        int p = 0, t = 0;
        int star = -1;
        int match = 0;

        while (t < text.length()) {
            if (p < pattern.length() && pattern.charAt(p) == text.charAt(t)) {
                p++; t++;
            } else if (p < pattern.length() && pattern.charAt(p) == '*') {
                star = p++;
                match = t;
            } else if (star != -1) {
                p = star + 1;
                t = ++match;
            } else {
                return false;
            }
        }

        while (p < pattern.length() && pattern.charAt(p) == '*') p++;
        return p == pattern.length();
    }

    private TagMatcher() {}
}
