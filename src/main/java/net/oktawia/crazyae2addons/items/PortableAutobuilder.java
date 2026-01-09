package net.oktawia.crazyae2addons.items;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.ISubMenuHost;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.BuildScheduler;
import net.oktawia.crazyae2addons.logic.BuilderPatternHost;
import net.oktawia.crazyae2addons.logic.CopyGadgetHost;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PortableAutobuilder extends WirelessTerminalItem implements IMenuItem, IUpgradeableObject, ISubMenuHost {

    private BlockPos cornerA = null;
    private BlockPos cornerB = null;
    private BlockPos origin = null;
    private Direction originFacing = Direction.NORTH;
    private static final String SEP               = "|";

    public PortableAutobuilder(Properties props) {
        super(() -> 200_000, props.stacksTo(1));
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, 5, this::onUpgradesChanged);
    }

    private void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades) {
        setAEMaxPowerMultiplier(stack, 1 + upgrades.getInstalledUpgrades(AEItems.ENERGY_CARD));
    }

    public static Map<AEItemKey, Long> computeRequirements(Map<Integer, BlockState> palette, String body,
                                                            Map<AEItemKey, ResourceLocation> nameMapOut) {
        Map<AEItemKey, Long> needed = new LinkedHashMap<>();
        BlockPos cursor = BlockPos.ZERO;
        int i = 0, n = body.length();
        while (i < n) {
            char c = body.charAt(i);

            if (c == 'H') { cursor = BlockPos.ZERO; i++; continue; }
            if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'U' || c == 'D') {
                cursor = stepCursor(cursor, c); i++; continue;
            }
            if (c == 'P' && i + 1 < n && body.charAt(i + 1) == '(') {
                int j = i + 2;
                while (j < n && body.charAt(j) != ')') j++;
                if (j < n) {
                    String num = body.substring(i + 2, j);
                    try {
                        int id = Integer.parseInt(num);
                        BlockState st = palette.get(id);
                        if (st != null) {
                            var item = st.getBlock().asItem();
                            if (item != Items.AIR) {
                                AEItemKey key = AEItemKey.of(item);
                                needed.merge(key, 1L, Long::sum);
                                nameMapOut.putIfAbsent(key, ForgeRegistries.ITEMS.getKey(item));
                            }
                        }
                    } catch (NumberFormatException ignored) { }
                    i = j + 1; continue;
                }
            }
            i++;
        }
        return needed;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        if (p.isShiftKeyDown()) {
            MenuOpener.open(CrazyMenuRegistrar.COPY_GADGET_MENU.get(), p, MenuLocators.forHand(p, hand));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }

        if (cornerA != null && cornerB != null && origin != null) {
            generateProgramAndCopy(level, p, stack);
            return InteractionResultHolder.success(stack);
        }

        var tag = stack.getTag();
        if (tag != null && tag.getBoolean("code")) {
            BlockHitResult hit = rayTrace(level, p, 50.0D);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos placeOrigin = hit.getBlockPos().relative(hit.getDirection());
                pasteNow(level, p, stack, placeOrigin, p.getDirection());
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.success(stack);
    }


    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null || player.isLocalPlayer()) return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();

        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BlockPos placeOrigin = clicked.relative(ctx.getClickedFace());
                pasteNow(level, player, stack, placeOrigin, player.getDirection());
            }
            return InteractionResult.SUCCESS;
        } else {
            if (cornerA == null) {
                cornerA = clicked.immutable();
                CompoundTag tag = stack.getOrCreateTag();
                tag.putIntArray("selA", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
                player.displayClientMessage(Component.translatable("gui.crazyae2addons.builder_corner_1"), true);
            } else if (cornerB == null) {
                cornerB = clicked.immutable();
                origin = clicked.immutable();
                originFacing = player.getDirection();
                stack.getOrCreateTag().remove("selA");
                player.displayClientMessage(Component.translatable("gui.crazyae2addons.builder_corner_2"), true);
            } else {
                cornerA = clicked.immutable();
                cornerB = null;
                origin = null;
                CompoundTag tag = stack.getOrCreateTag();
                tag.putIntArray("selA", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
                player.displayClientMessage(Component.translatable("gui.crazyae2addons.builder_corner_3"), true);
            }
            return InteractionResult.SUCCESS;
        }
    }


    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new CopyGadgetHost(player, inventorySlot, stack);
    }

    public static double calcStepCostFE(BlockPos from, BlockPos target) {
        double dx = target.getX() - from.getX();
        double dy = target.getY() - from.getY();
        double dz = target.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance * CrazyConfig.COMMON.PortableSpatialStorageCostMult.get();
    }

    private void generateProgramAndCopy(Level level, Player p, ItemStack stack) {
        if (cornerA == null || cornerB == null || origin == null) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.copy_paste_select_corners"), true);
            return;
        }

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

        double required = 0.0;
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos wp = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(wp);
                    if (state.isAir()) continue;

                    ResourceLocation blockIdRL = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (blockIdRL == null) continue;

                    var itemKey = AEItemKey.of(state.getBlock().asItem());
                    if (itemKey.fuzzyEquals(AEItemKey.of(Blocks.AIR.asItem()), FuzzyMode.IGNORE_ALL)) continue;

                    required += calcStepCostFE(origin, wp);
                }
            }
        }
        int needFE = (int) Math.ceil(required);
        if (!this.usePower(p, needFE/2.d, stack)) {
            p.displayClientMessage(Component.literal("Not enough energy (" + needFE + " FE) to copy."), true);
            return;
        }

        Basis basis = Basis.forFacing(originFacing);
        Map<String, Integer> blockMap = new LinkedHashMap<>();
        int blockIdCounter = 1;

        StringBuilder pattern = new StringBuilder("H");
        BlockPos cursorLocal = BlockPos.ZERO;

        int copyCount = 0;
        List<BlockPos> previewPositions = new ArrayList<>();
        List<Integer> previewIndices = new ArrayList<>();

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos wp = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(wp);
                    if (state.isAir()) continue;

                    ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (blockId == null) continue;

                    var itemKey = AEItemKey.of(state.getBlock().asItem());
                    if (itemKey.fuzzyEquals(AEItemKey.of(Blocks.AIR.asItem()), FuzzyMode.IGNORE_ALL)) continue;

                    StringBuilder fullId = new StringBuilder(blockId.toString());
                    if (!state.getValues().isEmpty()) {
                        fullId.append("[");
                        boolean first = true;
                        for (Map.Entry<Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
                            if (!first) fullId.append(",");
                            fullId.append(e.getKey().getName()).append("=").append(e.getValue());
                            first = false;
                        }
                        fullId.append("]");
                    }

                    String key = fullId.toString();
                    if (!blockMap.containsKey(key)) blockMap.put(key, blockIdCounter++);

                    BlockPos targetLocal = worldToLocal(wp, origin, basis);
                    pattern.append(moveCursorRelative(cursorLocal, targetLocal));
                    cursorLocal = targetLocal;
                    pattern.append("P(").append(blockMap.get(key)).append(")");

                    previewPositions.add(targetLocal);
                    previewIndices.add(blockMap.get(key) - 1);

                    copyCount++;
                }
            }
        }

        StringBuilder header = new StringBuilder();
        for (Map.Entry<String, Integer> e : blockMap.entrySet()) {
            header.append(e.getValue()).append("(").append(e.getKey()).append("),\n");
        }
        if (!header.isEmpty()) header.setLength(header.length() - 2);

        String finalCode = header + "\n" + SEP + SEP + "\n" + pattern;

        ProgramExpander.Result result = ProgramExpander.expand(finalCode);
        if (!result.success) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.error_saving"), true);
            return;
        }

        String programId = UUID.randomUUID().toString();
        var tag = stack.getOrCreateTag();
        tag.putBoolean("code", true);
        tag.putString("program_id", programId);
        tag.putInt("delay", 0);
        tag.putString("src_facing", originFacing.getName());

        var palList = new net.minecraft.nbt.ListTag();
        String[] idByIndex = new String[blockMap.size()];
        for (Map.Entry<String, Integer> e : blockMap.entrySet()) {
            idByIndex[e.getValue() - 1] = e.getKey();
        }
        for (String s : idByIndex) palList.add(net.minecraft.nbt.StringTag.valueOf(s));
        tag.put("preview_palette", palList);
        tag.putIntArray("preview_indices", previewIndices.stream().mapToInt(Integer::intValue).toArray());

        int[] posArr = new int[previewPositions.size() * 3];
        int k = 0;
        for (BlockPos lp : previewPositions) {
            posArr[k++] = lp.getX();
            posArr[k++] = lp.getY();
            posArr[k++] = lp.getZ();
        }
        tag.putIntArray("preview_positions", posArr);

        saveProgramToFile(programId, finalCode, p.getServer());
        p.displayClientMessage(Component.literal("Copied structure (" + copyCount + " blocks)"), true);

        cornerA = cornerB = origin = null;
    }

    private boolean checkAndExtractFromME(ItemStack stack, Level level, Player p,
                                          Map<AEItemKey, Long> needed,
                                          Map<AEItemKey, ResourceLocation> nameMap) {
        var grid = getLinkedGrid(stack, level, p);
        if (grid == null) {
            return false;
        }
        if (p.isCreative()) return true;

        IStorageService ss = grid.getStorageService();
        MEStorage inv = ss.getInventory();
        IActionSource src = IActionSource.ofPlayer(p);

        Map<AEItemKey, Long> missing = new LinkedHashMap<>();
        for (var e : needed.entrySet()) {
            AEItemKey key = e.getKey();
            long want = e.getValue();
            long can = inv.extract(key, want, Actionable.SIMULATE, src);
            if (can < want) {
                missing.put(key, want - can);
            }
        }

        if (!missing.isEmpty()) {
            StringBuilder sb = new StringBuilder("Missing: ");
            int shown = 0;
            for (var e : missing.entrySet()) {
                if (shown++ > 0) sb.append(", ");
                ResourceLocation rl = nameMap.getOrDefault(e.getKey(), new ResourceLocation("minecraft", "unknown"));
                sb.append(rl.getPath()).append(" x").append(e.getValue());
                if (shown >= 3 && missing.size() > shown) { sb.append(", ..."); break; }
            }
            if (!level.isClientSide()) {
                p.displayClientMessage(Component.literal(sb.toString()), true);
            }
            return false;
        }

        for (var e : needed.entrySet()) {
            AEItemKey key = e.getKey();
            long want = e.getValue();
            long got = inv.extract(key, want, Actionable.MODULATE, src);
            if (got < want && !level.isClientSide()) {
                ResourceLocation rl = nameMap.getOrDefault(key, new ResourceLocation("minecraft", "unknown"));
                p.displayClientMessage(Component.literal("Couldn't extract: " + rl + " x" + (want - got)), true);
                return false;
            }
        }
        return true;
    }

    private void pasteNow(Level level, Player p, ItemStack stack, BlockPos originWorld, Direction pasteFacing) {
        if (!stack.hasTag() || !stack.getTag().getBoolean("code")) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.copy_paste_no_structure"), true);
            return;
        }

        String full = BuilderPatternHost.loadProgramFromFile(stack, p.getServer());
        if (full.isEmpty()) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.copy_paste_no_structure"), true);
            return;
        }

        int i = full.lastIndexOf(SEP);
        final String header = i >= 0 ? full.substring(0, i) : "";
        final String body   = i >= 0 ? full.substring(i + (SEP).length()) : full;

        Map<Integer, BlockState> palette = parseHeaderToPalette(header);
        if (palette.isEmpty()) {
            return;
        }

        Direction structureFacing = readSrcFacingFromNbt(stack);
        if (!structureFacing.getAxis().isHorizontal()) {
            structureFacing = Direction.NORTH;
        }

        Basis basis = Basis.forFacing(structureFacing);

        {
            BlockPos cursor = BlockPos.ZERO;
            int idx = 0, n = body.length();
            while (idx < n) {
                char c = body.charAt(idx);
                if (c == 'H') { cursor = BlockPos.ZERO; idx++; continue; }
                if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'U' || c == 'D') {
                    cursor = stepCursor(cursor, c); idx++; continue;
                }
                if (c == 'P' && idx + 1 < n && body.charAt(idx + 1) == '(') {
                    int j = idx + 2;
                    while (j < n && body.charAt(j) != ')') j++;
                    if (j < n) {
                        BlockPos wp = localToWorld(cursor, originWorld, basis);
                        BlockState cur = level.getBlockState(wp);
                        if (!cur.canBeReplaced()) {
                            p.displayClientMessage(Component.literal(
                                    "Cant paste: collision on " + wp.getX() + "," + wp.getY() + "," + wp.getZ()), true);
                            return;
                        }
                        idx = j + 1; continue;
                    }
                }
                idx++;
            }
        }

        double required = 0.0;
        {
            BlockPos cursor = BlockPos.ZERO;
            int idx = 0, n = body.length();
            while (idx < n) {
                char c = body.charAt(idx);
                if (c == 'H') { cursor = BlockPos.ZERO; idx++; continue; }
                if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'U' || c == 'D') {
                    cursor = stepCursor(cursor, c); idx++; continue;
                }
                if (c == 'P' || c == 'X') {
                    BlockPos wp = localToWorld(cursor, originWorld, basis);
                    required += calcStepCostFE(originWorld, wp);
                }
                idx++;
            }
        }

        int needFE = (int) Math.ceil(required);
        if (!this.usePower(p, needFE / 2.d, stack)) {
            p.displayClientMessage(Component.literal("Not enough energy (" + needFE + " FE) to paste."), true);
            return;
        }

        Map<AEItemKey, ResourceLocation> prettyNames = new LinkedHashMap<>();
        Map<AEItemKey, Long> needed = computeRequirements(palette, body, prettyNames);
        if (!needed.isEmpty()) {
            boolean ok = checkAndExtractFromME(stack, level, p, needed, prettyNames);
            if (!ok) return;
        }

        BlockPos cursor = BlockPos.ZERO;
        List<Runnable> pasteOps = new ArrayList<>();
        final int[] placedCounter = {0};

        int idx = 0, n = body.length();
        while (idx < n) {
            char c = body.charAt(idx);
            if (c == 'H') { cursor = BlockPos.ZERO; idx++; continue; }
            if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'U' || c == 'D') {
                cursor = stepCursor(cursor, c); idx++; continue;
            }
            if (c == 'P' && idx + 1 < n && body.charAt(idx + 1) == '(') {
                int j = idx + 2;
                while (j < n && body.charAt(j) != ')') j++;
                if (j < n) {
                    String num = body.substring(idx + 2, j);
                    try {
                        int id2 = Integer.parseInt(num);
                        BlockState st = palette.get(id2);
                        if (st != null) {
                            BlockPos wp = localToWorld(cursor, originWorld, basis);
                            pasteOps.add(() -> {
                                if (level.hasChunkAt(wp)) {
                                    level.setBlock(wp, st, 3);
                                    placedCounter[0]++;
                                }
                            });
                        }
                    } catch (NumberFormatException ignored) { }
                    idx = j + 1; continue;
                }
            }
            if (c == 'X') {
                BlockPos wp = localToWorld(cursor, originWorld, basis);
                pasteOps.add(() -> {
                    if (level.hasChunkAt(wp)) {
                        level.setBlock(wp, Blocks.AIR.defaultBlockState(), 3);
                    }
                });
                idx++; continue;
            }
            idx++;
        }

        if (level instanceof ServerLevel sl) {
            BuildScheduler.enqueue(
                    sl,
                    p.getUUID(),
                    4,
                    pasteOps,
                    () -> p.displayClientMessage(Component.literal("Pasted: " + placedCounter[0] + " blocks"), true)
            );
        }
    }


    public static Map<Integer, BlockState> parseHeaderToPalette(String header) {
        Map<Integer, BlockState> out = new HashMap<>();
        if (header == null || header.isEmpty()) return out;

        String[] lines = header.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int paren = line.indexOf('(');
            int close = line.lastIndexOf(')');
            if (paren <= 0 || close <= paren) continue;
            String numStr = line.substring(0, paren).trim();
            String spec = line.substring(paren + 1, close).trim();
            try {
                int id = Integer.parseInt(numStr);
                BlockState st = parseBlockStateSpec(spec);
                if (st != null) out.put(id, st);
            } catch (Exception ignored) { }
        }
        return out;
    }

    public static String getRequirementsString(ItemStack stack, Level level, @Nullable Player player) {
        var server = level.getServer();
        if (server == null) {
            return "";
        }

        String full = BuilderPatternHost.loadProgramFromFile(stack, server);
        if (full.isEmpty()) {
            return "";
        }

        int i = full.lastIndexOf(SEP);
        final String header = i >= 0 ? full.substring(0, i) : "";
        final String body   = i >= 0 ? full.substring(i + SEP.length()) : full;

        Map<Integer, BlockState> palette = parseHeaderToPalette(header);
        if (palette.isEmpty()) {
            return "";
        }

        Map<AEItemKey, ResourceLocation> nameMap = new LinkedHashMap<>();
        Map<AEItemKey, Long> needed = computeRequirements(palette, body, nameMap);
        IGrid grid = null;
        if (stack.getItem() instanceof WirelessTerminalItem pb){
            grid = pb.getLinkedGrid(stack, level, player);
        }
        MEStorage inv = null;
        IActionSource src = null;

        if (grid != null && player != null) {
            inv = grid.getStorageService().getInventory();
            src = IActionSource.ofPlayer(player);
        }

        StringBuilder sb = new StringBuilder();

        for (var e : needed.entrySet()) {
            AEItemKey key = e.getKey();
            long need = e.getValue();
            long have = 0;

            if (inv != null) {
                have = inv.extract(key, Long.MAX_VALUE, Actionable.SIMULATE, src);
            }

            ResourceLocation rl = nameMap.getOrDefault(
                    key,
                    new ResourceLocation("minecraft", "unknown")
            );

            boolean craftable = grid != null && grid.getCraftingService().isCraftable(key);

            sb.append(rl.toString())
                    .append(";")
                    .append(need)
                    .append(";")
                    .append(have)
                    .append(";")
                    .append(craftable ? 1 : 0)
                    .append("\n");
        }

        return sb.toString();
    }

    private static BlockHitResult rayTrace(Level level, Player player, double maxDistance) {
        if (level == null || player == null) {
            return null;
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

        ClipContext ctx = new ClipContext(
                eye,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );

        HitResult res = level.clip(ctx);
        if (res instanceof BlockHitResult bhr && res.getType() == HitResult.Type.BLOCK) {
            return bhr;
        }
        return null;
    }


    public static BlockState parseBlockStateSpec(String spec) {
        String name = spec;
        String props = null;
        int br = spec.indexOf('[');
        if (br >= 0 && spec.endsWith("]")) {
            name = spec.substring(0, br);
            props = spec.substring(br + 1, spec.length() - 1);
        }
        ResourceLocation rl = ResourceLocation.tryParse(name);
        if (rl == null) return null;
        var block = ForgeRegistries.BLOCKS.getValue(rl);
        if (block == null) return null;

        BlockState state = block.defaultBlockState();
        if (props == null || props.isEmpty()) return state;

        StateDefinition<?, ?> def = block.getStateDefinition();
        String[] pairs = props.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();
            Property<?> prop = def.getProperty(key);
            if (prop == null) continue;

            Optional<?> parsed = ((Property) prop).getValue(val);
            if (parsed.isPresent()) {
                state = setUnchecked(state, prop, (Comparable) parsed.get());
            }
        }
        return state;
    }

    private static BlockState setUnchecked(BlockState state, Property prop, Comparable value) {
        return state.setValue(prop, value);
    }

    private static void saveProgramToFile(String id, String code, MinecraftServer server) {
        Path file = server.getWorldPath(new LevelResource("serverdata"))
                .resolve("autobuilder")
                .resolve(id);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, code, StandardCharsets.UTF_8);
        } catch (IOException ignored) { }
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.open(CrazyMenuRegistrar.COPY_GADGET_MENU.get(), player, MenuLocators.forHand(player, player.swingingArm));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyItemRegistrar.PORTABLE_BUILDER.get().asItem().getDefaultInstance();
    }

    public static class Basis {
        final int fx, fz;
        final int rx, rz;

        private Basis(int fx, int fz, int rx, int rz) {
            this.fx = fx;
            this.fz = fz;
            this.rx = rx;
            this.rz = rz;
        }

        public static Basis forFacing(Direction f) {
            return switch (f) {
                case SOUTH -> new Basis(0, 1, -1, 0);
                case EAST  -> new Basis(1, 0, 0, 1);
                case WEST  -> new Basis(-1, 0, 0, -1);
                default    -> new Basis(0, -1, 1, 0); // NORTH
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

    public static BlockPos localToWorld(BlockPos local, BlockPos origin, Basis b) {
        int dx = local.getX() * b.rx + local.getZ() * b.fx;
        int dz = local.getX() * b.rz + local.getZ() * b.fz;
        int dy = local.getY();
        return origin.offset(dx, dy, dz);
    }

    private static BlockPos stepCursor(BlockPos cursor, char ch) {
        return switch (ch) {
            case 'F' -> cursor.offset(0, 0, 1);
            case 'B' -> cursor.offset(0, 0, -1);
            case 'R' -> cursor.offset(1, 0, 0);
            case 'L' -> cursor.offset(-1, 0, 0);
            case 'U' -> cursor.offset(0, 1, 0);
            case 'D' -> cursor.offset(0, -1, 0);
            default  -> cursor;
        };
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

    public static Direction readSrcFacingFromNbt(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("src_facing")) {
            String s = stack.getTag().getString("src_facing");
            Direction d = Direction.byName(s);
            if (d != null && d.getAxis().isHorizontal()) return d;
        }
        return Direction.NORTH;
    }

    public static int rotationSteps(Direction source, Direction target) {
        int a = switch (source) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default    -> 0;
        };
        int b = switch (target) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default    -> 0;
        };
        return (b - a) & 3;
    }

    private static void rotatePaletteInPlace(Map<Integer, BlockState> palette, int steps) {
        net.minecraft.world.level.block.Rotation rot = switch (((steps % 4) + 4) % 4) {
            case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
        for (Map.Entry<Integer, BlockState> e : palette.entrySet()) {
            e.setValue(e.getValue().rotate(rot));
        }
    }
}