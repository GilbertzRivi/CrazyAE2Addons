package net.oktawia.crazyae2addons.logic.display;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;

import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.logic.display.keytypes.DisplayKeyCompatRegistry;
import net.oktawia.crazyae2addons.util.MathParser;

@OnlyIn(Dist.CLIENT)
public final class DisplayRenderData {

    public interface LineSeg {
    }

    public record TextSeg(Component c) implements LineSeg {
    }

    public record ItemIconSeg(ItemStack stack) implements LineSeg {
    }

    public record FluidIconSeg(FluidStack stack) implements LineSeg {
    }

    public interface RenderLine {
        float scaleMul();
    }

    public record StyledLine(List<LineSeg> segs, float scaleMul) implements RenderLine {
    }

    public record TableRow(List<List<LineSeg>> cells) {
    }

    public record TableBlock(List<TableRow> rows, int indentLevel, int[] align, float scaleMul) implements RenderLine {
    }

    public record RichTextWithColors(List<RenderLine> lines, @Nullable Integer backgroundColor) {
    }

    public record DrawEntry(RenderLine line, int tableRowsToDraw) {
    }

    public static final class BgBox {
        @Nullable
        public Integer v;
    }

    private record TableCells(String rowPrefix, List<String> cells) {
    }

    private record TableParseResult(TableBlock block, int endIndex) {
    }

    private record StructuralLine(String rawForInline, int indentLevel, boolean bullet, float scaleMul) {
    }

    private static final Pattern CLIENT_VAR_TOKEN = Pattern.compile(
            "&(d\\^(?:tag\\{[^}]*\\}|[a-z0-9_\\.:]+)(?:%\\d+[tsm])?@\\d+[tsm]|" +
                    "s\\^(?:tag\\{[^}]*\\}|[a-z0-9_\\.:]+)(?:%\\d+)?|" +
                    "i\\^[a-z0-9_.\\-]+(?::[a-z0-9_./\\-]+)+|" +
                    "(?![cb][0-9A-Fa-f]{6}\\b)(?!tag\\{)[A-Za-z0-9_]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CLIENT_STOCK_TOKEN = Pattern.compile("&s\\^([a-z0-9_\\.:]+)(?:%(\\d+))?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ICON_TOKEN = Pattern.compile("&i\\^([a-z0-9_.\\-]+(?::[a-z0-9_./\\-]+)+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LINE_SPLIT = Pattern.compile("&nl|\\r\\n|\\r|\\n");
    private static final Pattern MARKDOWN_SPAN = Pattern.compile("(\\*\\*|\\*|__|~~|`)(.+?)\\1");

    public static final float TABLE_HEADER_EXTRA_PX = 2f;

    public static final float TABLE_ROW_TOP_PAD_PX = 1f;

    private static final Pattern CLIENT_DYNAMIC_TOKEN = Pattern
            .compile("&d\\^([a-z0-9_\\.:]+)(?:%(\\d+[tsm]))?@(\\d+[tsm])", Pattern.CASE_INSENSITIVE);

    private DisplayRenderData() {
    }

    public static String resolveTokensClientSide(String rawInput, Map<String, String> variables) {
        String input = DisplayMacros.expand(rawInput);

        if (input.isEmpty()) {
            return "";
        }

        if (variables == null || variables.isEmpty()) {
            return evalMathExpressions(input);
        }

        String current = input;

        for (int depth = 0; depth < 8; depth++) {
            String next = resolveTokensClientSideOnce(current, variables);

            if (next.equals(current)) {
                current = next;
                break;
            }

            current = next;
        }

        return evalMathExpressions(current);
    }

    private static String resolveTokensClientSideOnce(String input, Map<String, String> variables) {
        StringBuilder sb = new StringBuilder();
        Matcher m = CLIENT_VAR_TOKEN.matcher(input);

        while (m.find()) {
            String key = m.group(1);
            String withAmp = "&" + key;

            String repl = null;

            if (key.regionMatches(true, 0, "s^", 0, 2)) {
                repl = resolveStockTokenClientSide(key, withAmp, variables);
            } else if (key.regionMatches(true, 0, "d^", 0, 2)) {
                repl = resolveDynamicTokenClientSide(key, withAmp, variables);
            }

            if (repl == null) {
                repl = variables.getOrDefault(key, withAmp);
            }

            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }

        m.appendTail(sb);
        return sb.toString();
    }

    @Nullable
    private static String resolveDynamicTokenClientSide(String key, String withAmp, Map<String, String> variables) {
        Matcher dm = CLIENT_DYNAMIC_TOKEN.matcher(withAmp);

        if (!dm.matches()) {
            return null;
        }

        String materialId = dm.group(1);
        String baseVal = variables.get(key);

        if (baseVal == null) {
            return null;
        }

        try {
            long amount = parseDisplayAmountToLong(baseVal);
            return formatMaterialAmountDefault(materialId, amount);
        } catch (NumberFormatException | ArithmeticException e) {
            CrazyAddons.LOGGER.debug("invalid dynamic amount in display token: {} = {}", withAmp, baseVal, e);
            return baseVal;
        }
    }

    private static long parseDisplayAmountToLong(String s) {
        if (s == null) {
            throw new NumberFormatException("null");
        }

        String t = s.trim();

        if (t.isEmpty()) {
            throw new NumberFormatException("empty");
        }

        return new BigDecimal(t)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    @Nullable
    private static String resolveStockTokenClientSide(String key, String withAmp, Map<String, String> variables) {
        Matcher sm = CLIENT_STOCK_TOKEN.matcher(withAmp);
        if (!sm.matches()) {
            return null;
        }

        String materialId = sm.group(1);
        String powStr = sm.group(2);

        String baseVal = variables.get("s^" + materialId);
        if (baseVal == null) {
            return variables.get(key);
        }

        try {
            long amount = Long.parseLong(baseVal);

            if (powStr != null) {
                return formatAmountWithExplicitDivisor(amount, powStr);
            }

            return formatMaterialAmountDefault(materialId, amount);
        } catch (NumberFormatException e) {
            CrazyAddons.LOGGER.debug("invalid stock amount in display token: {}", withAmp, e);
            return variables.get(key);
        }
    }

    private static String formatAmountWithExplicitDivisor(long amount, String powStr) {
        try {
            int pow = Math.min(18, Math.max(0, Integer.parseInt(powStr)));

            long divisor = 1L;
            for (int i = 0; i < pow; i++) {
                divisor *= 10L;
            }

            return String.valueOf(Math.round((double) amount / divisor));
        } catch (NumberFormatException e) {
            CrazyAddons.LOGGER.debug("invalid divisor in display token", e);
            return String.valueOf(amount);
        }
    }

    private static String formatMaterialAmountDefault(String materialId, long amount) {
        AEKey key = resolveMaterialKeyForFormatting(materialId);

        if (key == null) {
            return String.valueOf(amount);
        }

        if (key.getAmountPerUnit() > 1 || key.getUnitSymbol() != null) {
            return compactAmountUnit(key.formatAmount(amount, AmountFormat.FULL));
        }

        return String.valueOf(amount);
    }

    private static String compactAmountUnit(String s) {
        return s == null ? "" : s.replaceFirst("(?<=\\d)\\s+(?=\\D)", "");
    }

    @Nullable
    private static AEKey resolveMaterialKeyForFormatting(String id) {
        try {
            int colon = id.indexOf(':');

            if (colon > 0) {
                String prefix = id.substring(0, colon);
                String rest = id.substring(colon + 1);

                if (prefix.equals("item")) {
                    var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(rest)).orElse(null);
                    return item != null && item != Items.AIR ? AEItemKey.of(item) : null;
                }

                if (prefix.equals("fluid")) {
                    var fluid = BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(rest)).orElse(null);
                    return fluid != null && fluid != Fluids.EMPTY ? AEFluidKey.of(fluid) : null;
                }

                if (DisplayKeyCompatRegistry.hasPrefix(prefix)) {
                    return DisplayKeyCompatRegistry.resolve(prefix, rest);
                }
            }

            ResourceLocation rl = ResourceLocation.parse(id);

            var item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item != null && item != Items.AIR) {
                return AEItemKey.of(item);
            }

            var fluid = BuiltInRegistries.FLUID.getOptional(rl).orElse(null);
            if (fluid != null && fluid != Fluids.EMPTY) {
                return AEFluidKey.of(fluid);
            }
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to resolve material key for display amount: {}", id, e);
        }

        return null;
    }

    public static String evalMathExpressions(String s) {
        if (s == null || s.isEmpty() || !s.contains("&(")) {
            return s;
        }

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length();) {
            if (s.charAt(i) == '&' && i + 1 < s.length() && s.charAt(i + 1) == '(') {
                int start = i + 2;
                int depth = 0;
                int j = start;
                boolean found = false;

                for (; j < s.length(); j++) {
                    char c = s.charAt(j);
                    if (c == '(') {
                        depth++;
                    } else if (c == ')') {
                        if (depth == 0) {
                            found = true;
                            break;
                        }
                        depth--;
                    }
                }

                if (!found) {
                    out.append(s.charAt(i));
                    i++;
                    continue;
                }

                String inner = evalMathExpressions(s.substring(start, j));
                String repl;
                try {
                    double val = MathParser.parse(inner);
                    repl = formatMathResult(val, inner.contains("/"));
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

    private static String formatMathResult(double v, boolean roundedDivision) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "ERR";
        }

        BigDecimal bd = BigDecimal.valueOf(v);

        if (roundedDivision) {
            bd = bd.setScale(2, RoundingMode.HALF_UP);
        }

        bd = bd.setScale(2, RoundingMode.HALF_UP);

        if (bd.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }

        return bd.toPlainString();
    }

    public static RichTextWithColors parseStyledTextWithIcons(String rawText) {
        List<RenderLine> lines = new ArrayList<>();
        BgBox bg = new BgBox();
        String[] rawLines = LINE_SPLIT.split(rawText == null ? "" : rawText, -1);

        InlineParseState inlineState = new InlineParseState();

        for (int i = 0; i < rawLines.length; i++) {
            String rawLine0 = rawLines[i];

            if (!inlineState.hasOpenScope()) {
                TableParseResult tbl = tryParseTableBlock(rawLines, i, bg);
                if (tbl != null) {
                    lines.add(tbl.block());
                    i = tbl.endIndex();
                    continue;
                }
            }

            List<LineSeg> segs = new ArrayList<>();
            StructuralLine structural = preprocessStructuralLine(rawLine0);

            if (structural.indentLevel() > 0) {
                String indentVisual = "|>".repeat(structural.indentLevel()) + " ";
                segs.add(new TextSeg(Component.literal(indentVisual).withStyle(Style.EMPTY.withColor(0x888888))));
            }

            if (structural.bullet()) {
                segs.add(new TextSeg(Component.literal(" • ").withStyle(Style.EMPTY.withColor(0xAAAAAA))));
            }

            parseInlineWithColors(structural.rawForInline(), Style.EMPTY, bg, segs, inlineState, i);
            lines.add(new StyledLine(segs, structural.scaleMul()));
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
        if (startIdx + 1 >= rawLines.length) {
            return null;
        }

        String headerRaw = rawLines[startIdx];
        String sepRaw = rawLines[startIdx + 1];

        int indent = countIndentMarkers(headerRaw);
        if (countIndentMarkers(sepRaw) != indent) {
            return null;
        }

        String header = stripIndentMarkers(headerRaw, indent);
        String sep = stripIndentMarkers(sepRaw, indent);
        if (header == null || sep == null) {
            return null;
        }
        if (!isMdTableRowCore(header) || !isMdTableSepCore(sep)) {
            return null;
        }

        int cols = Math.max(splitMdTableCells(header).cells().size(), splitMdTableCells(sep).cells().size());
        int[] align = parseSepAlign(sep, cols);

        List<TableRow> rows = new ArrayList<>();
        rows.add(parseOneTableRow(header, bg));
        int end = startIdx + 1;

        for (int i = startIdx + 2; i < rawLines.length; i++) {
            String rowRaw = rawLines[i];
            if (countIndentMarkers(rowRaw) != indent) {
                break;
            }

            String row = stripIndentMarkers(rowRaw, indent);
            if (row == null || !isMdTableRowCore(row)) {
                break;
            }

            rows.add(parseOneTableRow(row, bg));
            end = i;
        }

        return new TableParseResult(new TableBlock(rows, indent, align, 1.0f), end);
    }

    private static TableRow parseOneTableRow(String rowLine, BgBox bg) {
        TableCells tc = splitMdTableCells(rowLine);
        List<List<LineSeg>> cellSegs = new ArrayList<>(tc.cells().size());

        InlineParseState inlineState = new InlineParseState();

        for (int i = 0; i < tc.cells().size(); i++) {
            List<LineSeg> segs = new ArrayList<>();
            String cell = tc.cells().get(i);

            String txt = (i == 0 && !tc.rowPrefix().isEmpty())
                    ? tc.rowPrefix() + cell
                    : cell;

            parseInlineWithColors(txt, Style.EMPTY, bg, segs, inlineState, 0);
            cellSegs.add(segs);
        }

        return new TableRow(cellSegs);
    }

    private static boolean startsWithAt(String s, int idx, String prefix) {
        return idx >= 0 && idx + prefix.length() <= s.length() && s.startsWith(prefix, idx);
    }

    private static boolean isHex6(String s, int pos) {
        if (pos < 0 || pos + 6 > s.length()) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            char ch = Character.toUpperCase(s.charAt(pos + i));
            if (!((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTextColorTokenAt(String s, int idx) {
        return idx >= 0
                && idx + 8 <= s.length()
                && s.charAt(idx) == '&'
                && (s.charAt(idx + 1) == 'c' || s.charAt(idx + 1) == 'C')
                && isHex6(s, idx + 2);
    }

    private static boolean isBackgroundColorTokenAt(String s, int idx) {
        return idx >= 0
                && idx + 8 <= s.length()
                && s.charAt(idx) == '&'
                && (s.charAt(idx + 1) == 'b' || s.charAt(idx + 1) == 'B')
                && isHex6(s, idx + 2);
    }

    private static int parseHexColorAt(String s, int idx) {
        return Integer.parseInt(s.substring(idx + 2, idx + 8), 16);
    }

    private static void appendStyledChunk(String raw, int from, int to, Style style, List<LineSeg> out) {
        if (from >= to) {
            return;
        }
        String chunk = raw.substring(from, to);
        if (!chunk.isEmpty()) {
            appendTextAndIcons(chunk, style, out);
        }
    }

    private static void parseInlineWithColors(String raw,
            Style initialStyle,
            BgBox bg,
            List<LineSeg> out,
            InlineParseState state,
            int lineIndex) {
        if (raw == null || raw.isEmpty()) {
            return;
        }

        Style currentStyle = state.lineStartStyle(initialStyle);
        int plainStart = 0;
        int i = 0;

        while (i < raw.length()) {
            if (isBackgroundColorTokenAt(raw, i)) {
                appendStyledChunk(raw, plainStart, i, currentStyle, out);
                bg.v = parseHexColorAt(raw, i);
                i += 8;
                plainStart = i;
                continue;
            }

            if (isTextColorTokenAt(raw, i)) {
                appendStyledChunk(raw, plainStart, i, currentStyle, out);
                int rgb = parseHexColorAt(raw, i);

                if (i + 8 < raw.length() && raw.charAt(i + 8) == '(') {
                    state.openScopedTextColor(currentStyle, rgb, lineIndex);
                    currentStyle = state.lineStartStyle(initialStyle);
                    i += 9;
                    plainStart = i;
                    continue;
                }

                currentStyle = currentStyle.withColor(rgb);
                i += 8;
                plainStart = i;
                continue;
            }

            char ch = raw.charAt(i);

            if (state.hasOpenScope()) {
                if (ch == '(') {
                    state.incrementParenDepth();
                    i++;
                    continue;
                }

                if (ch == ')') {
                    ScopedTextColorFrame top = state.peekFrame();
                    if (top != null && top.parenDepth == 0) {
                        appendStyledChunk(raw, plainStart, i, currentStyle, out);
                        ScopedTextColorFrame closed = state.popClosedFrame();
                        currentStyle = state.restoreStyleAfterClose(initialStyle, lineIndex, closed);
                        i++;
                        plainStart = i;
                        continue;
                    } else if (top != null) {
                        top.parenDepth--;
                    }
                }
            }

            i++;
        }

        appendStyledChunk(raw, plainStart, raw.length(), currentStyle, out);
    }

    private static void appendTextAndIcons(String text, Style baseStyle, List<LineSeg> out) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (!CrazyConfig.COMMON.DISPLAY_ICONS_ENABLED.get()) {
            out.add(new TextSeg(parseMarkdownSegment(text, baseStyle)));
            return;
        }

        Matcher im = ICON_TOKEN.matcher(text);
        int last = 0;

        while (im.find()) {
            if (im.start() > last) {
                String chunk = text.substring(last, im.start());
                if (!chunk.isEmpty()) {
                    out.add(new TextSeg(parseMarkdownSegment(chunk, baseStyle)));
                }
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
            if (!tail.isEmpty()) {
                out.add(new TextSeg(parseMarkdownSegment(tail, baseStyle)));
            }
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
                    return (item != null && item != Items.AIR)
                            ? new ItemIconSeg(new ItemStack(item))
                            : null;
                }

                if (prefix.equals("fluid")) {
                    var fluid = BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(rest)).orElse(null);
                    return (fluid != null && fluid != Fluids.EMPTY)
                            ? new FluidIconSeg(new FluidStack(fluid, 1000))
                            : null;
                }

                if (DisplayKeyCompatRegistry.hasPrefix(prefix)) {
                    ItemStack stack = DisplayKeyCompatRegistry.getIcon(prefix, rest);
                    return (stack != null && !stack.isEmpty()) ? new ItemIconSeg(stack) : null;
                }
            }

            ResourceLocation rl = ResourceLocation.parse(id);

            var item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
            if (item != null && item != Items.AIR) {
                return new ItemIconSeg(new ItemStack(item));
            }

            var block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
            if (block != null && block != Blocks.AIR && block.asItem() != Items.AIR) {
                return new ItemIconSeg(new ItemStack(block));
            }

            var fluid = BuiltInRegistries.FLUID.getOptional(rl).orElse(null);
            if (fluid != null && fluid != Fluids.EMPTY) {
                return new FluidIconSeg(new FluidStack(fluid, 1000));
            }
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to create icon segment", e);
        }

        return null;
    }

    private static Component parseMarkdownSegment(String text, Style baseStyle) {
        Matcher matcher = MARKDOWN_SPAN.matcher(text);
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
                case "*" -> baseStyle.withItalic(true);
                case "__" -> baseStyle.withUnderlined(true);
                case "~~" -> baseStyle.withStrikethrough(true);
                default -> baseStyle;
            };

            result.append(parseMarkdownSegment(content, newStyle));
            last = matcher.end();
        }

        if (last < text.length()) {
            result.append(Component.literal(text.substring(last)).withStyle(baseStyle));
        }

        return result;
    }

    private static int consumeLeadingColorTokensAndSpaces(String s, int start,
            @Nullable StringBuilder collectedColors) {
        int i = Math.max(0, start);

        while (true) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }

            if (isBackgroundColorTokenAt(s, i) || isTextColorTokenAt(s, i)) {
                if (collectedColors != null) {
                    collectedColors.append(s, i, i + 8);
                }
                i += 8;
                continue;
            }

            break;
        }

        return i;
    }

    private static String stripLeadingStructurePreamble(String s) {
        String t = s == null ? "" : s;
        int i = consumeLeadingColorTokensAndSpaces(t, 0, null);
        return t.substring(Math.min(i, t.length()));
    }

    private static StructuralLine preprocessStructuralLine(String rawLine) {
        if (rawLine == null || rawLine.isEmpty()) {
            return new StructuralLine("", 0, false, 1.0f);
        }

        StringBuilder keptColors = new StringBuilder();
        int cursor = consumeLeadingColorTokensAndSpaces(rawLine, 0, keptColors);

        int indentLevel = 0;
        boolean changed = false;

        while (startsWithAt(rawLine, cursor, ">>")) {
            changed = true;
            indentLevel++;
            cursor += 2;
            cursor = consumeLeadingColorTokensAndSpaces(rawLine, cursor, keptColors);
        }

        float scaleMul = 1.0f;
        boolean bullet = false;

        int headingStart = cursor;
        while (cursor < rawLine.length() && rawLine.charAt(cursor) == '#') {
            cursor++;
        }

        if (cursor > headingStart) {
            changed = true;
            int level = Math.min(6, cursor - headingStart);

            while (cursor < rawLine.length() && Character.isWhitespace(rawLine.charAt(cursor))) {
                cursor++;
            }
            cursor = consumeLeadingColorTokensAndSpaces(rawLine, cursor, keptColors);

            scaleMul = headingScaleMul(level);
        } else if (cursor + 1 < rawLine.length()
                && (rawLine.charAt(cursor) == '*' || rawLine.charAt(cursor) == '-')
                && Character.isWhitespace(rawLine.charAt(cursor + 1))) {
            changed = true;
            bullet = true;
            cursor += 2;
            cursor = consumeLeadingColorTokensAndSpaces(rawLine, cursor, keptColors);
        }

        if (!changed) {
            return new StructuralLine(rawLine, 0, false, 1.0f);
        }

        return new StructuralLine(keptColors + rawLine.substring(cursor), indentLevel, bullet, scaleMul);
    }

    private static int countIndentMarkers(String s) {
        String t = s == null ? "" : s;
        StringBuilder keptColors = new StringBuilder();
        int i = consumeLeadingColorTokensAndSpaces(t, 0, keptColors);

        int ind = 0;
        while (startsWithAt(t, i, ">>")) {
            ind++;
            i += 2;
            i = consumeLeadingColorTokensAndSpaces(t, i, keptColors);
        }

        return ind;
    }

    @Nullable
    private static String stripIndentMarkers(String s, int indentLevel) {
        String t = s == null ? "" : s;
        StringBuilder keptColors = new StringBuilder();
        int i = consumeLeadingColorTokensAndSpaces(t, 0, keptColors);

        for (int n = 0; n < indentLevel; n++) {
            if (!startsWithAt(t, i, ">>")) {
                return null;
            }
            i += 2;
            i = consumeLeadingColorTokensAndSpaces(t, i, keptColors);
        }

        return keptColors + t.substring(i);
    }

    private static boolean isMdTableRowCore(String s) {
        if (s == null) {
            return false;
        }

        String t = stripLeadingStructurePreamble(s).trim();
        return DisplayTableFormatter.pipeCount(t) >= 2;
    }

    private static boolean isMdTableSepCore(String s) {
        if (s == null) {
            return false;
        }

        String t = stripLeadingStructurePreamble(s).trim();
        if (t.isEmpty()) {
            return false;
        }

        boolean hasDash = false;
        int pipes = 0;

        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '|') {
                pipes++;
            } else if (c == '-') {
                hasDash = true;
            } else if (c != ':' && c != ' ' && c != '\t') {
                return false;
            }
        }

        return pipes >= 1 && hasDash;
    }

    private static TableCells splitMdTableCells(String line) {
        String t = line == null ? "" : line.trim();
        int firstPipe = DisplayTableFormatter.indexOfCellPipe(t, 0);
        if (firstPipe < 0) {
            return new TableCells("", List.of(t));
        }

        int lastPipe = DisplayTableFormatter.lastIndexOfCellPipe(t);
        boolean closed = lastPipe > firstPipe;

        String rowPrefix = t.substring(0, firstPipe);
        String rowSuffix = closed && lastPipe + 1 < t.length() ? t.substring(lastPipe + 1) : "";

        String middle = closed ? t.substring(firstPipe + 1, lastPipe) : t.substring(firstPipe + 1);

        List<String> out = new ArrayList<>();
        int cellStart = 0;
        int next = DisplayTableFormatter.indexOfCellPipe(middle, 0);

        while (next >= 0) {
            out.add(middle.substring(cellStart, next).trim());
            cellStart = next + 1;
            next = DisplayTableFormatter.indexOfCellPipe(middle, cellStart);
        }

        out.add(middle.substring(cellStart).trim());

        if (!out.isEmpty() && !rowSuffix.isEmpty()) {
            int last = out.size() - 1;
            out.set(last, out.get(last) + rowSuffix);
        }

        return new TableCells(rowPrefix, out);
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

    public record TableLayout(int cols, int[] colContentW, int padPx, int barW, int prefixW, String indentText,
            int totalW) {
    }

    public static int iconAdvancePx(Font font) {
        return font.lineHeight + 1;
    }

    public static int segsWidthPx(Font font, List<LineSeg> segs) {
        int w = 0;
        int iconW = iconAdvancePx(font);

        for (LineSeg s : segs) {
            if (s instanceof TextSeg ts) {
                w += font.width(ts.c());
            } else if (s instanceof ItemIconSeg || s instanceof FluidIconSeg) {
                w += iconW;
            }
        }

        return Math.max(1, w);
    }

    public static float lineWidthPx(Font font, StyledLine line) {
        return segsWidthPx(font, line.segs()) * line.scaleMul();
    }

    public static TableLayout computeTableLayout(Font font, TableBlock tb) {
        int cols = 0;
        for (TableRow r : tb.rows()) {
            cols = Math.max(cols, r.cells().size());
        }

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
        for (int w : colW) {
            sumCols += w;
        }
        int total = prefixW + (barW * (cols + 1)) + sumCols + (pad * 2 * cols);

        return new TableLayout(cols, colW, pad, barW, prefixW, indentText, total);
    }

    public static float renderLineWidthPx(Font font, RenderLine ln) {
        if (ln instanceof StyledLine sl) {
            return lineWidthPx(font, sl);
        }
        if (ln instanceof TableBlock tb) {
            return computeTableLayout(font, tb).totalW() * tb.scaleMul();
        }
        return 1f;
    }

    public static float renderLineHeightPx(Font font, RenderLine ln) {
        if (ln instanceof StyledLine sl) {
            return font.lineHeight * sl.scaleMul();
        }
        if (ln instanceof TableBlock tb) {
            return tableBlockHeightPx(font, tb, tb.rows().size());
        }
        return font.lineHeight;
    }

    public static float tableRowHeightPx(Font font) {
        return font.lineHeight + TABLE_ROW_TOP_PAD_PX;
    }

    public static float tableBlockHeightPx(Font font, TableBlock tb, int rows) {
        if (rows <= 0) {
            return 0f;
        }
        return (tableRowHeightPx(font) * rows + TABLE_HEADER_EXTRA_PX) * tb.scaleMul();
    }

    private static final class ScopedTextColorFrame {
        private final Style previousStyleSameLine;
        private final Style scopedStyle;
        private final int openedOnLine;
        private int parenDepth;

        private ScopedTextColorFrame(Style previousStyleSameLine, Style scopedStyle, int openedOnLine) {
            this.previousStyleSameLine = previousStyleSameLine;
            this.scopedStyle = scopedStyle;
            this.openedOnLine = openedOnLine;
            this.parenDepth = 0;
        }
    }

    private static final class InlineParseState {
        private final Deque<ScopedTextColorFrame> scopedTextColors = new ArrayDeque<>();

        private boolean hasOpenScope() {
            return !scopedTextColors.isEmpty();
        }

        private Style lineStartStyle(Style initialStyle) {
            return scopedTextColors.isEmpty() ? initialStyle : scopedTextColors.peek().scopedStyle;
        }

        private void openScopedTextColor(Style currentStyle, int rgb, int lineIndex) {
            scopedTextColors.push(new ScopedTextColorFrame(
                    currentStyle,
                    currentStyle.withColor(rgb),
                    lineIndex));
        }

        private void incrementParenDepth() {
            if (!scopedTextColors.isEmpty()) {
                scopedTextColors.peek().parenDepth++;
            }
        }

        private Style restoreStyleAfterClose(Style initialStyle, int lineIndex, ScopedTextColorFrame closed) {
            if (closed.openedOnLine == lineIndex) {
                return closed.previousStyleSameLine;
            }
            return scopedTextColors.isEmpty() ? initialStyle : scopedTextColors.peek().scopedStyle;
        }

        private ScopedTextColorFrame popClosedFrame() {
            return scopedTextColors.pop();
        }

        private ScopedTextColorFrame peekFrame() {
            return scopedTextColors.peek();
        }
    }
}
