package net.oktawia.crazyae2addons.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class LuaSyntaxHighlighter {

    private static final int COL_IDENT     = 0xFFDDDDDD;
    private static final int COL_KEYWORD   = 0xFFFFC66D;
    private static final int COL_BUILTIN   = 0xFF66D9EF;
    private static final int COL_NUMBER    = 0xFF55FFFF; // spójnie z builderem
    private static final int COL_STRING    = 0xFFFFC53D; // spójnie z builderem
    private static final int COL_OPERATOR  = 0xFFFF30BE; // spójnie z builderem
    private static final int COL_COMMENT   = 0xFF78828A;
    private static final int BASE_PAREN    = 0x5599FF;
    private static final int BASE_BRACKET  = 0x55FF55;
    private static final int BASE_BRACE    = 0xFFD166;

    private static final Set<String> KEYWORDS = Set.of(
            "and","break","do","else","elseif","end","false","for","function",
            "goto","if","in","local","nil","not","or","repeat","return","then",
            "true","until","while"
    );

    private static final Set<String> BUILTINS = Set.of(
            // globalne
            "assert","collectgarbage","dofile","error","_G","getmetatable","ipairs","load",
            "loadfile","next","pairs","pcall","print","rawequal","rawget","rawlen","rawset",
            "require","select","setmetatable","tonumber","tostring","type","xpcall",
            // biblioteki
            "coroutine","debug","io","math","os","package","string","table","utf8",
            // powszechne metody (ok podświetlić jako builtin)
            "sub","gsub","find","match","format","len","lower","upper","insert","remove","sort"
    );

    private LuaSyntaxHighlighter() {}

    public static List<SyntaxHighlighter.Tok> tokenize(String line, int[] bracketDepths, HighlighterState state) {
        List<SyntaxHighlighter.Tok> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int color = COL_IDENT;
        int i = 0, n = line.length();

        while (i < n) {
            char ch = line.charAt(i);

            // -- komentarz linijkowy
            if (startsWith(line, i, "--")) {
                // blokowy --[[ ... ]]
                if (startsWith(line, i + 2, "[[")) {
                    flush(out, buf, color);
                    int end = line.indexOf("]]", i + 4);
                    if (end < 0) {
                        out.add(new SyntaxHighlighter.Tok(line.substring(i), COL_COMMENT));
                        return out;
                    } else {
                        out.add(new SyntaxHighlighter.Tok(line.substring(i, end + 2), COL_COMMENT));
                        i = end + 2;
                        continue;
                    }
                } else {
                    flush(out, buf, color);
                    out.add(new SyntaxHighlighter.Tok(line.substring(i), COL_COMMENT));
                    break;
                }
            }

            // long-string [[ ... ]]
            if (startsWith(line, i, "[[")) {
                flush(out, buf, color);
                int end = line.indexOf("]]", i + 2);
                if (end < 0) {
                    out.add(new SyntaxHighlighter.Tok(line.substring(i), COL_STRING));
                    break;
                } else {
                    out.add(new SyntaxHighlighter.Tok(line.substring(i, end + 2), COL_STRING));
                    i = end + 2;
                    continue;
                }
            }

            // stringi '...' lub "..." z escapami
            if (ch == '\'' || ch == '"') {
                flush(out, buf, color);
                int start = i++;
                char quote = ch;
                boolean escape = false;
                while (i < n) {
                    char c = line.charAt(i++);
                    if (escape) { escape = false; continue; }
                    if (c == '\\') { escape = true; continue; }
                    if (c == quote) break;
                }
                out.add(new SyntaxHighlighter.Tok(line.substring(start, Math.min(i, n)), COL_STRING));
                continue;
            }

            // nawiasy z gradientem
            if (ch == '(') {
                flush(out, buf, color);
                out.add(new SyntaxHighlighter.Tok("(", bracketColor(BASE_PAREN, bracketDepths[0]++)));
                i++; continue;
            } else if (ch == ')') {
                bracketDepths[0] = Math.max(0, bracketDepths[0] - 1);
                flush(out, buf, color);
                out.add(new SyntaxHighlighter.Tok(")", bracketColor(BASE_PAREN, bracketDepths[0])));
                i++; continue;
            } else if (ch == '[') {
                flush(out, buf, color);
                out.add(new SyntaxHighlighter.Tok("[", bracketColor(BASE_BRACKET, bracketDepths[1]++)));
                i++; continue;
            } else if (ch == ']') {
                bracketDepths[1] = Math.max(0, bracketDepths[1] - 1);
                flush(out, buf, color);
                out.add(new SyntaxHighlighter.Tok("]", bracketColor(BASE_BRACKET, bracketDepths[1])));
                i++; continue;
            } else if (ch == '{') {
                flush(out, buf, color);
                out.add(new SyntaxHighlighter.Tok("{", bracketColor(BASE_BRACE, bracketDepths[2]++)));
                i++; continue;
            } else if (ch == '}') {
                bracketDepths[2] = Math.max(0, bracketDepths[2] - 1);
                flush(out, buf, color);
                out.add(new SyntaxHighlighter.Tok("}", bracketColor(BASE_BRACE, bracketDepths[2])));
                i++; continue;
            }

            // operatory 2-znakowe
            if (i + 1 < n) {
                String two = line.substring(i, i + 2);
                if (two.equals("..") || two.equals("==") || two.equals("~=") ||
                    two.equals("<=") || two.equals(">=") || two.equals("//")) {
                    flush(out, buf, color);
                    out.add(new SyntaxHighlighter.Tok(two, COL_OPERATOR));
                    i += 2; continue;
                }
            }
            // operatory 1-znakowe
            if ("+-*/%^#<>=:,.~".indexOf(ch) >= 0) {
                flush(out, buf, color);
                out.add(new SyntaxHighlighter.Tok(String.valueOf(ch), COL_OPERATOR));
                i++; continue;
            }

            // liczby: hex/dec/float/e
            if (Character.isDigit(ch) || (ch == '.' && i + 1 < n && Character.isDigit(line.charAt(i + 1)))) {
                flush(out, buf, color);
                int j = i;
                if (startsWith(line, i, "0x") || startsWith(line, i, "0X")) {
                    j += 2;
                    while (j < n && Character.digit(line.charAt(j), 16) != -1) j++;
                } else {
                    boolean dot = false, exp = false;
                    while (j < n) {
                        char cj = line.charAt(j);
                        if (Character.isDigit(cj)) { j++; continue; }
                        if (cj == '.' && !dot) { dot = true; j++; continue; }
                        if ((cj == 'e' || cj == 'E') && !exp) {
                            exp = true; j++;
                            if (j < n && (line.charAt(j) == '+' || line.charAt(j) == '-')) j++;
                            continue;
                        }
                        break;
                    }
                }
                out.add(new SyntaxHighlighter.Tok(line.substring(i, j), COL_NUMBER));
                i = j; continue;
            }

            // ident / keyword / builtin / call
            if (isIdStart(ch)) {
                flush(out, buf, color);
                int j = i + 1;
                while (j < n && isIdPart(line.charAt(j))) j++;
                String ident = line.substring(i, j);

                int col;
                if (KEYWORDS.contains(ident)) {
                    col = COL_KEYWORD;
                } else if (BUILTINS.contains(ident)) {
                    col = COL_BUILTIN;
                } else {
                    // funccall? (ident + spacje + '(')
                    int k = j;
                    while (k < n && Character.isWhitespace(line.charAt(k))) k++;
                    col = (k < n && line.charAt(k) == '(') ? 0xFF66CCFF /* call */ : COL_IDENT;
                }
                out.add(new SyntaxHighlighter.Tok(ident, col));
                i = j; continue;
            }

            // domyślnie
            buf.append(ch);
            i++;
        }

        flush(out, buf, color);
        return out;
    }

    // === helpers ===
    private static boolean isIdStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static boolean startsWith(String s, int i, String pat) {
        int n = pat.length();
        if (i + n > s.length()) return false;
        for (int k = 0; k < n; k++) if (s.charAt(i + k) != pat.charAt(k)) return false;
        return true;
    }

    private static void flush(List<SyntaxHighlighter.Tok> out, StringBuilder buf, int col) {
        if (buf.length() > 0) {
            out.add(new SyntaxHighlighter.Tok(buf.toString(), col));
            buf.setLength(0);
        }
    }

    private static int bracketColor(int baseRGB, int depth) {
        int r = (baseRGB >> 16) & 0xFF;
        int g = (baseRGB >> 8) & 0xFF;
        int b =  baseRGB        & 0xFF;
        float factor = (float) Math.pow(0.75, Math.min(depth, 6));
        r = (int)(r * factor);
        g = (int)(g * factor);
        b = (int)(b * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
