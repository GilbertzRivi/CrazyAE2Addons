package net.oktawia.crazyae2addons.items;

import appeng.api.config.FuzzyMode;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.stacks.AEItemKey;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.BuilderPatternHost;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BuilderPatternItem extends AEBaseItem implements IMenuItem {

    private BlockPos cornerA = null;
    private BlockPos cornerB = null;

    private BlockPos origin = null;
    private Direction originFacing = Direction.NORTH;
    private static final String SEP = "|";


    public BuilderPatternItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {

        if (!level.isClientSide() && p.isShiftKeyDown()) {
            MenuOpener.open(CrazyMenuRegistrar.BUILDER_PATTERN_MENU.get(), p, MenuLocators.forHand(p, hand));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, p.getItemInHand(hand));
        }

        ItemStack stack = p.getItemInHand(hand);

        if (!level.isClientSide() && cornerA != null && cornerB != null && origin != null) {
            BlockPos min = new BlockPos(
                    Math.min(cornerA.getX(), cornerB.getX()),
                    Math.min(cornerA.getY(), cornerB.getY()),
                    Math.min(cornerA.getZ(), cornerB.getZ())
            );
            BlockPos max = new BlockPos(
                    Math.max(cornerA.getX(), cornerB.getX()),
                    Math.max(cornerA.getY(), cornerB.getY()),
                    Math.max(cornerA.getZ(), cornerB.getZ())
            );

            Basis basis = Basis.forFacing(originFacing);

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
                        if (state.isAir()) continue;

                        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                        if (blockId == null) continue;

                        var itemKey = AEItemKey.of(state.getBlock().asItem());
                        if (itemKey.fuzzyEquals(AEItemKey.of(Blocks.AIR.asItem()), FuzzyMode.IGNORE_ALL)) continue;

                        StringBuilder fullId = new StringBuilder(blockId.toString());

                        if (!state.getValues().isEmpty()) {
                            fullId.append("[");
                            boolean first = true;
                            for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
                                if (!first) fullId.append(",");
                                fullId.append(entry.getKey().getName()).append("=").append(entry.getValue());
                                first = false;
                            }
                            fullId.append("]");
                        }

                        String key = fullId.toString();
                        if (!blockMap.containsKey(key)) {
                            blockMap.put(key, blockIdCounter++);
                        }
                        BlockPos targetLocal = worldToLocal(currentWorld, origin, basis);
                        pattern.append(moveCursorRelative(cursorLocal, targetLocal));
                        cursorLocal = targetLocal;
                        pattern.append("P(").append(blockMap.get(key)).append(")");
                    }
                }
            }

            StringBuilder header = new StringBuilder();
            for (Map.Entry<String, Integer> entry : blockMap.entrySet()) {
                header.append(entry.getValue()).append("(").append(entry.getKey()).append("),\n");
            }
            if (!header.isEmpty()) header.setLength(header.length() - 2);

            String finalCode = header + "\n||\n" + pattern;

            ProgramExpander.Result result = ProgramExpander.expand(finalCode);
            if (result.success) {
                String programId = UUID.randomUUID().toString();
                stack.getOrCreateTag().putBoolean("code", true);
                stack.getOrCreateTag().putString("program_id", programId);
                stack.getOrCreateTag().putInt("delay", 0);

                stack.getOrCreateTag().putString("srcFacing", originFacing.getName());

                saveProgramToFile(programId, finalCode, p.getServer());
                p.displayClientMessage(Component.literal("Saved pattern. Length: " + finalCode.length()), true);
            } else {
                p.displayClientMessage(Component.literal("Could not save this structure"), true);
            }

            cornerA = null;
            cornerB = null;
            origin = null;

            return InteractionResultHolder.success(stack);
        }

        return new InteractionResultHolder<>(
                InteractionResult.sidedSuccess(level.isClientSide()), p.getItemInHand(hand));
    }


    public static void saveProgramToFile(String id, String code, MinecraftServer server) {
        Path file = server.getWorldPath(new LevelResource("serverdata"))
                .resolve("autobuilder")
                .resolve(id);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, code, UTF_8);
        } catch (IOException e) {
            LogUtils.getLogger().info(e.toString());
        }
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

    private static class Basis {
        final int fx, fz;
        final int rx, rz;

        private Basis(int fx, int fz, int rx, int rz) {
            this.fx = fx; this.fz = fz; this.rx = rx; this.rz = rz;
        }

        static Basis forFacing(Direction f) {
            return switch (f) {
                case NORTH -> new Basis( 0, -1,  1,  0);
                case SOUTH -> new Basis( 0,  1, -1,  0);
                case EAST  -> new Basis( 1,  0,  0,  1);
                case WEST  -> new Basis(-1,  0,  0, -1);
                default    -> new Basis( 0, -1,  1,  0);
            };
        }
    }

    private static BlockPos worldToLocal(BlockPos worldPos, BlockPos origin, Basis b) {
        int dx = worldPos.getX() - origin.getX();
        int dy = worldPos.getY() - origin.getY();
        int dz = worldPos.getZ() - origin.getZ();

        int right   = dx * b.rx + dz * b.rz;
        int up      = dy;
        int forward = dx * b.fx + dz * b.fz;

        return new BlockPos(right, up, forward);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos clicked = context.getClickedPos();
        Player player = context.getPlayer();

        if (player != null && !player.isLocalPlayer()) {
            if (cornerA == null) {
                cornerA = clicked.immutable();
                player.displayClientMessage(Component.literal("Corner 1 set!"), true);
            } else if (cornerB == null) {
                cornerB = clicked.immutable();
                origin = clicked.immutable();
                originFacing = player.getDirection();
                player.displayClientMessage(Component.literal("Corner 2 set! (origin)"), true);
            } else {
                cornerA = clicked.immutable();
                cornerB = null;
                origin = null;
                player.displayClientMessage(Component.literal("Corner 1 set! (reset)"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new BuilderPatternHost(player, inventorySlot, stack);
    }

    public static void applyFlipHorizontalToItem(ItemStack stack, MinecraftServer server, @Nullable Player player) {
        applyFlipInPlace(stack, server, Axis.X, player, "Flipped horizontally");
    }

    public static void applyFlipVerticalToItem(ItemStack stack, MinecraftServer server, @Nullable Player player) {
        applyFlipInPlace(stack, server, Axis.Y, player, "Flipped vertically");
    }

    private enum Axis { X, Y }

    private static void applyFlipInPlace(ItemStack stack, MinecraftServer server, Axis axis, @Nullable Player player, String okMsg) {
        String full = BuilderPatternHost.loadProgramFromFile(stack, server);
        if (full.isEmpty()) {
            if (player != null) player.displayClientMessage(Component.literal("No program"), true);
            return;
        }
        int sepIdx = full.lastIndexOf(SEP);
        final String header, body;
        if (sepIdx >= 0) {
            header = full.substring(0, sepIdx);
            body   = full.substring(sepIdx + SEP.length());
        } else {
            header = "";
            body   = full;
        }

        String bodyOut = flipBodyInPlace(body, axis);
        String updated = (header.isEmpty() ? "" : header + SEP) + bodyOut;

        try {
            var tag = stack.getOrCreateTag();
            if (!tag.contains("program_id")) tag.putString("program_id", UUID.randomUUID().toString());
            String id = tag.getString("program_id");
            Path file = server.getWorldPath(new LevelResource("serverdata"))
                    .resolve("autobuilder").resolve(id);
            Files.createDirectories(file.getParent());
            Files.writeString(file, updated, UTF_8);
            tag.putBoolean("code", true);
            if (player != null) player.displayClientMessage(Component.literal(okMsg), true);
        } catch (Exception e) {
            LogUtils.getLogger().info(e.toString());
            if (player != null) player.displayClientMessage(Component.literal("Failed to save changes"), true);
        }
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
            String kind;
            String payload;
            BlockPos pos;
            Ev(String k, String p, BlockPos bp) { kind = k; payload = p; pos = bp; }
        }
        ArrayList<Ev> events = new ArrayList<>();
        BlockPos cursor = BlockPos.ZERO;
        int i = 0, n = s.length();

        while (i < n) {
            char c = s.charAt(i);
            if (c == 'H') { events.add(new Ev("H", "H", null)); cursor = BlockPos.ZERO; i++; continue; }
            if (c == 'Z' && i + 1 < n && s.charAt(i + 1) == '|') {
                int j = i + 2; while (j < n && Character.isDigit(s.charAt(j))) j++;
                events.add(new Ev("Z", s.substring(i, j), null)); i = j; continue;
            }
            if (c == 'P' && i + 1 < n && s.charAt(i + 1) == '(') {
                int j = i + 2; while (j < n && s.charAt(j) != ')') j++; if (j < n) j++;
                events.add(new Ev("ACT", s.substring(i, j), cursor)); i = j; continue;
            }
            if (c == 'P' && i + 1 < n && s.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < n) {
                    char cj = s.charAt(j);
                    if (cj == 'H' || cj == 'Z' || cj == 'P' || cj == 'F' || cj == 'B' || cj == 'L' || cj == 'R' || cj == 'U' || cj == 'D' || cj == 'X') break;
                    j++;
                }
                events.add(new Ev("ACT", s.substring(i, j), cursor)); i = j; continue;
            }
            if (c == 'X') { events.add(new Ev("ACT", "X", cursor)); i++; continue; }
            if ("FBLRUD".indexOf(c) >= 0) { cursor = stepCursor(cursor, c); i++; continue; }
            i++;
        }

        boolean hasAct = false;
        for (Ev ev : events) if ("ACT".equals(ev.kind)) { hasAct = true; break; }
        if (!hasAct) return applyMapSkippingTokens(s, axis == Axis.X ? BuilderPatternItem::mapFlipHorizontal : BuilderPatternItem::mapFlipVertical);

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Ev ev : events) if ("ACT".equals(ev.kind)) {
            minX = Math.min(minX, ev.pos.getX()); maxX = Math.max(maxX, ev.pos.getX());
            minY = Math.min(minY, ev.pos.getY()); maxY = Math.max(maxY, ev.pos.getY());
        }
        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;

        StringBuilder out = new StringBuilder(s.length() + 64);
        BlockPos outCursor = BlockPos.ZERO;

        for (Ev ev : events) {
            switch (ev.kind) {
                case "H" -> { out.append("H"); outCursor = BlockPos.ZERO; }
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
                int j = i + 2; while (j < s.length() && s.charAt(j) != ')') j++; j = Math.min(j + 1, s.length());
                out.append(s, i, j); i = j; continue;
            }
            if (c == 'Z' && i + 1 < s.length() && s.charAt(i + 1) == '|') {
                int j = i + 2; while (j < s.length() && Character.isDigit(s.charAt(j))) j++;
                out.append(s, i, j); i = j; continue;
            }
            if (c == 'P' && i + 1 < s.length() && s.charAt(i + 1) == '|') {
                int j = i + 2; while (j < s.length()) {
                    char cj = s.charAt(j);
                    if (cj == 'H' || cj == 'Z' || cj == 'P' || cj == 'F' || cj == 'B' || cj == 'L' || cj == 'R' || cj == 'U' || cj == 'D' || cj == 'X') break;
                    j++;
                }
                out.append(s, i, j); i = j; continue;
            }
            int mapped = mapper.applyAsInt(c);
            out.append((char) mapped);
            i++;
        }
        return out.toString();
    }

    public static void applyRotateCWToItem(ItemStack stack, MinecraftServer server, int times, Player player) {
        String full = BuilderPatternHost.loadProgramFromFile(stack, server);
        if (full.isEmpty()) return;
        int i = full.indexOf(SEP);
        String header = i >= 0 ? full.substring(0, i) : "";
        String body   = i >= 0 ? full.substring(i + SEP.length()) : full;
        String outBody = rotateBodyInPlace(body, times);
        String updated = (header.isEmpty() ? "" : header + SEP) + outBody;

        var tag = stack.getOrCreateTag();
        if (!tag.contains("program_id")) tag.putString("program_id", UUID.randomUUID().toString());
        String id = tag.getString("program_id");
        Path file = server.getWorldPath(new LevelResource("serverdata")).resolve("autobuilder").resolve(id);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, updated, UTF_8);
            tag.putBoolean("code", true);
        } catch (IOException ignored) {}
    }


    private static String rotateBodyInPlace(String s, int times) {
        class Ev {
            String kind;
            String payload;
            BlockPos pos;
            Ev(String k, String p, BlockPos bp) { kind = k; payload = p; pos = bp; }
        }
        ArrayList<Ev> events = new ArrayList<>();
        BlockPos cursor = BlockPos.ZERO;
        int i = 0, n = s.length();

        while (i < n) {
            char c = s.charAt(i);
            if (c == 'H') { events.add(new Ev("H", "H", null)); cursor = BlockPos.ZERO; i++; continue; }
            if (c == 'Z' && i + 1 < n && s.charAt(i + 1) == '|') {
                int j = i + 2; while (j < n && Character.isDigit(s.charAt(j))) j++;
                events.add(new Ev("Z", s.substring(i, j), null)); i = j; continue;
            }
            if (c == 'P' && i + 1 < n && s.charAt(i + 1) == '(') {
                int j = i + 2; while (j < n && s.charAt(j) != ')') j++; if (j < n) j++;
                events.add(new Ev("ACT", s.substring(i, j), cursor)); i = j; continue;
            }
            if (c == 'P' && i + 1 < n && s.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < n) {
                    char cj = s.charAt(j);
                    if (cj == 'H' || cj == 'Z' || cj == 'P' || cj == 'F' || cj == 'B' || cj == 'L' || cj == 'R' || cj == 'U' || cj == 'D' || cj == 'X') break;
                    j++;
                }
                events.add(new Ev("ACT", s.substring(i, j), cursor)); i = j; continue;
            }
            if (c == 'X') { events.add(new Ev("ACT", "X", cursor)); i++; continue; }
            if ("FBLRUD".indexOf(c) >= 0) { cursor = stepCursor(cursor, c); i++; continue; }
            i++;
        }

        boolean hasAct = false;
        for (Ev ev : events) if ("ACT".equals(ev.kind)) { hasAct = true; break; }
        if (!hasAct) return s;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (Ev ev : events) if ("ACT".equals(ev.kind)) {
            minX = Math.min(minX, ev.pos.getX()); maxX = Math.max(maxX, ev.pos.getX());
            minZ = Math.min(minZ, ev.pos.getZ()); maxZ = Math.max(maxZ, ev.pos.getZ());
        }
        double cx = (minX + maxX) / 2.0;
        double cz = (minZ + maxZ) / 2.0;

        times = ((times % 4) + 4) % 4;
        if (times == 0) return s;

        StringBuilder out = new StringBuilder(s.length() + 64);
        BlockPos outCursor = BlockPos.ZERO;

        for (Ev ev : events) {
            switch (ev.kind) {
                case "H" -> { out.append("H"); outCursor = BlockPos.ZERO; }
                case "Z" -> out.append(ev.payload);
                case "ACT" -> {
                    BlockPos p = ev.pos;
                    double rx = p.getX() - cx;
                    double rz = p.getZ() - cz;
                    double x = rx, z = rz;
                    for (int k = 0; k < times; k++) {
                        double tmp = x;
                        x = -z;
                        z = tmp;
                    }
                    int newX = (int)Math.round(x + cx);
                    int newZ = (int)Math.round(z + cz);
                    BlockPos target = new BlockPos(newX, p.getY(), newZ);
                    out.append(moveCursorRelative(outCursor, target));
                    outCursor = target;
                    out.append(ev.payload);
                }
            }
        }
        return out.toString();
    }


    private static int mapFlipHorizontal(int ch) {
        return switch (ch) { case 'L' -> 'R'; case 'R' -> 'L'; default -> ch; };
    }
    private static int mapFlipVertical(int ch) {
        return switch (ch) { case 'U' -> 'D'; case 'D' -> 'U'; default -> ch; };
    }
}
