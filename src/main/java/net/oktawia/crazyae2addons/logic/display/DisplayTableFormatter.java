package net.oktawia.crazyae2addons.logic.display;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

public final class DisplayTableFormatter {

    private static final Pattern LINE_SPLIT = Pattern.compile("&nl|\\r\\n|\\r|\\n");

    private static final int MIN_CELL_WIDTH = 3;

    private static final int ALIGN_NONE = 0;
    private static final int ALIGN_LEFT = 1;
    private static final int ALIGN_CENTER = 2;
    private static final int ALIGN_RIGHT = 3;

    private record Row(String prefix, List<String> cells) {
    }

    private DisplayTableFormatter() {
    }

    public static boolean hasTable(String text) {
        List<String> lines = splitLines(text);

        for (int i = 0; i + 1 < lines.size(); i++) {
            if (isTableStart(lines, i)) {
                return true;
            }
        }

        return false;
    }

    public static String format(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }

        List<String> lines = splitLines(text);
        List<String> separators = splitSeparators(text);
        List<String> out = new ArrayList<>(lines.size());

        int i = 0;

        while (i < lines.size()) {
            if (!isTableStart(lines, i)) {
                out.add(lines.get(i));
                i++;
                continue;
            }

            int indent = indentLevel(parseRow(lines.get(i)).prefix());
            int end = i + 1;

            while (end + 1 < lines.size()) {
                String candidate = lines.get(end + 1);
                Row next = parseRow(candidate);

                if (next == null || pipeCount(candidate) < 2 || indentLevel(next.prefix()) != indent) {
                    break;
                }

                end++;
            }

            List<Row> rows = new ArrayList<>(end - i + 1);
            for (int r = i; r <= end; r++) {
                rows.add(parseRow(lines.get(r)));
            }

            out.addAll(formatBlock(rows));
            i = end + 1;
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int line = 0; line < out.size(); line++) {
            sb.append(out.get(line));
            if (line < separators.size()) {
                sb.append(separators.get(line));
            }
        }

        return sb.toString();
    }

    private static List<String> formatBlock(List<Row> rows) {
        int cols = 0;
        for (Row row : rows) {
            cols = Math.max(cols, row.cells().size());
        }

        int[] align = new int[cols];
        List<String> separatorCells = rows.get(1).cells();
        for (int c = 0; c < cols; c++) {
            align[c] = c < separatorCells.size() ? alignmentOf(separatorCells.get(c)) : ALIGN_NONE;
        }

        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) {
            widths[c] = MIN_CELL_WIDTH;
        }

        for (int r = 0; r < rows.size(); r++) {
            if (r == 1) {
                continue;
            }

            List<String> cells = rows.get(r).cells();
            for (int c = 0; c < cells.size(); c++) {
                widths[c] = Math.max(widths[c], cells.get(c).length());
            }
        }

        List<String> out = new ArrayList<>(rows.size());

        for (int r = 0; r < rows.size(); r++) {
            StringBuilder sb = new StringBuilder(rows.get(r).prefix());
            List<String> cells = rows.get(r).cells();

            for (int c = 0; c < cols; c++) {
                String cell = r == 1
                        ? separatorCell(widths[c], align[c])
                        : pad(c < cells.size() ? cells.get(c) : "", widths[c], align[c]);
                sb.append("| ").append(cell).append(' ');
            }

            sb.append('|');
            out.add(sb.toString());
        }

        return out;
    }

    private static String separatorCell(int width, int align) {
        StringBuilder sb = new StringBuilder(width);

        sb.append(align == ALIGN_LEFT || align == ALIGN_CENTER ? ':' : '-');
        for (int i = 2; i < width; i++) {
            sb.append('-');
        }
        sb.append(align == ALIGN_RIGHT || align == ALIGN_CENTER ? ':' : '-');

        return sb.toString();
    }

    private static int alignmentOf(String separatorCell) {
        String t = separatorCell.trim();
        boolean left = t.startsWith(":");
        boolean right = t.endsWith(":");

        if (left && right) {
            return ALIGN_CENTER;
        }
        if (right) {
            return ALIGN_RIGHT;
        }

        return left ? ALIGN_LEFT : ALIGN_NONE;
    }

    private static String pad(String cell, int width, int align) {
        int missing = width - cell.length();
        if (missing <= 0) {
            return cell;
        }

        if (align == ALIGN_RIGHT) {
            return " ".repeat(missing) + cell;
        }

        if (align == ALIGN_CENTER) {
            int left = missing / 2;
            return " ".repeat(left) + cell + " ".repeat(missing - left);
        }

        return cell + " ".repeat(missing);
    }

    private static boolean isTableStart(List<String> lines, int index) {
        if (index + 1 >= lines.size()) {
            return false;
        }

        Row header = parseRow(lines.get(index));
        Row separator = parseRow(lines.get(index + 1));

        if (header == null || separator == null || pipeCount(lines.get(index)) < 2) {
            return false;
        }

        if (indentLevel(header.prefix()) != indentLevel(separator.prefix())) {
            return false;
        }

        return isSeparatorRow(separator.cells());
    }

    private static boolean isSeparatorRow(List<String> cells) {
        if (cells.isEmpty()) {
            return false;
        }

        for (String cell : cells) {
            String t = cell.trim();

            if (t.isEmpty()) {
                return false;
            }

            int dashes = 0;
            for (int i = 0; i < t.length(); i++) {
                char c = t.charAt(i);

                if (c == '-') {
                    dashes++;
                } else if (c != ':') {
                    return false;
                }
            }

            if (dashes == 0) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    private static Row parseRow(String line) {
        int firstPipe = indexOfCellPipe(line, 0);
        if (firstPipe < 0) {
            return null;
        }

        String prefix = line.substring(0, firstPipe);
        List<String> cells = new ArrayList<>();

        int cellStart = firstPipe + 1;
        int next = indexOfCellPipe(line, cellStart);

        while (next >= 0) {
            cells.add(line.substring(cellStart, next).trim());
            cellStart = next + 1;
            next = indexOfCellPipe(line, cellStart);
        }

        String tail = line.substring(cellStart).trim();
        if (!tail.isEmpty()) {
            cells.add(tail);
        }

        if (cells.isEmpty()) {
            return null;
        }

        return new Row(prefix, cells);
    }

    public static int pipeCount(String line) {
        int count = 0;
        int at = indexOfCellPipe(line, 0);

        while (at >= 0) {
            count++;
            at = indexOfCellPipe(line, at + 1);
        }

        return count;
    }

    public static int indexOfCellPipe(String line, int from) {
        int depth = 0;

        for (int i = Math.max(0, from); i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
            } else if (c == '|' && depth == 0) {
                return i;
            }
        }

        return -1;
    }

    public static int lastIndexOfCellPipe(String line) {
        int last = -1;
        int at = indexOfCellPipe(line, 0);

        while (at >= 0) {
            last = at;
            at = indexOfCellPipe(line, at + 1);
        }

        return last;
    }

    private static int indentLevel(String prefix) {
        int level = 0;
        int i = 0;

        while (i + 1 < prefix.length()) {
            if (prefix.charAt(i) == '>' && prefix.charAt(i + 1) == '>') {
                level++;
                i += 2;
            } else {
                i++;
            }
        }

        return level;
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        return List.of(LINE_SPLIT.split(text, -1));
    }

    private static List<String> splitSeparators(String text) {
        List<String> out = new ArrayList<>();
        Matcher matcher = LINE_SPLIT.matcher(text);

        while (matcher.find()) {
            out.add(matcher.group());
        }

        return out;
    }
}
