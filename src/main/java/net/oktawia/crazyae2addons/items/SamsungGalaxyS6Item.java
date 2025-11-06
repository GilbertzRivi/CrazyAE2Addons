package net.oktawia.crazyae2addons.items;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.items.AEBaseItem;
import appeng.me.helpers.MachineSource;
import appeng.me.helpers.PlayerSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.logic.BuildScheduler;
import net.oktawia.crazyae2addons.logic.BuilderPatternHost;
import net.oktawia.crazyae2addons.logic.CopyGadgetHost;
import net.oktawia.crazyae2addons.logic.GadgetHost;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;

public class SamsungGalaxyS6Item extends AEBaseItem implements IMenuItem {

    private BlockPos cornerA = null;
    private BlockPos cornerB = null;
    private BlockPos origin = null;
    private Direction originFacing = Direction.NORTH;

    private static final int BASE_ENERGY_CAPACITY = 200_000;
    private static final int ENERGY_CARD_BONUS    = 200_000;
    private static final int ENERGY_CARD_SLOTS    = 5;
    private static final int MAX_RECEIVE          = 25_000;
    private static final int MAX_EXTRACT          = Integer.MAX_VALUE;

    private static final String NBT_ENERGY        = "energy";

    private static final String SEP               = "|";

    private static final String NBT_LINKED_AP = "linked_ap";

    private List<GenericStack> toCraft = new ArrayList<>();
    public List<Future<ICraftingPlan>> toCraftPlans = new ArrayList<>();

    public SamsungGalaxyS6Item(Properties props) {
        super(props.stacksTo(1));
    }

    public static void setLinkedAccessPoint(ItemStack stack, GlobalPos gp) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag c = new CompoundTag();
        c.putString("dim", gp.dimension().location().toString());
        c.putInt("x", gp.pos().getX());
        c.putInt("y", gp.pos().getY());
        c.putInt("z", gp.pos().getZ());
        tag.put(NBT_LINKED_AP, c);
    }

    public static void clearLinkedAccessPoint(ItemStack stack) {
        if (!stack.hasTag()) return;
        stack.getTag().remove(NBT_LINKED_AP);
    }

    @Nullable
    private static GlobalPos getLinkedAccessPoint(ItemStack stack) {
        if (!stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_LINKED_AP)) return null;

        CompoundTag c = tag.getCompound(NBT_LINKED_AP);
        var rl = ResourceLocation.tryParse(c.getString("dim"));
        if (rl == null) return null;

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, rl);
        BlockPos pos = new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z"));
        return GlobalPos.of(dimKey, pos);
    }

    public static @Nullable IGrid getLinkedGrid(ItemStack item, Level level, @Nullable Player sendMessagesTo) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        GlobalPos linked = getLinkedAccessPoint(item);
        if (linked == null) {
            if (sendMessagesTo != null && !level.isClientSide())
                sendMessagesTo.displayClientMessage(Component.literal("Not linked to any AE2 grid."), true);
            return null;
        }

        ServerLevel targetLevel = serverLevel.getServer().getLevel(linked.dimension());
        if (targetLevel == null) {
            if (sendMessagesTo != null && !level.isClientSide())
                sendMessagesTo.displayClientMessage(Component.literal("Linked dimension not found/loaded."), true);
            return null;
        }

        BlockEntity be = targetLevel.getBlockEntity(linked.pos());
        if (!(be instanceof IWirelessAccessPoint wap)) {
            if (sendMessagesTo != null && !level.isClientSide())
                sendMessagesTo.displayClientMessage(Component.literal("No Wireless Access Point at linked position."), true);
            return null;
        }

        IGrid grid = wap.getGrid();
        if (grid == null) {
            if (sendMessagesTo != null && !level.isClientSide())
                sendMessagesTo.displayClientMessage(Component.literal("AE2 grid not found."), true);
            return null;
        }

        if (!grid.getEnergyService().isNetworkPowered()) {
            if (sendMessagesTo != null && !level.isClientSide())
                sendMessagesTo.displayClientMessage(Component.literal("AE2 grid is not powered."), true);
            return null;
        }

        return grid;
    }

    private static Map<AEItemKey, Long> computeRequirements(Map<Integer, BlockState> palette, String body,
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


    public void scheduleCrafts(IGrid grid, Level level, Player player) {
        var builder = grid.getMachines(AutoBuilderBE.class).stream().findFirst();
        if (builder.isEmpty()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.literal("Auto Builder not found. Can not schedule crafts."), true);
            }
            return;
        }
        for (GenericStack stack : toCraft) {
                toCraftPlans.add(grid.getCraftingService().beginCraftingCalculation(
                    level,
                    () -> new MachineSource(builder.get()),
                    stack.what(),
                    stack.amount(),
                    CalculationStrategy.REPORT_MISSING_ITEMS
            ));
        }
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.literal("Scheduling crafts."), true);
        }
    }

    private boolean checkAndExtractFromME(ItemStack stack, Level level, Player p,
                                          Map<AEItemKey, Long> needed,
                                          Map<AEItemKey, ResourceLocation> nameMap) {
        var grid = getLinkedGrid(stack, level, p);
        if (grid == null) {
            return false;
        }

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
            IUpgradeInventory upgrades = getUpgrades(stack);
            int craftingCards = upgrades.getInstalledUpgrades(AEItems.CRAFTING_CARD);

            if (craftingCards > 0) {
                toCraft.clear();
                for (var tc : missing.entrySet()){
                    AEItemKey key = tc.getKey();
                    long amount = tc.getValue();
                    toCraft.add(new GenericStack(key, amount));
                }
                scheduleCrafts(grid, level, p);
                return false;
            }

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

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);

        if (!level.isClientSide() && p.isShiftKeyDown()) {
            MenuOpener.open(CrazyMenuRegistrar.COPY_GADGET_MENU.get(), p, MenuLocators.forHand(p, hand));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }

        if (!level.isClientSide() && cornerA != null && cornerB != null && origin != null) {
            generateProgramAndCopy(level, p, stack);
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

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BlockPos placeOrigin = clicked.relative(ctx.getClickedFace());
                pasteNow(level, player, stack, placeOrigin, player.getDirection());
            }
            return InteractionResult.SUCCESS;
        }

        if (cornerA == null) {
            cornerA = clicked.immutable();
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


    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new CopyGadgetHost(player, inventorySlot, stack);
    }

    private static int getEnergy(ItemStack stack) {
        return stack.getOrCreateTag().getInt(NBT_ENERGY);
    }

    private static void setEnergy(ItemStack stack, int value) {
        int cap = getMaxEnergyCapacity(stack);
        stack.getOrCreateTag().putInt(NBT_ENERGY, Math.max(0, Math.min(cap, value)));
    }

    public IUpgradeInventory getUpgrades(ItemStack stack) {
        IUpgradeInventory inv = UpgradeInventories.forItem(stack, ENERGY_CARD_SLOTS);
        int cap = getMaxEnergyCapacity(stack);
        if (getEnergy(stack) > cap) setEnergy(stack, cap);
        return inv;
    }

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

        IUpgradeInventory inv = getUpgrades(stack);
        int cards = inv.getInstalledUpgrades(AEItems.ENERGY_CARD);
        tooltip.add(Component.literal("Energy Cards: " + cards + " / " + (ENERGY_CARD_SLOTS - 1)));

        GlobalPos gp = getLinkedAccessPoint(stack);
        if (gp != null) {
            tooltip.add(Component.literal(
                    "Linked to: %d, %d, %d".formatted(
                            gp.pos().getX(), gp.pos().getY(), gp.pos().getZ()
                    )));
        } else {
            tooltip.add(Component.literal("Linked AP: <none>"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (!level.isClientSide()) {
            getUpgrades(stack);
        }
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        Iterator<Future<ICraftingPlan>> iterator = toCraftPlans.iterator();
        while (iterator.hasNext()) {
            Future<ICraftingPlan> craftingPlan = iterator.next();
            if (craftingPlan.isDone()) {
                try {
                    if (!craftingPlan.get().missingItems().isEmpty()){
                        this.toCraftPlans.clear();
                        player.displayClientMessage(Component.literal("Can not craft all required items."), true);
                        return;
                    }
                    var grid = getLinkedGrid(stack, level, null);
                    if (grid == null) return;
                    var result = grid.getCraftingService().submitJob(
                            craftingPlan.get(), null, null, true, IActionSource.ofPlayer(player));
                    if (result.successful()) {
                        iterator.remove();
                    }
                } catch (Throwable ignored) {}
            }
        }

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
        return distance * CrazyConfig.COMMON.NokiaCost.get();
    }

    private void generateProgramAndCopy(Level level, Player p, ItemStack stack) {
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
            p.displayClientMessage(Component.literal("Not enough energy (" + needFE + " FE) to copy."), true);
            return;
        }
        extractEnergyFrom(stack, needFE, false);

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
            p.displayClientMessage(Component.literal("Could not save this structure"), true);
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
                if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'U' || c == 'D') { cursor = stepCursor(cursor, c); idx++; continue; }
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
                if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'U' || c == 'D') { cursor = stepCursor(cursor, c); idx++; continue; }
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

        Map<AEItemKey, ResourceLocation> prettyNames = new LinkedHashMap<>();
        Map<AEItemKey, Long> needed = computeRequirements(palette, body, prettyNames);
        if (!needed.isEmpty()) {
            boolean ok = checkAndExtractFromME(stack, level, p, needed, prettyNames);
            if (!ok) return;
        }

        extractEnergyFrom(stack, needFE, false);

        BlockPos cursor = BlockPos.ZERO;
        List<Runnable> pasteOps = new ArrayList<>();
        final int[] placedCounter = {0};

        int idx = 0, n = body.length();
        while (idx < n) {
            char c = body.charAt(idx);
            if (c == 'H') { cursor = BlockPos.ZERO; idx++; continue; }
            if (c == 'F' || c == 'B' || c == 'L' || c == 'R' || c == 'U' || c == 'D') { cursor = stepCursor(cursor, c); idx++; continue; }
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

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyCapProvider(stack);
    }

    private record StackEnergyStorage(ItemStack stack) implements IEnergyStorage {

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return SamsungGalaxyS6Item.receiveEnergyInto(stack, maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return SamsungGalaxyS6Item.extractEnergyFrom(stack, maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return SamsungGalaxyS6Item.getEnergy(stack);
        }

        @Override
        public int getMaxEnergyStored() {
            return SamsungGalaxyS6Item.getMaxEnergyCapacity(stack);
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }

    private static final class EnergyCapProvider implements ICapabilityProvider {
        private final LazyOptional<IEnergyStorage> energy;

        EnergyCapProvider(ItemStack stack) {
            this.energy = LazyOptional.of(() -> new StackEnergyStorage(stack));
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
        }
    }

}