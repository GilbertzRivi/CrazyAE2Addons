package net.oktawia.crazyae2addons.logic.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.oktawia.crazyae2addons.logic.display.keytypes.DisplayKeyCompatRegistry;
import net.oktawia.crazyae2addons.misc.MathParser;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public final class DisplayRenderData {

    public interface LineSeg {}
    public record TextSeg(Component c) implements LineSeg {}
    public record ItemIconSeg(net.minecraft.world.item.ItemStack stack) implements LineSeg {}
    public record FluidIconSeg(FluidStack stack) implements LineSeg {}

    public interface RenderLine { float scaleMul(); }
    public record StyledLine(List<LineSeg> segs, float scaleMul) implements RenderLine {}
    public record TableRow(List<List<LineSeg>> cells) {}
    public record TableBlock(List<TableRow> rows, int indentLevel, int[] align, float scaleMul) implements RenderLine {}
    public record RichTextWithColors(List<RenderLine> lines, @Nullable Integer backgroundColor) {}
    public record DrawEntry(RenderLine line, int tableRowsToDraw) {}

    public static final class BgBox { @Nullable public Integer v; }
    private record TableCells(String rowPrefix, List<String> cells) {}
    private record TableParseResult(TableBlock block, int endIndex) {}

    private static final Pattern CLIENT_VAR_TOKEN = Pattern.compile(
            "&(d\\^[a-z0-9_\\.:]+(?:%\\d+[tsm])?@\\d+[tsm]|" +
            "s\\^[a-z0-9_\\.:]+(?:%\\d+)?|" +
            "i\\^[a-z0-9_.\\-]+(?::[a-z0-9_./\\-]+)+|" +
            "[A-Za-z0-9_]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CLIENT_STOCK_TOKEN = Pattern.compile("&s\\^([a-z0-9_\\.:]+)(?:%(\\d+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ICON_TOKEN = Pattern.compile("&i\\^([a-z0-9_.\\-]+(?::[a-z0-9_./\\-]+)+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE_SPLIT = Pattern.compile("(?:&nl|\\r\\n|\\r|\\n)");
    private static final Pattern COLOR_TOKEN = Pattern.compile("(&[cb])([0-9A-Fa-f]{6})");
    private static final Pattern LEADING_TABLE_PREFIX = Pattern.compile("^\\s*(?:&[cb][0-9A-Fa-f]{6}\\s*)+");

    private DisplayRenderData() {}

    public static String resolveTokensClientSide(String input, Map<String, String> variables) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        Matcher m = CLIENT_VAR_TOKEN.matcher(input);

        while (m.find()) {
            String key = m.group(1);
            String withAmp = "&" + key;
            String repl = variables.get(key);

            if (repl == null) {
                Matcher sm = CLIENT_STOCK_TOKEN.matcher(withAmp);
                if (sm.matches()) {
                    String itemId = sm.group(1);
                    String powStr = sm.group(2);
                    String baseVal = variables.get("s^" + itemId);
                    if (baseVal != null) {
                        try {
                            long amount = Long.parseLong(baseVal);
                            long divisor = 1L;
                            if (powStr != null) {
                                int pow = Integer.parseInt(powStr);
                                if (pow > 0) divisor = (long) Math.pow(10, pow);
                            }
                            repl = String.valueOf(Math.round((double) amount / divisor));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (repl == null) repl = variables.getOrDefault(key, withAmp);
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return evalMathExpressions(sb.toString());
    }

    public static String evalMathExpressions(String s) {
        if (s == null || s.isEmpty() || !s.contains("&(")) return s;

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            if (s.charAt(i) == '&' && i + 1 < s.length() && s.charAt(i + 1) == '(') {
                int start = i + 2;
                int depth = 0;
                int j = start;
                boolean found = false;
                for (; j < s.length(); j++) {
                    char c = s.charAt(j);
                    if (c == '(') depth++;
                    else if (c == ')') {
                        if (depth == 0) { found = true; break; }
                        depth--;
                    }
                }
                if (!found) { out.append(s.charAt(i)); i++; continue; }
                String inner = evalMathExpressions(s.substring(start, j));
                String repl;
                try {
                    double val = MathParser.parse(inner);
                    repl = formatMathResult(val);
                } catch (Throwable t) {
                    repl = "ERR";
                }
                out.append(repl);
                i = j + 1;
            } else {
                out.append(s.charAt(i));
                i++;
            }
        }
        return out.toString();
    }

    private static String formatMathResult(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "ERR";
        BigDecimal bd = BigDecimal.valueOf(v).stripTrailingZeros();
        if (bd.compareTo(BigDecimal.ZERO) == 0) return "0";
        return bd.toPlainString();
    }

    public static RichTextWithColors parseStyledTextWithIcons(String rawText) {
        List<RenderLine> lines = new ArrayList<>();
        BgBox bg = new BgBox();
        String[] rawLines = LINE_SPLIT.split(rawText == null ? "" : rawText, -1);

        for (int i = 0; i < rawLines.length; i++) {
            String rawLine0 = rawLines[i];

            TableParseResult tbl = tryParseTableBlock(rawLines, i, bg);
            if (tbl != null) {
                lines.add(tbl.block());
                i = tbl.endIndex();
                continue;
            }

            String rawLine = rawLine0;
            List<LineSeg> segs = new ArrayList<>();

            int indentLevel = 0;
            while (rawLine.startsWith(">>")) {
                indentLevel++;
                rawLine = rawLine.substring(2);
            }

            if (indentLevel > 0) {
                String indentVisual = "|>".repeat(indentLevel) + " ";
                segs.add(new TextSeg(Component.literal(indentVisual).withStyle(Style.EMPTY.withColor(0x888888))));
            }

            if (rawLine.matches("^[*-] .*")) {
                segs.add(new TextSeg(Component.literal(" \u2022 ").withStyle(Style.EMPTY.withColor(0xAAAAAA))));
                rawLine = rawLine.substring(2);
            }

            float lineScaleMul = 1.0f;
            int h = 0;
            while (h < rawLine.length() && rawLine.charAt(h) == '#') h++;
            if (h > 0) {
                int level = Math.min(6, h);
                int cut = h;
                if (cut < rawLine.length() && rawLine.charAt(cut) == ' ') cut++;
                rawLine = rawLine.substring(cut);
                lineScaleMul = headingScaleMul(level);
            }

            parseInlineWithColors(rawLine, Style.EMPTY, bg, segs);
            lines.add(new StyledLine(segs, lineScaleMul));
        }

        return new RichTextWithColors(lines, bg.v);
    }

    private static float headingScaleMul(int level) {
        return switch (level) {
            case 1 -> 1.60f;
            case 2 -> 1.35f;
            case 3 -> 1.20f;
            case 4 -> 1.10f;
            case 5 -> 1.00f;
            default -> 0.95f;
        };
    }

    @Nullable
    private static TableParseResult tryParseTableBlock(String[] rawLines, int startIdx, BgBox bg) {
        if (startIdx + 1 >= rawLines.length) return null;

        String headerRaw = rawLines[startIdx];
        String sepRaw = rawLines[startIdx + 1];

        int indent = countIndentMarkers(headerRaw);
        if (countIndentMarkers(sepRaw) != indent) return null;

        String header = stripIndentMarkers(headerRaw, indent);
        String sep = stripIndentMarkers(sepRaw, indent);
        if (header == null || sep == null) return null;
        if (!isMdTableRowCore(header) || !isMdTableSepCore(sep)) return null;

        int cols = Math.max(splitMdTableCells(header).cells().size(), splitMdTableCells(sep).cells().size());
        int[] align = parseSepAlign(sep, cols);

        List<TableRow> rows = new ArrayList<>();
        rows.add(parseOneTableRow(header, bg));
        int end = startIdx + 1;

        for (int i = startIdx + 2; i < rawLines.length; i++) {
            String rowRaw = rawLines[i];
            if (countIndentMarkers(rowRaw) != indent) break;
            String row = stripIndentMarkers(rowRaw, indent);
            if (row == null || !isMdTableRowCore(row)) break;
            rows.add(parseOneTableRow(row, bg));
            end = i;
        }

        return new TableParseResult(new TableBlock(rows, indent, align, 1.0f), end);
    }

    private static TableRow parseOneTableRow(String rowLine, BgBox bg) {
        TableCells tc = splitMdTableCells(rowLine);
        List<List<LineSeg>> cellSegs = new ArrayList<>(tc.cells().size());
        for (String cell : tc.cells()) {
            List<LineSeg> segs = new ArrayList<>();
            String txt = tc.rowPrefix().isEmpty() ? cell : (tc.rowPrefix() + cell);
            parseInlineWithColors(txt, Style.EMPTY, bg, segs);
            cellSegs.add(segs);
        }
        return new TableRow(cellSegs);
    }

    private static void parseInlineWithColors(String raw, Style initialStyle, BgBox bg, List<LineSeg> out) {
        if (raw == null || raw.isEmpty()) return;
        Style currentStyle = initialStyle;
        Matcher colorMatcher = COLOR_TOKEN.matcher(raw);
        int last = 0;

        while (colorMatcher.find()) {
            if (colorMatcher.start() > last) {
                appendTextAndIcons(raw.substring(last, colorMatcher.start()), currentStyle, out);
            }
            String type = colorMatcher.group(1);
            int color = Integer.parseInt(colorMatcher.group(2), 16);
            if (type.equalsIgnoreCase("&c")) {
                currentStyle = currentStyle.withColor(color);
            } else {
                bg.v = color;
            }
            last = colorMatcher.end();
        }

        if (last < raw.length()) {
            appendTextAndIcons(raw.substring(last), currentStyle, out);
        }
    }

    private static void appendTextAndIcons(String text, Style baseStyle, List<LineSeg> out) {
        if (text == null || text.isEmpty()) return;
        Matcher im = ICON_TOKEN.matcher(text);
        int last = 0;

        while (im.find()) {
            if (im.start() > last) {
                String chunk = text.substring(last, im.start());
                if (!chunk.isEmpty()) out.add(new TextSeg(parseMarkdownSegment(chunk, baseStyle)));
            }
            LineSeg seg = makeIconSeg(im.group(1));
            if (seg != null) {
                out.add(seg);
            } else {
                out.add(new TextSeg(Component.literal(text.substring(im.start(), im.end())).withStyle(baseStyle)));
            }
            last = im.end();
        }

        if (last < text.length()) {
            String tail = text.substring(last);
            if (!tail.isEmpty()) out.add(new TextSeg(parseMarkdownSegment(tail, baseStyle)));
        }
    }

    @Nullable
    private static LineSeg makeIconSeg(String id) {
        try {
            int colon = id.indexOf(':');
            if (colon > 0) {
                String prefix = id.substring(0, colon);
                String rest = id.substring(colon + 1);
                if (prefix.equals("item")) {
                    var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(rest)).orElse(null);
                    return (item != null && item != Items.AIR) ? new ItemIconSeg(new net.minecraft.world.item.ItemStack(item)) : null;
                }
                if (prefix.equals("fluid")) {
                    var fluid = BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(rest)).orElse(null);
                    return (fluid != null && fluid != Fluids.EMPTY) ? new FluidIconSeg(new FluidStack(fluid, 1000)) : null;
                }
                if (DisplayKeyCompatRegistry.hasPrefix(prefix)) {
                    var stack = DisplayKeyCompatRegistry.getIcon(prefix, rest);
                    return (stack != null && !stack.isEmpty()) ? new ItemIconSeg(stack) : null;
                }
            }
            var rl = ResourceLocation.parse(id);
            var item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item != null && item != Items.AIR) return new ItemIconSeg(new net.minecraft.world.item.ItemStack(item));
            var block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
            if (block != null && block != Blocks.AIR && block.asItem() != Items.AIR)
                return new ItemIconSeg(new net.minecraft.world.item.ItemStack(block));
            var fluid = BuiltInRegistries.FLUID.getOptional(rl).orElse(null);
            if (fluid != null && fluid != Fluids.EMPTY) return new FluidIconSeg(new FluidStack(fluid, 1000));
        } catch (Throwable ignored) {}
        return null;
    }

    private static Component parseMarkdownSegment(String text, Style baseStyle) {
        Pattern pattern = Pattern.compile("(\\*\\*|\\*|__|~~|`)(.+?)\\1");
        Matcher matcher = pattern.matcher(text);
        MutableComponent result = Component.empty();
        int last = 0;

        while (matcher.find()) {
            if (matcher.start() > last) {
                result.append(Component.literal(text.substring(last, matcher.start())).withStyle(baseStyle));
            }
            String tag = matcher.group(1);
            String content = matcher.group(2);
            Style newStyle = switch (tag) {
                case "**" -> baseStyle.withBold(true);
                case "*"  -> baseStyle.withItalic(true);
                case "__" -> baseStyle.withUnderlined(true);
                case "~~" -> baseStyle.withStrikethrough(true);
                default   -> baseStyle;
            };
            result.append(parseMarkdownSegment(content, newStyle));
            last = matcher.end();
        }

        if (last < text.length()) {
            result.append(Component.literal(text.substring(last)).withStyle(baseStyle));
        }
        return result;
    }

    private static int countIndentMarkers(String s) {
        int ind = 0;
        String t = s == null ? "" : s;
        while (t.startsWith(">>")) { ind++; t = t.substring(2); }
        return ind;
    }

    @Nullable
    private static String stripIndentMarkers(String s, int indentLevel) {
        String t = s == null ? "" : s;
        for (int i = 0; i < indentLevel; i++) {
            if (!t.startsWith(">>")) return null;
            t = t.substring(2);
        }
        return t.stripLeading();
    }

    private static boolean isMdTableRowCore(String s) {
        if (s == null) return false;
        String t = s.trim();
        int pipes = 0;
        for (int i = 0; i < t.length(); i++) if (t.charAt(i) == '|') pipes++;
        return pipes >= 2;
    }

    private static boolean isMdTableSepCore(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;
        boolean hasDash = false;
        int pipes = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '|') pipes++;
            else if (c == '-') hasDash = true;
            else if (c != ':' && c != ' ' && c != '\t') return false;
        }
        return pipes >= 1 && hasDash;
    }

    private static TableCells splitMdTableCells(String line) {
        String t = (line == null) ? "" : line.trim();
        int firstPipe = t.indexOf('|');
        if (firstPipe < 0) return new TableCells("", List.of(t));

        String before = t.substring(0, firstPipe);
        String prefix = "";
        Matcher pm = LEADING_TABLE_PREFIX.matcher(before);
        if (pm.find()) prefix = pm.group(0).trim();

        t = t.substring(firstPipe);
        if (t.startsWith("|")) t = t.substring(1);
        if (t.endsWith("|")) t = t.substring(0, t.length() - 1);

        String[] parts = t.split("\\|", -1);
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) out.add(p.trim());
        return new TableCells(prefix, out);
    }

    private static int[] parseSepAlign(String sepLine, int cols) {
        int[] out = new int[Math.max(0, cols)];
        Arrays.fill(out, 1);
        List<String> sepCells = splitMdTableCells(sepLine).cells();
        for (int i = 0; i < Math.min(out.length, sepCells.size()); i++) {
            String cell = sepCells.get(i).trim();
            boolean left = cell.startsWith(":");
            boolean right = cell.endsWith(":");
            out[i] = (left && right) ? 1 : right ? 2 : 0;
        }
        return out;
    }

    public record TableLayout(int cols, int[] colContentW, int padPx, int barW, int prefixW, String indentText, int totalW) {}

    public static int iconAdvancePx(Font font) {
        return font.lineHeight + 1;
    }

    public static int segsWidthPx(Font font, List<LineSeg> segs) {
        int w = 0;
        int iconW = iconAdvancePx(font);
        for (LineSeg s : segs) {
            if (s instanceof TextSeg ts) w += font.width(ts.c());
            else if (s instanceof ItemIconSeg || s instanceof FluidIconSeg) w += iconW;
        }
        return Math.max(1, w);
    }

    public static float lineWidthPx(Font font, StyledLine line) {
        return segsWidthPx(font, line.segs()) * line.scaleMul();
    }

    public static TableLayout computeTableLayout(Font font, TableBlock tb) {
        int cols = 0;
        for (TableRow r : tb.rows()) cols = Math.max(cols, r.cells().size());

        int[] colW = new int[Math.max(0, cols)];
        for (TableRow r : tb.rows()) {
            for (int c = 0; c < r.cells().size(); c++) {
                List<LineSeg> cell = r.cells().get(c);
                int w = (cell == null || cell.isEmpty()) ? 0 : segsWidthPx(font, cell);
                colW[c] = Math.max(colW[c], w);
            }
        }

        int pad = 4;
        int barW = font.width("|");
        String indentText = tb.indentLevel() > 0 ? "|>".repeat(tb.indentLevel()) + " " : "";
        int prefixW = indentText.isEmpty() ? 0 : font.width(indentText);

        int sumCols = 0;
        for (int w : colW) sumCols += w;
        int total = prefixW + (barW * (cols + 1)) + sumCols + (pad * 2 * cols);

        return new TableLayout(cols, colW, pad, barW, prefixW, indentText, total);
    }

    public static float renderLineWidthPx(Font font, RenderLine ln) {
        if (ln instanceof StyledLine sl) return lineWidthPx(font, sl);
        if (ln instanceof TableBlock tb) return computeTableLayout(font, tb).totalW() * tb.scaleMul();
        return 1f;
    }

    public static float renderLineHeightPx(Font font, RenderLine ln) {
        if (ln instanceof StyledLine sl) return font.lineHeight * sl.scaleMul();
        if (ln instanceof TableBlock tb) return (font.lineHeight * tb.rows().size()) * tb.scaleMul();
        return font.lineHeight;
    }
}
