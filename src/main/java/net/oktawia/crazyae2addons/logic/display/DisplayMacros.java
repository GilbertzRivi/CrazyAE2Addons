package net.oktawia.crazyae2addons.logic.display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DisplayMacros {

    private static final Pattern LINE_SPLIT = Pattern.compile("&nl|\\r\\n|\\r|\\n");

    private static final Pattern RESERVED_NAME = Pattern.compile("nl|[cb][0-9a-f]{6}", Pattern.CASE_INSENSITIVE);

    private static final String DEFINITION_PREFIX = "$with(";

    private static final int MAX_MACROS = 64;
    private static final int MAX_NAME_LENGTH = 32;
    private static final int MAX_VALUE_LENGTH = 256;

    private DisplayMacros() {
    }

    public static String expand(String input) {
        if (input == null) {
            return "";
        }

        if (input.indexOf('$') < 0) {
            return input;
        }

        Map<String, String> macros = new LinkedHashMap<>();
        List<String> keptLines = new ArrayList<>();
        List<String> keptSeparators = new ArrayList<>();

        Matcher separator = LINE_SPLIT.matcher(input);
        int lineStart = 0;

        while (true) {
            boolean hasSeparator = separator.find(lineStart);
            int lineEnd = hasSeparator ? separator.start() : input.length();

            StringBuilder lineOut = new StringBuilder();
            boolean defined = stripDefinitions(input.substring(lineStart, lineEnd), macros, lineOut);
            String line = lineOut.toString();

            if (defined && line.isBlank()) {
                if (!hasSeparator && !keptSeparators.isEmpty()) {
                    keptSeparators.set(keptSeparators.size() - 1, "");
                }
            } else {
                keptLines.add(line);
                keptSeparators.add(hasSeparator ? input.substring(separator.start(), separator.end()) : "");
            }

            if (!hasSeparator) {
                break;
            }

            lineStart = separator.end();
        }

        StringBuilder out = new StringBuilder(input.length());

        for (int i = 0; i < keptLines.size(); i++) {
            out.append(macros.isEmpty() ? keptLines.get(i) : substitute(keptLines.get(i), macros));
            out.append(keptSeparators.get(i));
        }

        return out.toString();
    }

    public static Set<String> collectNames(String input) {
        if (input == null || input.indexOf('$') < 0) {
            return Set.of();
        }

        Map<String, String> macros = new LinkedHashMap<>();
        StringBuilder discarded = new StringBuilder();

        for (String line : LINE_SPLIT.split(input, -1)) {
            discarded.setLength(0);
            stripDefinitions(line, macros, discarded);
        }

        return macros.keySet();
    }

    private static boolean stripDefinitions(String line, Map<String, String> macros, StringBuilder out) {
        boolean defined = false;
        int i = 0;

        while (i < line.length()) {
            if (line.charAt(i) != '$'
                    || !line.regionMatches(true, i, DEFINITION_PREFIX, 0, DEFINITION_PREFIX.length())) {
                out.append(line.charAt(i));
                i++;
                continue;
            }

            int bodyStart = i + DEFINITION_PREFIX.length();
            int bodyEnd = findClosingParen(line, bodyStart);

            if (bodyEnd < 0 || !defineMacro(line.substring(bodyStart, bodyEnd), macros)) {
                out.append(line.charAt(i));
                i++;
                continue;
            }

            defined = true;
            i = bodyEnd + 1;

            if (i < line.length() && line.charAt(i) == ' ') {
                i++;
            }
        }

        return defined;
    }

    private static int findClosingParen(String s, int from) {
        int depth = 0;

        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }

        return -1;
    }

    private static boolean defineMacro(String body, Map<String, String> macros) {
        int eq = body.indexOf('=');
        if (eq < 0) {
            return false;
        }

        String name = body.substring(0, eq).trim();
        if (!isValidName(name) || (macros.size() >= MAX_MACROS && !macros.containsKey(name))) {
            return false;
        }

        String value = body.substring(eq + 1).trim();
        if (value.length() > MAX_VALUE_LENGTH) {
            return false;
        }

        macros.put(name, macros.isEmpty() ? value : substitute(value, macros));
        return true;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.isEmpty() && !RESERVED_NAME.matcher(name).matches() && isNameCharset(name);
    }

    public static boolean isNameCharset(String name) {
        if (name == null || name.length() > MAX_NAME_LENGTH) {
            return false;
        }

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean allowed = c == '_'
                    || (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z');

            if (!allowed) {
                return false;
            }
        }

        return true;
    }

    private static String substitute(String text, Map<String, String> macros) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);

            if (c != '&' && c != '^') {
                out.append(c);
                i++;
                continue;
            }

            int nameEnd = identifierEnd(text, i + 1);
            String value = nameEnd > i + 1 ? macros.get(text.substring(i + 1, nameEnd)) : null;

            if (value == null || continuesIdentifier(text, nameEnd, c)) {
                out.append(c);
                i++;
                continue;
            }

            if (c == '^') {
                out.append(c);
            }

            out.append(value);
            i = nameEnd;
        }

        return out.toString();
    }

    private static int identifierEnd(String text, int from) {
        int i = from;

        while (i < text.length()) {
            char c = text.charAt(i);
            boolean part = c == '_'
                    || (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z');

            if (!part) {
                break;
            }

            i++;
        }

        return i;
    }

    private static boolean continuesIdentifier(String text, int nameEnd, char prefix) {
        if (nameEnd >= text.length()) {
            return false;
        }

        char next = text.charAt(nameEnd);

        if (prefix == '^') {
            return next == ':' || next == '.' || next == '-' || next == '/';
        }

        return next == ':' || next == '^';
    }
}
