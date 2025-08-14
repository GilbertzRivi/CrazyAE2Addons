package net.oktawia.crazyae2addons.misc;

import appeng.api.stacks.AEItemKey;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public final class TagMatcher {

    public static boolean doesItemMatch(@Nullable AEItemKey item, String expr) {
        if (item == null || expr == null || expr.isBlank()) return false;
        try {
            Node root = new Parser(expr).parseExpression();
            Holder<Item> holder = item.getItem().builtInRegistryHolder();
            return root.eval(holder);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private interface Node { boolean eval(Holder<Item> holder); }

    private record HasTagNode(TagKey<Item> key) implements Node {
        @Override public boolean eval(Holder<Item> h) { return h.is(key); }
    }

    private record GlobNode(Pattern regex) implements Node {
        @Override public boolean eval(Holder<Item> h) {
            return h.tags().anyMatch(k -> regex.matcher(k.location().toString()).matches());
        }
    }

    private record OpNode(Node l, Node r, BiFunction<Boolean, Boolean, Boolean> op) implements Node {
        @Override public boolean eval(Holder<Item> h) { return op.apply(l.eval(h), r.eval(h)); }
    }

    private record NotNode(Node n) implements Node {
        @Override public boolean eval(Holder<Item> h) { return !n.eval(h); }
    }

    private static final class Parser {
        private final String src; private int pos;
        Parser(String s){ this.src = s; }

        Node parseExpression() {
            Node n = parseTerm();
            while (true) {
                skipWs();
                if (peek("||") || peekInsensitive("or")) {
                    if (peek("||")) consume(2); else consumeInsensitive("or");
                    n = new OpNode(n, parseTerm(), (a, b) -> a || b);
                } else if (peekInsensitive("xor") || peek("^^")) {
                    if (peekInsensitive("xor")) consumeInsensitive("xor");
                    else consume(2);
                    n = new OpNode(n, parseTerm(), (a, b) -> a ^ b);
                } else {
                    break;
                }
            }
            return n;
        }

        Node parseTerm() {
            Node n = parseFactor();
            while (true) {
                skipWs();
                if (peek("&&") || peekInsensitive("and")) {
                    if (peek("&&")) consume(2); else consumeInsensitive("and");
                    n = new OpNode(n, parseFactor(), (a, b) -> a && b);
                } else if (peekInsensitive("nand") || peek("!&")) {
                    if (peekInsensitive("nand")) consumeInsensitive("nand");
                    else consume(2);
                    n = new OpNode(n, parseFactor(), (a, b) -> !(a && b));
                } else {
                    break;
                }
            }
            return n;
        }

        Node parseFactor() {
            skipWs();

            if (peek("!") && !peek("!&")) {
                consume(1);
                return new NotNode(parseFactor());
            }
            if (peekInsensitive("not")) {
                consumeInsensitive("not");
                return new NotNode(parseFactor());
            }

            if (peek("(")) {
                consume(1);
                Node n = parseExpression();
                expect(')');
                return n;
            }

            if (peek("#") || isTagStartChar(currentChar())) {
                String tok = readTagToken();
                return tokenToNode(tok);
            }

            throw err("Nieoczekiwany token");
        }

        private Node tokenToNode(String tokenRaw) {
            String token = stripHash(tokenRaw).toLowerCase(Locale.ROOT);
            if (token.indexOf('*') >= 0) {
                Pattern rx = globToRegex(token);
                return new GlobNode(rx);
            } else {
                TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.parse(token));
                return new HasTagNode(key);
            }
        }

        private String stripHash(String s){ return s.startsWith("#") ? s.substring(1) : s; }

        private Pattern globToRegex(String glob) {
            StringBuilder sb = new StringBuilder(glob.length()*2);
            sb.append("^");
            for (int i = 0; i < glob.length(); i++) {
                char ch = glob.charAt(i);
                if (ch == '*') { sb.append(".*"); continue; }
                if ("\\.[]{}()+-^$|?".indexOf(ch) >= 0) sb.append('\\');
                sb.append(ch);
            }
            sb.append("$");
            return Pattern.compile(sb.toString());
        }

        private boolean isTagStartChar(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '_' || ch == ':' || ch == '*';
        }

        private String readTagToken() {
            int start = pos;
            if (peek("#")) pos++;
            while (pos < src.length()) {
                char ch = src.charAt(pos);
                if (Character.isWhitespace(ch)) break;
                if (ch == '(' || ch == ')' || ch == '&' || ch == '|' || ch == '^' || ch == '!') break;
                pos++;
            }
            return src.substring(start, pos);
        }

        private char currentChar(){ return pos < src.length() ? src.charAt(pos) : '\0'; }
        private boolean peek(String s){ return src.regionMatches(pos, s, 0, s.length()); }
        private boolean peekInsensitive(String s){ return src.regionMatches(true, pos, s, 0, s.length()); }
        private void consume(int n){ pos += n; }
        private void consumeInsensitive(String s){ if (!peekInsensitive(s)) throw err("Expected \""+s+"\""); pos += s.length(); }
        private void skipWs(){ while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++; }
        private void expect(char c){ if (pos >= src.length() || src.charAt(pos) != c) throw err("Oczekiwano '"+c+"'"); pos++; }
        private IllegalStateException err(String m){ return new IllegalStateException(m+" (pos="+pos+") w '"+src+"'"); }
    }

    private TagMatcher() {}
}
