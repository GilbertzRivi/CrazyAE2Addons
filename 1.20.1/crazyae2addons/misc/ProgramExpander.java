package net.oktawia.crazyae2addons.misc;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramExpander {

    public static Result expand(String code) {
        try {
            String[] parts = code.split("\\|", 3);
            if (parts.length < 2) return Result.error("Code must have at least MAP | PROGRAM or MAP | MACRO | PROGRAM");

            String mapPart = parts[0];
            String macroPart = parts.length == 3 ? parts[1] : "";
            String programPart = parts.length == 3 ? parts[2] : parts[1];

            Map<Integer, String> blockMap = parseBlockMap(mapPart);
            Map<String, String> macros = parseMacros(macroPart);

            String expandedProgram = expandMacros(programPart, macros);

            List<String> expanded = tokenize(expandedProgram, blockMap, 0);
            return Result.success(expanded);
        } catch (Exception e) {
            return Result.error("Failed to expand program: " + e.getMessage());
        }
    }

    private static String expandMacros(String input, Map<String, String> macros) throws Exception {
        String previous;
        int maxDepth = 50;
        int depth = 0;

        do {
            previous = input;
            StringBuilder output = new StringBuilder();
            int i = 0;
            while (i < input.length()) {
                char c = input.charAt(i);
                if (c == '[') {
                    int j = i + 1;
                    while (j < input.length() && input.charAt(j) != ']') j++;
                    if (j >= input.length()) throw new Exception("Unclosed macro reference at position " + i);
                    String macroName = input.substring(i + 1, j);
                    if (!macros.containsKey(macroName)) throw new Exception("Macro [" + macroName + "] not defined at position " + i);
                    output.append(macros.get(macroName));
                    i = j + 1;
                } else {
                    output.append(c);
                    i++;
                }
            }
            input = output.toString();
            depth++;
        } while (!input.equals(previous) && depth < maxDepth);

        if (depth >= maxDepth)
            throw new Exception("Macro expansion too deep (possible infinite recursion)");

        return input;
    }

    private static Map<Integer, String> parseBlockMap(String map) throws Exception {
        Map<Integer, String> result = new HashMap<>();
        List<String> entries = splitMapEntries(map);

        Pattern pattern = Pattern.compile("^(\\d+)\\((.+)\\)$");

        for (String entry : entries) {
            Matcher matcher = pattern.matcher(entry.trim());
            if (!matcher.matches()) {
                throw new Exception("Invalid block map entry: " + entry);
            }
            int id = Integer.parseInt(matcher.group(1));
            String block = matcher.group(2);

            if (block.contains("{"))
                throw new Exception("Block definitions must not contain NBT data: " + block);

            if (result.containsKey(id)) {
                throw new Exception("Duplicate block ID: " + id);
            }
            result.put(id, block);
        }
        return result;
    }

    private static List<String> splitMapEntries(String map) throws Exception {
        List<String> entries = new ArrayList<>();
        int bracketLevelSquare = 0;
        int bracketLevelRound = 0;
        int lastSplit = 0;

        for (int i = 0; i < map.length(); i++) {
            char c = map.charAt(i);
            if (c == '[') bracketLevelSquare++;
            else if (c == ']') bracketLevelSquare--;
            else if (c == '(') bracketLevelRound++;
            else if (c == ')') bracketLevelRound--;
            else if (c == ',' && bracketLevelSquare == 0 && bracketLevelRound == 0) {
                entries.add(map.substring(lastSplit, i));
                lastSplit = i + 1;
            }
            if (bracketLevelSquare < 0 || bracketLevelRound < 0)
                throw new Exception("Unbalanced brackets in map");
        }
        if (lastSplit < map.length()) {
            entries.add(map.substring(lastSplit));
        }
        return entries;
    }

    private static Map<String, String> parseMacros(String macro) throws Exception {
        Map<String, String> result = new HashMap<>();
        int i = 0;
        while (i < macro.length()) {
            if (macro.charAt(i) == '[') {
                int nameStart = i + 1;
                int nameEnd = macro.indexOf(']', nameStart);
                if (nameEnd == -1) throw new Exception("Unclosed macro name at position " + i);
                String name = macro.substring(nameStart, nameEnd);

                if (nameEnd + 1 >= macro.length() || macro.charAt(nameEnd + 1) != '(')
                    throw new Exception("Expected '(' after macro name at position " + nameEnd);

                int bodyStart = nameEnd + 2;
                int depth = 1;
                int j = bodyStart;
                while (j < macro.length() && depth > 0) {
                    char c = macro.charAt(j);
                    if (c == '(') depth++;
                    else if (c == ')') depth--;
                    j++;
                }

                if (depth != 0) throw new Exception("Unclosed macro body for [" + name + "]");

                String body = macro.substring(bodyStart, j - 1);
                result.put(name, body);
                i = j;
            } else if (Character.isWhitespace(macro.charAt(i))) {
                i++;
            } else {
                throw new Exception("Unexpected character in macro definition at position " + i);
            }
        }
        return result;
    }

    private static List<String> tokenize(String input, Map<Integer, String> blockMap, int depth) throws Exception {
        if (depth > 50) throw new Exception("Too many nested expansions (possible infinite loop)");
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == 'P' && i + 1 < input.length() && input.charAt(i + 1) == '(') {
                int j = i + 2;
                while (j < input.length() && input.charAt(j) != ')') j++;
                if (j >= input.length()) throw new Exception("Unclosed P(n) at position " + i);
                String idStr = input.substring(i + 2, j);
                if (!idStr.matches("\\d+")) throw new Exception("Invalid block ID in P(n) at position " + i);
                int id = Integer.parseInt(idStr);
                if (!blockMap.containsKey(id)) throw new Exception("Block ID [" + id + "] not defined in map at position " + i);
                tokens.add("P|" + blockMap.get(id));
                i = j + 1;
            } else if ("FBLRUDXH".indexOf(c) != -1) {
                tokens.add(String.valueOf(c));
                i++;
            } else if (Character.isDigit(c)) {
                int j = i;
                while (j < input.length() && Character.isDigit(input.charAt(j))) j++;
                int count = Integer.parseInt(input.substring(i, j));
                if (j >= input.length() || input.charAt(j) != '{') throw new Exception("Expected '{' after repeat count at position " + i);
                j++;
                int depthCount = 1;
                StringBuilder loopBody = new StringBuilder();
                while (j < input.length() && depthCount > 0) {
                    char cj = input.charAt(j);
                    if (cj == '{') depthCount++;
                    else if (cj == '}') depthCount--;
                    if (depthCount > 0) loopBody.append(cj);
                    j++;
                }
                if (depthCount != 0) throw new Exception("Unmatched '{' at position " + i);
                List<String> expanded = tokenize(loopBody.toString(), blockMap, depth + 1);
                for (int k = 0; k < count; k++) tokens.addAll(expanded);
                i = j;
            } else if (input.startsWith("Z(", i)) {
                int j = i + 2;
                while (j < input.length() && input.charAt(j) != ')') j++;
                if (j >= input.length()) throw new Exception("Unclosed Z(n) at position " + i);
                String delay = input.substring(i + 2, j);
                tokens.add("Z|" + delay);
                i = j + 1;
            } else if (Character.isWhitespace(c)) {
                i++;
            } else {
                throw new Exception("Unexpected character: '" + c + "' at position " + i);
            }
        }
        return tokens;
    }

    public static class Result {
        public final boolean success;
        public final List<String> program;
        public final String error;

        private Result(boolean success, List<String> program, String error) {
            this.success = success;
            this.program = program;
            this.error = error;
        }

        public static Result success(List<String> program) {
            return new Result(true, program, null);
        }

        public static Result error(String errorMessage) {
            return new Result(false, null, errorMessage);
        }
    }

    public static Map<String, Integer> countUsedBlocks(String compiledCode) {
        Map<String, Integer> usage = new HashMap<>();
        try {
            String[] tokens = compiledCode.split("/");
            for (String token : tokens) {
                if (token.startsWith("P|")) {
                    String block = token.substring(2);
                    usage.put(block.split("\\[")[0], usage.getOrDefault(block.split("\\[")[0], 0) + 1);
                }
            }
        } catch (Exception e) {
            return Collections.emptyMap();
        }
        return usage;
    }
}
