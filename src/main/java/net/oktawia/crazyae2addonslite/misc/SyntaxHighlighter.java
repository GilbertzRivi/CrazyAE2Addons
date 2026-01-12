package net.oktawia.crazyae2addonslite.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class SyntaxHighlighter {

    public record Tok(String s, int col) {}

    public static final int COL_GRAY = 0xFFAAAAAA;
    public static final int COL_MAG  = 0xFFFF55FF;
    public static final int COL_GOLD = 0xFFffc53d;
    public static final int COL_CYAN = 0xFF55FFFF;
    public static final int COL_RED  = 0xFFff30be;
    private static final String DEFAULT_COLOR      = "&FFF";
    private static final Map<Character,int[]> BASE = Map.of(
            '(', new int[]{0,15,0},
            ')', new int[]{0,15,0},
            '[', new int[]{15,0,0},
            ']', new int[]{15,0,0},
            '{', new int[]{0,0,15},
            '}', new int[]{0,0,15}
    );
    private static final int DEFAULT_COLORN    = 0xFFFFFF;
    private static final int MD_MARKER_COLOR  = 0xFFC800;
    private static final int MD_COMMAND_COLOR = 0x00FFC8;
    private static final int MD_INDENT_COLOR  = 0x888888;


    private static boolean isHex(String s, int pos, int len) {
        if (pos + len > s.length()) return false;
        for (int i = 0; i < len; i++) {
            char ch = Character.toUpperCase(s.charAt(pos + i));
            if (!((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F'))) return false;
        }
        return true;
    }

    public static List<Tok> colorizeMarkdown(String src,
                                             int[] palette,
                                             HighlighterState st) {

        List<Tok> out = new ArrayList<>();
        int curCol = DEFAULT_COLORN;
        StringBuilder buf = new StringBuilder();

        BiConsumer<StringBuilder,Integer> flush = (b, c) -> {
            if (b.length() > 0) {
                out.add(new Tok(b.toString(), c));
                b.setLength(0);
            }
        };

        String[] lines = src.split("\\R", -1);
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li];
            int i = 0;

            while (line.startsWith(">>", i)) {
                flush.accept(buf, curCol);
                out.add(new Tok(">>", MD_INDENT_COLOR));
                i += 2;
            }

            if (i + 1 < line.length()
                    && (line.charAt(i) == '*' || line.charAt(i) == '-')
                    && line.charAt(i + 1) == ' ') {
                flush.accept(buf, curCol);
                out.add(new Tok(String.valueOf(line.charAt(i)), MD_MARKER_COLOR));
                i += 2;
                buf.append(' ');
            }

            while (i < line.length()) {
                char c = line.charAt(i);

                if (c == '&' && i + 7 < line.length()
                        && (line.charAt(i + 1) == 'c' || line.charAt(i + 1) == 'b')
                        && isHex(line, i + 2, 6)) {
                    flush.accept(buf, curCol);
                    out.add(new Tok(line.substring(i, i + 8), MD_COMMAND_COLOR));
                    i += 8;
                    continue;
                }

                if (i + 1 < line.length()
                        && (line.startsWith("**", i)
                        || line.startsWith("__", i)
                        || line.startsWith("~~", i))) {
                    flush.accept(buf, curCol);
                    out.add(new Tok(line.substring(i, i + 2), MD_MARKER_COLOR));
                    i += 2;
                    continue;
                }

                if (c == '*' && !(i + 1 < line.length() && line.charAt(i + 1) == '*')
                        && !(i > 0 && line.charAt(i - 1) == '*')) {
                    flush.accept(buf, curCol);
                    out.add(new Tok("*", MD_MARKER_COLOR));
                    i++;
                    continue;
                }

                buf.append(c);
                i++;
            }

            if (li < lines.length - 1) buf.append('\n');
        }
        flush.accept(buf, curCol);
        return out;
    }


    public static List<Tok> tokenize(String line, int[] bracketDepths, HighlighterState state) {
        List<Tok> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int color = state.currentColor;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (state.inQuotes) {
                buf.append(ch);
                if (ch == '"') {
                    flush(out, buf, COL_GOLD);
                    state.inQuotes = false;
                    color = 0xFFFFFFFF;
                }
                continue;
            }

            if (ch == '"') {
                flush(out, buf, color);
                buf.append(ch);
                state.inQuotes = true;
                color = COL_GOLD;
                continue;
            }

            if (ch == '(') {
                flush(out, buf, color);
                out.add(new Tok("(", bracketColor(0x5599FF, bracketDepths[0]++)));
                continue;
            } else if (ch == ')') {
                bracketDepths[0] = Math.max(0, bracketDepths[0] - 1);
                flush(out, buf, color);
                out.add(new Tok(")", bracketColor(0x5599FF, bracketDepths[0])));
                continue;
            } else if (ch == '[') {
                flush(out, buf, color);
                out.add(new Tok("[", bracketColor(0x55FF55, bracketDepths[1]++)));
                color = 0xFFDDDDDD;
                continue;
            } else if (ch == ']') {
                flush(out, buf, color);
                bracketDepths[1] = Math.max(0, bracketDepths[1] - 1);
                out.add(new Tok("]", bracketColor(0x55FF55, bracketDepths[1])));
                color = 0xFFFFFFFF;
                continue;
            } else if (ch == '{') {
                flush(out, buf, color);
                out.add(new Tok("{", bracketColor(0xffd166, bracketDepths[2]++)));
                continue;
            } else if (ch == '}') {
                bracketDepths[2] = Math.max(0, bracketDepths[2] - 1);
                flush(out, buf, color);
                out.add(new Tok("}", bracketColor(0xffd166, bracketDepths[2])));
                continue;
            }

            if (Character.isDigit(ch) && i + 1 < line.length() && line.charAt(i + 1) == '(') {
                flush(out, buf, color);
                int j = i;
                while (j < line.length() && Character.isDigit(line.charAt(j))) j++;
                out.add(new Tok(line.substring(i, j), 0xFFFFDD55));
                i = j - 1;
                continue;
            }

            if (Character.isLetter(ch)) {
                int j = i;
                boolean hasColon = false;
                while (j < line.length()) {
                    char cj = line.charAt(j);
                    if (cj == ':') {
                        if (hasColon) break;
                        hasColon = true;
                    } else if (!Character.isLetterOrDigit(cj) && cj != '_') {
                        break;
                    }
                    j++;
                }

                if (hasColon && j > i + 2) {
                    flush(out, buf, color);
                    out.add(new Tok(line.substring(i, j), 0xFF66CCFF));
                    i = j - 1;
                    continue;
                }
            }

            if (":,=".indexOf(ch) >= 0) {
                flush(out, buf, color);
                out.add(new Tok(String.valueOf(ch), COL_GRAY));
                continue;
            }

            if (matchWord(line, i, "AND", "OR", "XOR", "NAND", "P", "Z", "X", "F", "B", "U", "D", "R", "L", "H")) {
                flush(out, buf, color);
                String w = readWord(line, i);
                out.add(new Tok(w, COL_RED));
                i += w.length() - 1;
                continue;
            }

            if (Character.isDigit(ch) || (ch == '-' && i + 1 < line.length() && Character.isDigit(line.charAt(i + 1)))) {
                flush(out, buf, color);
                int j = i + 1;
                while (j < line.length() && (Character.isDigit(line.charAt(j)) || line.charAt(j) == '.')) j++;
                out.add(new Tok(line.substring(i, j), COL_CYAN));
                i = j - 1;
                continue;
            }

            buf.append(ch);
        }

        flush(out, buf, color);

        state.currentColor = color;
        return out;
    }


    private static void flush(List<Tok> out, StringBuilder buf, int col) {
        if (!buf.isEmpty()) {
            out.add(new Tok(buf.toString(), col));
            buf.setLength(0);
        }
    }

    private static int bracketColor(int baseRGB, int depth) {
        int r = (baseRGB >> 16) & 0xFF;
        int g = (baseRGB >> 8) & 0xFF;
        int b = baseRGB & 0xFF;

        float factor = (float) Math.pow(0.75, Math.min(depth, 6));
        r = (int)(r * factor);
        g = (int)(g * factor);
        b = (int)(b * factor);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static boolean matchWord(String s, int i, String... words) {
        String w = readWord(s, i);
        for (String t : words) if (t.equals(w)) return true;
        return false;
    }

    private static String readWord(String s, int i) {
        int j = i;
        while (j < s.length() && Character.isLetter(s.charAt(j))) j++;
        return s.substring(i, j);
    }

    private static String rgb(int r, int g, int b) {
        return "&" + Integer.toHexString(r).toUpperCase()
                + Integer.toHexString(g).toUpperCase()
                + Integer.toHexString(b).toUpperCase();
    }

    public static List<Tok> EmptyTokenizer(String s, int[] ints, HighlighterState highlighterState) {
        return List.of(new Tok(s, 0xFFFFFFFF));
    }
}
