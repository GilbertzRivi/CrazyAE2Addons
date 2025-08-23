package net.oktawia.crazyae2addons.items;

import appeng.api.config.FuzzyMode;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.BuildScheduler;
import net.oktawia.crazyae2addons.logic.BuilderPatternHost;
import net.oktawia.crazyae2addons.logic.GadgetHost;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import net.oktawia.crazyae2addons.recipes.StructureSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StructureGadgetItem extends AEBaseItem implements IMenuItem {

    // =========================
    // Selekcja/stan pracy
    // =========================
    private BlockPos cornerA = null;
    private BlockPos cornerB = null;
    private BlockPos origin = null;
    private Direction originFacing = Direction.NORTH;

    // =========================
    // Energia
    // =========================
    private static final int BASE_ENERGY_CAPACITY = 200_000;     // baza
    private static final int ENERGY_CARD_BONUS    = 200_000;     // +200k za kartę
    private static final int ENERGY_CARD_SLOTS    = 4;           // max kart
    private static final int MAX_RECEIVE          = 25_000;      // I/O limity
    private static final int MAX_EXTRACT          = Integer.MAX_VALUE;

    private static final String NBT_ENERGY        = "energy";

    // =========================
    // Program
    // =========================
    private static final String SEP = "|";

    public StructureGadgetItem(Properties props) {
        super(props.stacksTo(1));
    }

    // =========================================================
    // UI / użycie
    // =========================================================

    public static boolean hasStoredStructure(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        if (!tag.getBoolean("code")) return false;
        if (!tag.contains("program_id")) return false;
        String id = tag.getString("program_id");
        return id != null && !id.isEmpty();
    }

    @Nullable
    public static String getProgramIdOrNull(ItemStack stack) {
        if (!hasStoredStructure(stack)) return null;
        return stack.getTag().getString("program_id");
    }

    // === Snapshot z NBT gadżetu → StructureSnapshot ===
    public static @Nullable StructureSnapshot loadSnapshot(ItemStack stack, @Nullable Level level) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;

        if (!tag.contains("preview_palette") || !tag.contains("preview_positions") || !tag.contains("preview_indices"))
            return null;

        // --- paleta ---
        var palList = tag.getList("preview_palette", Tag.TAG_STRING);
        if (palList.isEmpty()) return null;

        List<BlockState> palette = new ArrayList<>(palList.size());
        for (int i = 0; i < palList.size(); i++) {
            String spec = palList.getString(i);
            BlockState st = parseBlockStateSpecForSnapshot(spec);
            if (st == null) return null;
            palette.add(st);
        }

        int[] posArr = tag.getIntArray("preview_positions");
        int[] idxArr = tag.getIntArray("preview_indices");
        if (posArr.length == 0 || posArr.length % 3 != 0) return null;
        int blocksN = posArr.length / 3;
        if (idxArr.length != blocksN) return null;

        // --- bounding box ---
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < blocksN; i++) {
            int x = posArr[i * 3];
            int y = posArr[i * 3 + 1];
            int z = posArr[i * 3 + 2];
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
        }

        int sizeX = (maxX - minX) + 1;
        int sizeY = (maxY - minY) + 1;
        int sizeZ = (maxZ - minZ) + 1;

        // --- mapa bloków ---
        Map<BlockPos, BlockState> map = new HashMap<>(blocksN * 2);
        for (int i = 0; i < blocksN; i++) {
            int lx = posArr[i * 3]     - minX;
            int ly = posArr[i * 3 + 1] - minY;
            int lz = posArr[i * 3 + 2] - minZ;

            int palIndex = idxArr[i];
            if (palIndex < 0 || palIndex >= palette.size()) continue;
            map.put(new BlockPos(lx, ly, lz), palette.get(palIndex));
        }

        return new StructureSnapshot(sizeX, sizeY, sizeZ, map);
    }

    // Helper: spec „modid:block[prop=val,...]” → BlockState
    private static @Nullable BlockState parseBlockStateSpecForSnapshot(String spec) {
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


    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);

        // Otwórz GUI gadżetu (Shift+PPM)
        if (!level.isClientSide() && p.isShiftKeyDown()) {
            MenuOpener.open(CrazyMenuRegistrar.GADGET_MENU.get(), p, MenuLocators.forHand(p, hand));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }

        // Jeśli mamy już kod – najpierw wklej
        if (stack.hasTag() && stack.getTag().getBoolean("code")) {
            if (level.isClientSide()) {
                p.displayClientMessage(Component.literal("First paste your current structure"), true);
            }
            return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()), stack);
        }

        // Gdy mamy dwa rogi i origin – wytnij + zapisz program
        if (!level.isClientSide() && cornerA != null && cornerB != null && origin != null) {
            generateProgramAndCut(level, p, stack);
            return InteractionResultHolder.success(stack);
        }

        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()), stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null || player.isLocalPlayer()) return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();

        // Shift+klik = wklej w miejscu (po stronie klikniętej ścianki)
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BlockPos placeOrigin = clicked.relative(ctx.getClickedFace());
                pasteNow(level, player, stack, placeOrigin, player.getDirection());
            }
            return InteractionResult.SUCCESS;
        }

        // Wybór cornerów do CUT
        if (stack.hasTag() && stack.getTag().getBoolean("code")) {
            player.displayClientMessage(Component.literal("Paste current structure first"), true);
        } else if (cornerA == null) {
            cornerA = clicked.immutable();
            // zapisz do NBT dla clientowego preview kosztu
            CompoundTag tag = stack.getOrCreateTag();
            tag.putIntArray("selA", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
            player.displayClientMessage(Component.literal("Corner 1 set!"), true);
        } else if (cornerB == null) {
            cornerB = clicked.immutable();
            origin = clicked.immutable();
            originFacing = player.getDirection();
            stack.getOrCreateTag().remove("selA");
            player.displayClientMessage(Component.literal("Corner 2 set! (origin)"), true);
        } else {
            cornerA = clicked.immutable();
            cornerB = null;
            origin = null;
            CompoundTag tag = stack.getOrCreateTag();
            tag.putIntArray("selA", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
            player.displayClientMessage(Component.literal("Corner 1 set! (reset)"), true);
        }
        return InteractionResult.SUCCESS;
    }

    // =========================================================
    // AE2 menu-host
    // =========================================================

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new GadgetHost(player, inventorySlot, stack);
    }

    // =========================================================
    // Energia (z AE2 UpgradeInventories — Energy Card)
    // =========================================================

    private static int getEnergy(ItemStack stack) {
        return stack.getOrCreateTag().getInt(NBT_ENERGY);
    }

    private static void setEnergy(ItemStack stack, int value) {
        int cap = getMaxEnergyCapacity(stack);
        stack.getOrCreateTag().putInt(NBT_ENERGY, Math.max(0, Math.min(cap, value)));
    }

    /** Zwraca inwentarz upgrade’ów AE2 dla tego itemu (utrzymywany w NBT stacka). */
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        IUpgradeInventory inv = UpgradeInventories.forItem(stack, ENERGY_CARD_SLOTS);
        // sanity: przytnij energię gdy zmalała pojemność (wyjęto karty)
        int cap = getMaxEnergyCapacity(stack);
        if (getEnergy(stack) > cap) setEnergy(stack, cap);
        return inv;
    }

    /** Maksymalna pojemność = baza + (liczba ENERGY_CARD) * bonus. */
    private static int getMaxEnergyCapacity(ItemStack stack) {
        IUpgradeInventory inv = UpgradeInventories.forItem(stack, ENERGY_CARD_SLOTS);
        int cards = inv.getInstalledUpgrades(AEItems.ENERGY_CARD);
        return BASE_ENERGY_CAPACITY + cards * ENERGY_CARD_BONUS;
    }

    private static int receiveEnergyInto(ItemStack stack, int amount, boolean simulate) {
        int cap = getMaxEnergyCapacity(stack);
        int stored = getEnergy(stack);
        int canReceive = Math.min(cap - stored, Math.min(MAX_RECEIVE, amount));
        if (!simulate && canReceive > 0) setEnergy(stack, stored + canReceive);
        return Math.max(0, canReceive);
    }

    private static int extractEnergyFrom(ItemStack stack, int amount, boolean simulate) {
        int stored = getEnergy(stack);
        int canExtract = Math.min(stored, Math.min(MAX_EXTRACT, amount));
        if (!simulate && canExtract > 0) setEnergy(stack, stored - canExtract);
        return Math.max(0, canExtract);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) { return true; }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        int e = getEnergy(stack);
        int cap = Math.max(1, getMaxEnergyCapacity(stack));
        return Math.round(13.0f * e / cap);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) { return 0x00FF66; }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        int e = getEnergy(stack);
        int cap = getMaxEnergyCapacity(stack);
        tooltip.add(Component.literal(String.format("Energy: %,d / %,d FE", e, cap)));
        IUpgradeInventory inv = UpgradeInventories.forItem(stack, ENERGY_CARD_SLOTS);
        int cards = inv.getInstalledUpgrades(AEItems.ENERGY_CARD);
        tooltip.add(Component.literal("Energy Cards: " + cards + " / " + ENERGY_CARD_SLOTS));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide()) {
            int cap = getMaxEnergyCapacity(stack);
            if (getEnergy(stack) > cap) {
                setEnergy(stack, cap);
            }
        }
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        if (getEnergy(stack) >= getMaxEnergyCapacity(stack)) return;

        final int[] toFill = { Math.min(MAX_RECEIVE, getMaxEnergyCapacity(stack) - getEnergy(stack)) };
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && toFill[0] > 0; i++) {
            if (i == slot) continue;
            ItemStack other = inv.getItem(i);
            if (other.isEmpty()) continue;

            other.getCapability(ForgeCapabilities.ENERGY).ifPresent(src -> {
                if (!src.canExtract()) return;
                int canGive = src.extractEnergy(Math.min(MAX_RECEIVE, toFill[0]), true);
                if (canGive > 0) {
                    int accepted = receiveEnergyInto(stack, canGive, false);
                    if (accepted > 0) {
                        src.extractEnergy(accepted, false);
                        toFill[0] -= accepted;
                    }
                }
            });
        }
        super.inventoryTick(stack, level, entity, slot, selected);
    }

    public static double calcStepCostFE(BlockPos from, BlockPos target) {
        double dx = target.getX() - from.getX();
        double dy = target.getY() - from.getY();
        double dz = target.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return Math.pow(distance, 3) / 25.0;
    }

    public static int computeCutCostFE(Level level, BlockPos cornerA, BlockPos cornerB, BlockPos origin) {
        if (level == null || cornerA == null || cornerB == null || origin == null) return 0;

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

                    var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (id == null) continue;

                    var itemKey = AEItemKey.of(state.getBlock().asItem());
                    if (itemKey.fuzzyEquals(AEItemKey.of(Blocks.AIR.asItem()), FuzzyMode.IGNORE_ALL)) continue;

                    required += calcStepCostFE(origin, wp);
                }
            }
        }
        return (int) Math.ceil(required);
    }

    private void generateProgramAndCut(Level level, Player p, ItemStack stack) {
        if (stack.hasTag() && stack.getTag().getBoolean("code")) {
            p.displayClientMessage(Component.literal("Paste current structure first"), true);
            return;
        }
        if (cornerA == null || cornerB == null || origin == null) {
            p.displayClientMessage(Component.literal("Select corners first"), true);
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
        if (extractEnergyFrom(stack, needFE, true) < needFE) {
            p.displayClientMessage(Component.literal("Not enough energy (" + needFE + " FE) to cut."), true);
            return;
        }
        extractEnergyFrom(stack, needFE, false);

        Basis basis = Basis.forFacing(originFacing);
        Map<String, Integer> blockMap = new LinkedHashMap<>();
        int blockIdCounter = 1;

        StringBuilder pattern = new StringBuilder("H");
        BlockPos cursorLocal = BlockPos.ZERO;

        int cutCount = 0;
        List<BlockPos> previewPositions = new ArrayList<>();
        List<Integer> previewIndices = new ArrayList<>();
        List<Runnable> cutOps = new ArrayList<>();

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

                    cutOps.add(() -> {
                        if (level.hasChunkAt(wp)) {
                            level.removeBlock(wp, false);
                        }
                    });

                    cutCount++;
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
            p.displayClientMessage(Component.literal("Could not save this structure"), true);
            return;
        }

        p.displayClientMessage(Component.literal("Starting CUT: " + cutCount + " blocks"), true);

        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            int finalCutCount = cutCount;
            net.oktawia.crazyae2addons.logic.BuildScheduler.enqueue(
                    sl,
                    p.getUUID(),
                    4,
                    cutOps,
                    () -> {
                        // dopiero TERAZ zapisujemy program do NBT
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
                        p.displayClientMessage(Component.literal("CUT complete (" + finalCutCount + " blocks)"), true);
                        cornerA = cornerB = origin = null;
                    }
            );
        }
    }

    private void pasteNow(Level level, Player p, ItemStack stack, BlockPos originWorld, Direction pasteFacing) {
        if (!stack.hasTag() || !stack.getTag().getBoolean("code")) {
            p.displayClientMessage(Component.literal("No stored structure"), true);
            return;
        }

        String full = BuilderPatternHost.loadProgramFromFile(stack, p.getServer());
        if (full.isEmpty()) {
            p.displayClientMessage(Component.literal("No stored structure"), true);
            return;
        }
        int i = full.lastIndexOf(SEP);
        final String header = i >= 0 ? full.substring(0, i) : "";
        final String body = i >= 0 ? full.substring(i + (SEP).length()) : full;

        Map<Integer, BlockState> palette = parseHeaderToPalette(header);
        if (palette.isEmpty()) {
            p.displayClientMessage(Component.literal("Invalid palette"), true);
            return;
        }

        Direction srcFacing = readSrcFacingFromNbt(stack);
        int steps = rotationSteps(srcFacing, pasteFacing);
        if (steps != 0) rotatePaletteInPlace(palette, steps);

        Basis basis = Basis.forFacing(pasteFacing);

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
                        String num = body.substring(idx + 2, j);
                        try {
                            int id = Integer.parseInt(num);
                            BlockState st = palette.get(id);
                            if (st != null) {
                                BlockPos wp = localToWorld(cursor, originWorld, basis);
                                BlockState cur = level.getBlockState(wp);
                                if (!cur.canBeReplaced() || cur.equals(st)) {
                                    p.displayClientMessage(Component.literal(
                                            "Cant paste: collision on " + wp.getX() + "," + wp.getY() + "," + wp.getZ()), true);
                                    return;
                                }
                            }
                        } catch (NumberFormatException ignored) { }
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
        if (extractEnergyFrom(stack, needFE, true) < needFE) {
            p.displayClientMessage(Component.literal("Not enough energy (" + needFE + " FE) to paste."), true);
            return;
        }
        extractEnergyFrom(stack, needFE, false);

        clearStoredStructure(stack, p.getServer());

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
                        int id = Integer.parseInt(num);
                        BlockState st = palette.get(id);
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

        p.displayClientMessage(Component.literal("Starting PASTE: " + pasteOps.size() + " blocks @ 4/tick"), true);

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


    private static void clearStoredStructure(ItemStack stack, MinecraftServer server) {
        if (!stack.hasTag()) return;
        var tag = stack.getTag();

        String programId = tag.contains("program_id") ? tag.getString("program_id") : null;

        tag.remove("code");
        tag.remove("program_id");
        tag.remove("delay");
        tag.remove("src_facing");
        tag.remove("preview_palette");
        tag.remove("preview_indices");
        tag.remove("preview_positions");

        if (server != null && programId != null && !programId.isEmpty()) {
            try {
                Path file = server.getWorldPath(new LevelResource("serverdata"))
                        .resolve("autobuilder")
                        .resolve(programId);
                Files.deleteIfExists(file);
            } catch (Exception ignored) { }
        }
    }

    private Map<Integer, BlockState> parseHeaderToPalette(String header) {
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

    private BlockState parseBlockStateSpec(String spec) {
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

            java.util.Optional<?> parsed = ((Property) prop).getValue(val);
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

    // =========================================================
    // Transformacje układu współrzędnych i narzędzia
    // =========================================================

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

    // =========================================================
    // Odtwarzanie danych preview z kodu (na potrzeby klienta)
    // =========================================================

    public static void rebuildPreviewFromCode(ItemStack stack, @Nullable MinecraftServer server, String full) {
        if (stack == null) return;
        if (full == null) full = "";

        CompoundTag tag = stack.getOrCreateTag();
        tag.remove("preview_palette");
        tag.remove("preview_indices");
        tag.remove("preview_positions");
        if (full.isEmpty()) return;

        int sep = full.lastIndexOf(SEP);
        String header = sep >= 0 ? full.substring(0, sep) : "";
        String body   = sep >= 0 ? full.substring(sep + 1) : full;

        Map<Integer, String> idToSpec = new HashMap<>();
        if (!header.isEmpty()) {
            List<String> tokens = new ArrayList<>();
            StringBuilder cur = new StringBuilder();
            int depth = 0;
            for (int pos = 0; pos < header.length(); pos++) {
                char ch = header.charAt(pos);
                if (ch == '(') { depth++; cur.append(ch); }
                else if (ch == ')') { depth = Math.max(0, depth - 1); cur.append(ch); }
                else if (ch == ',' && depth == 0) {
                    String t = cur.toString().trim();
                    if (!t.isEmpty()) tokens.add(t);
                    cur.setLength(0);
                } else { cur.append(ch); }
            }
            String last = cur.toString().trim();
            if (!last.isEmpty()) tokens.add(last);

            java.util.regex.Pattern pat = java.util.regex.Pattern.compile("^\\s*(\\d+)\\s*\\((.*)\\)\\s*$");
            for (String tok : tokens) {
                String t = tok.trim();
                if (t.isEmpty()) continue;
                java.util.regex.Matcher m = pat.matcher(t);
                if (!m.matches()) continue;
                try {
                    int id = Integer.parseInt(m.group(1));
                    String spec = m.group(2).trim();
                    if (!spec.isEmpty()) idToSpec.put(id, spec);
                } catch (NumberFormatException ignored) { }
            }
        }
        if (idToSpec.isEmpty()) return;

        List<Integer> sortedIds = new ArrayList<>(idToSpec.keySet());
        Collections.sort(sortedIds);
        Map<Integer, Integer> idToIndex = new HashMap<>();
        var palList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < sortedIds.size(); i++) {
            int id = sortedIds.get(i);
            idToIndex.put(id, i);
            palList.add(net.minecraft.nbt.StringTag.valueOf(idToSpec.get(id)));
        }
        tag.put("preview_palette", palList);

        List<BlockPos> positions = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        BlockPos cursor = BlockPos.ZERO;

        int i = 0, n = body.length();
        while (i < n) {
            char c = body.charAt(i);

            if (c == 'H') { cursor = BlockPos.ZERO; i++; continue; }

            if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'U' || c == 'D') {
                cursor = stepCursor(cursor, c);
                i++; continue;
            }

            if (c == 'Z' && i + 1 < n && body.charAt(i + 1) == '|') {
                i += 2;
                while (i < n && Character.isDigit(body.charAt(i))) i++;
                continue;
            }

            if (c == 'P' && i + 1 < n && body.charAt(i + 1) == '(') {
                int j = i + 2;
                while (j < n && body.charAt(j) != ')') j++;
                if (j < n) {
                    String num = body.substring(i + 2, j);
                    try {
                        int id = Integer.parseInt(num);
                        Integer palIdx = idToIndex.get(id);
                        if (palIdx != null) {
                            positions.add(cursor);
                            indices.add(palIdx);
                        }
                    } catch (NumberFormatException ignored) { }
                    i = j + 1;
                    continue;
                }
            }

            if (c == 'P' && i + 1 < n && body.charAt(i + 1) == '|') {
                int j = i + 2;
                while (j < n) {
                    char cj = body.charAt(j);
                    if (cj=='H'||cj=='Z'||cj=='P'||cj=='F'||cj=='B'||cj=='L'||cj=='R'||cj=='U'||cj=='D'||cj=='X' || cj=='\n' || cj=='\r') break;
                    j++;
                }
                i = j; continue;
            }

            if (c == 'X') { i++; continue; }
            i++;
        }

        int[] idxArr = indices.stream().mapToInt(Integer::intValue).toArray();
        tag.putIntArray("preview_indices", idxArr);

        int[] posArr = new int[positions.size() * 3];
        int k = 0;
        for (BlockPos bp : positions) {
            posArr[k++] = bp.getX();
            posArr[k++] = bp.getY();
            posArr[k++] = bp.getZ();
        }
        tag.putIntArray("preview_positions", posArr);
    }
}
