package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.autobuilder.BuilderPatternHost;
import net.oktawia.crazyae2addons.util.ProgramExpander;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BuilderPatternItem extends AEBaseItem implements IMenuItem {

    private static final String SEP = "||";
    private static final String SEP_FORMATTED = "\n||\n";

    private static final String TAG_ROOT = "CrazyBuilderPattern";
    private static final String TAG_PROGRAM_ID = "ProgramId";
    private static final String TAG_SOURCE_FACING = "SourceFacing";

    private static final Map<UUID, SelectionState> SELECTIONS = new ConcurrentHashMap<>();

    public BuilderPatternItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {

        if (!level.isClientSide() && player.isShiftKeyDown()) {
            MenuOpener.open(CrazyMenuRegistrar.BUILDER_PATTERN_MENU.get(), player, MenuLocators.forHand(player, hand));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, player.getItemInHand(hand));
        }

        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            SelectionState selection = SELECTIONS.get(player.getUUID());
            if (selection != null && selection.cornerA != null && selection.cornerB != null && selection.origin != null) {
                BlockPos min = new BlockPos(
                        Math.min(selection.cornerA.getX(), selection.cornerB.getX()),
                        Math.min(selection.cornerA.getY(), selection.cornerB.getY()),
                        Math.min(selection.cornerA.getZ(), selection.cornerB.getZ())
                );
                BlockPos max = new BlockPos(
                        Math.max(selection.cornerA.getX(), selection.cornerB.getX()),
                        Math.max(selection.cornerA.getY(), selection.cornerB.getY()),
                        Math.max(selection.cornerA.getZ(), selection.cornerB.getZ())
                );

                Basis basis = Basis.forFacing(selection.originFacing);
                Map<String, Integer> blockMap = new LinkedHashMap<>();
                int blockIdCounter = 1;
                StringBuilder pattern = new StringBuilder();
                pattern.append("H");
                BlockPos cursorLocal = BlockPos.ZERO;

                for (int y = min.getY(); y <= max.getY(); y++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        for (int x = min.getX(); x <= max.getX(); x++) {
                            BlockPos currentWorld = new BlockPos(x, y, z);
                            BlockState state = level.getBlockState(currentWorld);
                            if (state.isAir()) {
                                continue;
                            }
                            if (state.getBlock().asItem() == Items.AIR) {
                                continue;
                            }

                            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                            if (blockId == null) {
                                continue;
                            }

                            StringBuilder fullId = new StringBuilder(blockId.toString());
                            if (!state.getValues().isEmpty()) {
                                fullId.append("[");
                                boolean first = true;
                                for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
                                    if (!first) {
                                        fullId.append(",");
                                    }
                                    fullId.append(entry.getKey().getName()).append("=").append(entry.getValue());
                                    first = false;
                                }
                                fullId.append("]");
                            }

                            String key = fullId.toString();
                            if (!blockMap.containsKey(key)) {
                                blockMap.put(key, blockIdCounter++);
                            }

                            BlockPos targetLocal = worldToLocal(currentWorld, selection.origin, basis);
                            pattern.append(moveCursorRelative(cursorLocal, targetLocal));
                            cursorLocal = targetLocal;
                            pattern.append("P(").append(blockMap.get(key)).append(")\n");
                        }
                    }
                }

                StringBuilder header = new StringBuilder();
                for (Map.Entry<String, Integer> entry : blockMap.entrySet()) {
                    header.append(entry.getValue()).append("(").append(entry.getKey()).append("),\n");
                }
                if (!header.isEmpty()) {
                    header.setLength(header.length() - 2);
                }

                String finalCode = header + SEP_FORMATTED + pattern;
                ProgramExpander.Result result = ProgramExpander.expand(finalCode);

                if (result.success) {
                    String programId = UUID.randomUUID().toString();
                    BuilderPatternHost.saveProgramToFile(programId, finalCode, player.getServer());

                    setProgramId(stack, programId);
                    setSourceFacing(stack, selection.originFacing);

                    player.displayClientMessage(
                            Component.translatable(LangDefs.PROGRAM_SAVED.getTranslationKey())
                                    .append(String.valueOf(finalCode.length())),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.translatable(LangDefs.PROGRAM_INVALID.getTranslationKey()),
                            true
                    );
                }

                SELECTIONS.remove(player.getUUID());
                return InteractionResultHolder.success(stack);
            }
        }

        return new InteractionResultHolder<>(
                InteractionResult.sidedSuccess(level.isClientSide()),
                player.getItemInHand(hand)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clicked = context.getClickedPos();
        Player player = context.getPlayer();

        if (player != null && !context.getLevel().isClientSide()) {
            SelectionState state = SELECTIONS.computeIfAbsent(player.getUUID(), id -> new SelectionState());

            if (state.cornerA == null) {
                state.cornerA = clicked.immutable();
                player.displayClientMessage(
                        Component.translatable(LangDefs.CORNER_SET_A.getTranslationKey()),
                        true
                );
            } else if (state.cornerB == null) {
                state.cornerB = clicked.immutable();
                state.origin = clicked.immutable();
                state.originFacing = player.getDirection();
                player.displayClientMessage(
                        Component.translatable(LangDefs.CORNER_SET_B.getTranslationKey()),
                        true
                );
            } else {
                SelectionState newState = new SelectionState();
                newState.cornerA = clicked.immutable();
                SELECTIONS.put(player.getUUID(), newState);

                player.displayClientMessage(
                        Component.translatable(LangDefs.CORNER_RESET.getTranslationKey()),
                        true
                );
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new BuilderPatternHost(player, stack, inventorySlot);
    }

    public static void applyFlipHorizontalToItem(ItemStack stack, MinecraftServer server, @Nullable Player player) {
        applyFlipInPlace(stack, server, Axis.X, player);
    }

    public static void applyFlipVerticalToItem(ItemStack stack, MinecraftServer server, @Nullable Player player) {
        applyFlipInPlace(stack, server, Axis.Y, player);
    }

    private enum Axis { X, Y }

    private static void applyFlipInPlace(ItemStack stack, MinecraftServer server, Axis axis, @Nullable Player player) {
        String full = BuilderPatternHost.loadProgramFromFile(stack, server);
        if (full.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable(LangDefs.PROGRAM_NO_CODE.getTranslationKey()), true);
            }
            return;
        }

        ProgramSections sections = splitProgramSections(full);
        String bodyOut = flipBodyInPlace(sections.body, axis);
        String updated = joinProgramSections(sections.header, bodyOut);

        String id = getProgramId(stack);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }

        BuilderPatternHost.saveProgramToFile(id, updated, server);
        setProgramId(stack, id);
    }

    public static void applyRotateCWToItem(ItemStack stack, MinecraftServer server, int times, @Nullable Player player) {
        String full = BuilderPatternHost.loadProgramFromFile(stack, server);
        if (full.isEmpty()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable(LangDefs.PROGRAM_NO_CODE.getTranslationKey()), true);
            }
            return;
        }

        ProgramSections sections = splitProgramSections(full);
        String outBody = rotateBodyInPlace(sections.body, times);
        String updated = joinProgramSections(sections.header, outBody);

        String id = getProgramId(stack);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }

        BuilderPatternHost.saveProgramToFile(id, updated, server);
        setProgramId(stack, id);
    }

    private static ProgramSections splitProgramSections(String full) {
        int idx = full.indexOf(SEP_FORMATTED);
        if (idx >= 0) {
            return new ProgramSections(
                    full.substring(0, idx),
                    full.substring(idx + SEP_FORMATTED.length())
            );
        }

        idx = full.indexOf(SEP);
        if (idx >= 0) {
            return new ProgramSections(
                    full.substring(0, idx),
                    full.substring(idx + SEP.length())
            );
        }

        return new ProgramSections("", full);
    }

    private static String joinProgramSections(String header, String body) {
        if (header == null || header.isEmpty()) {
            return body;
        }
        return header + SEP_FORMATTED + body;
    }

    private static String moveCursorRelative(BlockPos fromLocal, BlockPos toLocal) {
        StringBuilder moves = new StringBuilder();
        int dx = toLocal.getX() - fromLocal.getX();
        int dy = toLocal.getY() - fromLocal.getY();
        int dz = toLocal.getZ() - fromLocal.getZ();

        while (dx > 0) { moves.append("R"); dx--; }
        while (dx < 0) { moves.append("L"); dx++; }
        while (dy > 0) { moves.append("U"); dy--; }
        while (dy < 0) { moves.append("D"); dy++; }
        while (dz > 0) { moves.append("F"); dz--; }
        while (dz < 0) { moves.append("B"); dz++; }

        return moves.toString();
    }

    private record Basis(int fx, int fz, int rx, int rz) {

        static Basis forFacing(Direction f) {
                return switch (f) {
                    case NORTH -> new Basis(0, -1, 1, 0);
                    case SOUTH -> new Basis(0, 1, -1, 0);
                    case EAST -> new Basis(1, 0, 0, 1);
                    case WEST -> new Basis(-1, 0, 0, -1);
                    default -> new Basis(0, -1, 1, 0);
                };
            }
        }

    private static BlockPos worldToLocal(BlockPos worldPos, BlockPos origin, Basis b) {
        int dx = worldPos.getX() - origin.getX();
        int dy = worldPos.getY() - origin.getY();
        int dz = worldPos.getZ() - origin.getZ();

        int right = dx * b.rx + dz * b.rz;
        int forward = dx * b.fx + dz * b.fz;

        return new BlockPos(right, dy, forward);
    }

    private static BlockPos stepCursor(BlockPos cursor, char ch) {
        return switch (ch) {
            case 'F' -> cursor.offset(0, 0, 1);
            case 'B' -> cursor.offset(0, 0, -1);
            case 'R' -> cursor.offset(1, 0, 0);
            case 'L' -> cursor.offset(-1, 0, 0);
            case 'U' -> cursor.offset(0, 1, 0);
            case 'D' -> cursor.offset(0, -1, 0);
            default -> cursor;
        };
    }

    private static String flipBodyInPlace(String s, Axis axis) {
        class Ev {
            String kind, payload;
            BlockPos pos;
            Ev(String k, String p, BlockPos bp) { kind = k; payload = p; pos = bp; }
        }

        ArrayList<Ev> events = new ArrayList<>();
        BlockPos cursor = BlockPos.ZERO;
        int i = 0, n = s.length();

        while (i < n) {
            char c = s.charAt(i);
            if (c == 'H') {
                events.add(new Ev("H", "H", null));
                cursor = BlockPos.ZERO;
                i++;
                continue;
            }
            if (c == 'Z' && i + 1 < n && s.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < n && Character.isDigit(s.charAt(j))) j++;
                events.add(new Ev("Z", s.substring(i, j), null));
                i = j;
                continue;
            }
            if (c == 'P' && i + 1 < n && s.charAt(i + 1) == '(') {
                int j = i + 2;
                while (j < n && s.charAt(j) != ')') j++;
                if (j < n) j++;
                events.add(new Ev("ACT", s.substring(i, j), cursor));
                i = j;
                continue;
            }
            if (c == 'P' && i + 1 < n && s.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < n) {
                    char cj = s.charAt(j);
                    if ("HZPFBLRUDX".indexOf(cj) >= 0) break;
                    j++;
                }
                events.add(new Ev("ACT", s.substring(i, j), cursor));
                i = j;
                continue;
            }
            if (c == 'X') {
                events.add(new Ev("ACT", "X", cursor));
                i++;
                continue;
            }
            if ("FBLRUD".indexOf(c) >= 0) {
                cursor = stepCursor(cursor, c);
                i++;
                continue;
            }
            i++;
        }

        boolean hasAct = events.stream().anyMatch(e -> "ACT".equals(e.kind));
        if (!hasAct) {
            return applyMapSkippingTokens(s, axis == Axis.X ? BuilderPatternItem::mapFlipHorizontal : BuilderPatternItem::mapFlipVertical);
        }

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (Ev ev : events) {
            if ("ACT".equals(ev.kind)) {
                minX = Math.min(minX, ev.pos.getX());
                maxX = Math.max(maxX, ev.pos.getX());
                minY = Math.min(minY, ev.pos.getY());
                maxY = Math.max(maxY, ev.pos.getY());
            }
        }

        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;

        StringBuilder out = new StringBuilder(s.length() + 64);
        BlockPos outCursor = BlockPos.ZERO;

        for (Ev ev : events) {
            switch (ev.kind) {
                case "H" -> {
                    out.append("H");
                    outCursor = BlockPos.ZERO;
                }
                case "Z" -> out.append(ev.payload);
                case "ACT" -> {
                    BlockPos p = ev.pos;
                    BlockPos target = axis == Axis.X
                            ? new BlockPos((int) Math.round(2 * cx - p.getX()), p.getY(), p.getZ())
                            : new BlockPos(p.getX(), (int) Math.round(2 * cy - p.getY()), p.getZ());
                    out.append(moveCursorRelative(outCursor, target));
                    outCursor = target;
                    out.append(ev.payload);
                }
            }
        }

        return out.toString();
    }

    private static String applyMapSkippingTokens(String s, java.util.function.IntUnaryOperator mapper) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;

        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 'P' && i + 1 < s.length() && s.charAt(i + 1) == '(') {
                int j = i + 2;
                while (j < s.length() && s.charAt(j) != ')') j++;
                j = Math.min(j + 1, s.length());
                out.append(s, i, j);
                i = j;
                continue;
            }
            if (c == 'Z' && i + 1 < s.length() && s.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
                out.append(s, i, j);
                i = j;
                continue;
            }
            if (c == 'P' && i + 1 < s.length() && s.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < s.length()) {
                    char cj = s.charAt(j);
                    if ("HZPFBLRUDX".indexOf(cj) >= 0) break;
                    j++;
                }
                out.append(s, i, j);
                i = j;
                continue;
            }
            int mapped = mapper.applyAsInt(c);
            out.append((char) mapped);
            i++;
        }

        return out.toString();
    }

    private static String rotateBodyInPlace(String s, int times) {
        class Ev {
            String kind, payload;
            BlockPos pos;
            Ev(String k, String p, BlockPos bp) { kind = k; payload = p; pos = bp; }
        }

        ArrayList<Ev> events = new ArrayList<>();
        BlockPos cursor = BlockPos.ZERO;
        int i = 0, n = s.length();

        while (i < n) {
            char c = s.charAt(i);
            if (c == 'H') {
                events.add(new Ev("H", "H", null));
                cursor = BlockPos.ZERO;
                i++;
                continue;
            }
            if (c == 'Z' && i + 1 < n && s.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < n && Character.isDigit(s.charAt(j))) j++;
                events.add(new Ev("Z", s.substring(i, j), null));
                i = j;
                continue;
            }
            if (c == 'P' && i + 1 < n && s.charAt(i + 1) == '(') {
                int j = i + 2;
                while (j < n && s.charAt(j) != ')') j++;
                if (j < n) j++;
                events.add(new Ev("ACT", s.substring(i, j), cursor));
                i = j;
                continue;
            }
            if (c == 'P' && i + 1 < n && s.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < n) {
                    char cj = s.charAt(j);
                    if ("HZPFBLRUDX".indexOf(cj) >= 0) break;
                    j++;
                }
                events.add(new Ev("ACT", s.substring(i, j), cursor));
                i = j;
                continue;
            }
            if (c == 'X') {
                events.add(new Ev("ACT", "X", cursor));
                i++;
                continue;
            }
            if ("FBLRUD".indexOf(c) >= 0) {
                cursor = stepCursor(cursor, c);
                i++;
                continue;
            }
            i++;
        }

        boolean hasAct = events.stream().anyMatch(e -> "ACT".equals(e.kind));
        if (!hasAct) {
            return s;
        }

        times = ((times % 4) + 4) % 4;
        if (times == 0) {
            return s;
        }

        StringBuilder out = new StringBuilder(s.length() + 64);
        BlockPos outCursor = BlockPos.ZERO;

        for (Ev ev : events) {
            switch (ev.kind) {
                case "H" -> {
                    out.append("H");
                    outCursor = BlockPos.ZERO;
                }
                case "Z" -> out.append(ev.payload);
                case "ACT" -> {
                    BlockPos p = ev.pos;
                    int x = p.getX();
                    int y = p.getY();
                    int z = p.getZ();
                    int rx = x;
                    int rz = z;

                    switch (times) {
                        case 1 -> {
                            rx = -z;
                            rz = x;
                        }
                        case 2 -> {
                            rx = -x;
                            rz = -z;
                        }
                        case 3 -> {
                            rx = z;
                            rz = -x;
                        }
                    }

                    BlockPos target = new BlockPos(rx, y, rz);
                    out.append(moveCursorRelative(outCursor, target));
                    outCursor = target;
                    out.append(ev.payload);
                }
            }
        }

        return out.toString();
    }

    private static int mapFlipHorizontal(int ch) {
        return switch (ch) {
            case 'L' -> 'R';
            case 'R' -> 'L';
            default -> ch;
        };
    }

    private static int mapFlipVertical(int ch) {
        return switch (ch) {
            case 'U' -> 'D';
            case 'D' -> 'U';
            default -> ch;
        };
    }

    @Nullable
    public static String getProgramId(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        if (tag == null) {
            return null;
        }

        String id = tag.getString(TAG_PROGRAM_ID);
        return id.isEmpty() ? null : id;
    }

    @Nullable
    public static Direction getSourceFacing(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ROOT);
        if (tag == null || !tag.contains(TAG_SOURCE_FACING)) {
            return null;
        }
        return Direction.byName(tag.getString(TAG_SOURCE_FACING));
    }

    public static void setProgramId(ItemStack stack, @Nullable String programId) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        if (programId == null || programId.isEmpty()) {
            tag.remove(TAG_PROGRAM_ID);
        } else {
            tag.putString(TAG_PROGRAM_ID, programId);
        }
        cleanupRootTag(stack, tag);
    }

    public static void setSourceFacing(ItemStack stack, @Nullable Direction facing) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ROOT);
        if (facing == null) {
            tag.remove(TAG_SOURCE_FACING);
        } else {
            tag.putString(TAG_SOURCE_FACING, facing.getName());
        }
        cleanupRootTag(stack, tag);
    }

    private static void cleanupRootTag(ItemStack stack, CompoundTag root) {
        if (root.isEmpty()) {
            CompoundTag full = stack.getTag();
            if (full != null) {
                full.remove(TAG_ROOT);
                if (full.isEmpty()) {
                    stack.setTag(null);
                }
            }
        }
    }

    private record ProgramSections(String header, String body) {
    }

    private static final class SelectionState {
        private BlockPos cornerA;
        private BlockPos cornerB;
        private BlockPos origin;
        private Direction originFacing = Direction.NORTH;
    }
}