package net.oktawia.crazyae2addons.logic.builder;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageHelper;
import appeng.core.definitions.AEItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.entities.AutoBuilderCreativeSupplyBE;
import net.oktawia.crazyae2addons.misc.ProgramExpander;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AutoBuilderWorldOps {

    private AutoBuilderWorldOps() {
    }

    public static TickRateModulation tickingRequest(AutoBuilderBE be, IGridNode node, int ticksSinceLastCall) {
        tickRedstonePulse(be, ticksSinceLastCall);

        var missing = be.buffer.tick(ticksSinceLastCall);
        if (missing != null) {
            be.setMissingItem(missing);
            be.isCrafting = false;
        }

        if (be.buffer.isFlushPending()) {
            return TickRateModulation.URGENT;
        }
        if (!be.isRunning || be.code.isEmpty() || be.isCrafting) {
            return TickRateModulation.URGENT;
        }
        if (tickPreRunChecks(be, ticksSinceLastCall)) {
            return TickRateModulation.URGENT;
        }

        tickDispatchInstructions(be);
        return TickRateModulation.URGENT;
    }

    static void tickRedstonePulse(AutoBuilderBE be, int ticksSinceLastCall) {
        if (be.redstonePulseTicks > 0) {
            be.redstonePulseTicks -= ticksSinceLastCall;
            if (be.redstonePulseTicks <= 0) {
                be.redstonePulseTicks = 0;
                be.isPulsing = false;
                be.setChanged();

                if (be.getLevel() != null && !be.getLevel().isClientSide) {
                    be.getLevel().updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
                }
            }
        }
    }

    static boolean tickPreRunChecks(AutoBuilderBE be, int ticksSinceLastCall) {
        if (!be.energyPrepaid) {
            be.isRunning = false;
            beginFlushBuffer(be);
            return true;
        }

        if (be.inventory.getStackInSlot(0).isEmpty()) {
            be.isRunning = false;
            AutoBuilderPreviewOps.resetGhostToHome(be);
            beginFlushBuffer(be);
            return true;
        }

        if (be.tickDelayLeft > 0) {
            be.tickDelayLeft -= ticksSinceLastCall;
            return true;
        }

        return false;
    }

    static void tickDispatchInstructions(AutoBuilderBE be) {
        boolean didWork = false;

        for (int steps = 0; steps < calcStepsPerTick(be) && be.currentInstruction < be.code.size(); steps++) {
            String inst = be.code.get(be.currentInstruction);

            if (inst.startsWith("Z|")) {
                be.tickDelayLeft = Integer.parseInt(inst.substring(2));
                be.currentInstruction++;
                be.setChanged();
                return;
            }

            didWork = true;

            switch (inst) {
                case "F", "B", "L", "R", "U", "D" ->
                        AutoBuilderPreviewOps.setGhostRenderPos(
                                be,
                                AutoBuilderPreviewOps.stepRelative(be, be.getGhostRenderPos(), inst.charAt(0))
                        );
                case "H" -> AutoBuilderPreviewOps.resetGhostToHome(be);
                case "X" -> {
                    if (executeBreak(be)) {
                        be.currentInstruction++;
                        be.setChanged();
                        return;
                    }
                }
                default -> {
                    if (inst.startsWith("P|")) {
                        if (tickInstructionPlace(be, inst.substring(2))) {
                            return;
                        }
                    } else if (inst.startsWith("PEQ|") || inst.startsWith("PNE|")) {
                        String[] parts = inst.substring(4).split("\\|", 2);
                        if (parts.length == 2) {
                            String checkId = parts[1].contains("[")
                                    ? parts[1].substring(0, parts[1].indexOf('['))
                                    : parts[1];
                            Block checkBlock = BuiltInRegistries.BLOCK
                                    .getOptional(ResourceLocation.parse(checkId))
                                    .orElse(Blocks.AIR);
                            boolean matches = be.getLevel().getBlockState(be.getGhostRenderPos()).getBlock() == checkBlock;
                            if ((inst.startsWith("PEQ|") == matches) && tickInstructionPlace(be, parts[0])) {
                                return;
                            }
                        }
                    } else if (inst.startsWith("XEQ|") || inst.startsWith("XNE|")) {
                        String checkId = inst.substring(4);
                        if (checkId.contains("[")) {
                            checkId = checkId.substring(0, checkId.indexOf('['));
                        }
                        Block checkBlock = BuiltInRegistries.BLOCK
                                .getOptional(ResourceLocation.parse(checkId))
                                .orElse(Blocks.AIR);
                        boolean matches = be.getLevel().getBlockState(be.getGhostRenderPos()).getBlock() == checkBlock;
                        if (inst.startsWith("XEQ|") == matches) {
                            if (executeBreak(be)) {
                                be.currentInstruction++;
                                be.setChanged();
                                return;
                            }
                        }
                    }
                }
            }

            be.currentInstruction++;
        }

        if (be.currentInstruction >= be.code.size()) {
            be.isRunning = false;
            be.energyPrepaid = false;

            ItemStack pattern = be.inventory.getStackInSlot(0);
            if (!pattern.isEmpty()) {
                be.inventory.setItemDirect(0, ItemStack.EMPTY);
                be.inventory.setItemDirect(1, pattern.copyWithCount(1));
            }

            AutoBuilderPreviewOps.resetGhostToHome(be);
            triggerRedstonePulse(be);

            if (!be.buffer.isEmpty()) {
                beginFlushBuffer(be);
            }

            be.setChanged();
        } else if (didWork) {
            be.tickDelayLeft = be.delay;
            be.setChanged();
        }
    }

    static boolean tickInstructionPlace(AutoBuilderBE be, String blockIdRaw) {
        try {
            String blockIdClean;
            Map<String, String> props = new HashMap<>();
            int idx = blockIdRaw.indexOf('[');

            if (idx > 0 && blockIdRaw.endsWith("]")) {
                blockIdClean = blockIdRaw.substring(0, idx);
                String propString = blockIdRaw.substring(idx + 1, blockIdRaw.length() - 1);
                for (String pair : propString.split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        props.put(kv[0], kv[1]);
                    }
                }
            } else {
                blockIdClean = blockIdRaw;
            }

            Block block = BuiltInRegistries.BLOCK
                    .getOptional(ResourceLocation.parse(blockIdClean))
                    .orElse(Blocks.AIR);
            if (block == Blocks.AIR) {
                return false;
            }

            var grid = be.getMainNode().getGrid();
            if (grid == null) {
                return false;
            }

            BlockPos target = be.getGhostRenderPos();
            if (be.getLevel().getBlockState(target).getBlock() == block) {
                return false;
            }

            if (BuilderCoordMath.isBreakable(be.getLevel().getBlockState(target), be.getLevel(), target)) {
                var drops = getSilkTouchDrops(be.getLevel().getBlockState(target), (ServerLevel) be.getLevel(), target);
                long inserted = 0;
                for (var drop : drops) {
                    inserted += StorageHelper.poweredInsert(
                            grid.getEnergyService(),
                            grid.getStorageService().getInventory(),
                            AEItemKey.of(drop.getItem()),
                            1,
                            IActionSource.ofMachine(be),
                            Actionable.MODULATE
                    );
                }
                if (inserted <= 0 && !drops.isEmpty()) {
                    be.currentInstruction++;
                    be.setChanged();
                    return true;
                }
            }

            boolean creative = hasCreativeSupply(be);
            long extracted = 0;

            if (!creative) {
                AEItemKey key = AEItemKey.of(block.asItem());
                extracted = be.buffer.extract(key, 1);
                if (extracted <= 0) {
                    be.setMissingItem(new GenericStack(key, 1));
                    if (be.skipEmpty) {
                        be.currentInstruction++;
                        be.setChanged();
                        return true;
                    }
                    be.isRunning = false;
                    be.energyPrepaid = false;
                    beginFlushBuffer(be);
                    return true;
                }
            }

            if (extracted > 0 || creative) {
                BlockState state = block.defaultBlockState();
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                    if (property != null) {
                        state = BuilderCoordMath.applyProperty(state, property, entry.getValue());
                    }
                }

                int delta = Math.floorMod(
                        BuilderCoordMath.yawStepsFromNorth(AutoBuilderPreviewOps.getFacing(be)) -
                                BuilderCoordMath.yawStepsFromNorth(be.sourceFacing),
                        4
                );
                state = BuilderCoordMath.rotateStateByDelta(state, delta);
                be.getLevel().setBlock(target, state, 3);
                be.clearMissingItem();
            }
        } catch (Exception e) {
            CrazyAddons.LOGGER.debug("error during builder block placement", e);
        }

        return false;
    }

    static List<GenericStack> toGenericStackList(Map<String, Integer> required) {
        List<GenericStack> result = new ArrayList<>();
        for (var entry : required.entrySet()) {
            var block = BuiltInRegistries.BLOCK
                    .getOptional(ResourceLocation.parse(entry.getKey().split("\\[")[0]))
                    .orElse(null);
            if (block == null || block == Blocks.AIR) {
                continue;
            }
            var key = AEItemKey.of(block.asItem());
            result.add(new GenericStack(key, (long) entry.getValue()));
        }
        return result;
    }

    public static void onRedstoneActivate(AutoBuilderBE be) {
        if (be.getLevel() == null) {
            return;
        }
        if (be.buffer.isFlushPending()) {
            return;
        }
        if (!be.buffer.getRequestedJobs().isEmpty()) {
            return;
        }

        if (be.inventory.getStackInSlot(0).isEmpty() && !be.inventory.getStackInSlot(1).isEmpty()) {
            be.inventory.setItemDirect(0, be.inventory.getStackInSlot(1).copyWithCount(1));
            be.inventory.setItemDirect(1, ItemStack.EMPTY);
        }

        be.loadCode();
        if (be.code.isEmpty()) {
            if (!be.buffer.isEmpty()) {
                beginFlushBuffer(be);
            }
            return;
        }

        var rawRequired = ProgramExpander.countUsedBlocks(String.join("/", be.code));
        var required = toGenericStackList(rawRequired);

        if (!hasCreativeSupply(be)) {
            be.buffer.setCanCraft(be.upgrades.isInstalled(AEItems.CRAFTING_CARD) && !be.isClientSide());
            be.buffer.collectFromNetwork(required, () -> hasCreativeSupply(be));
            var missing = be.buffer.computeMissing(required, () -> hasCreativeSupply(be));

            if (!missing.isEmpty() && be.buffer.requestCrafting(missing)) {
                be.isCrafting = true;
                be.isRunning = false;
                be.energyPrepaid = false;
                be.setChanged();
                return;
            }

            if (!missing.isEmpty()) {
                be.setMissingItem(missing.get(0));
                if (!be.skipEmpty) {
                    beginFlushBuffer(be);
                    return;
                }
            } else {
                be.clearMissingItem();
                be.isCrafting = false;
            }
        } else {
            be.clearMissingItem();
            be.isCrafting = false;
        }

        recalculateRequiredEnergy(be);

        var node = be.getMainNode();
        if (node.getGrid() == null) {
            be.clearMissingItem();
            be.isCrafting = false;
            be.isRunning = false;
            be.energyPrepaid = false;
            beginFlushBuffer(be);
            return;
        }

        var es = node.getGrid().getEnergyService();
        double can = es.extractAEPower(be.requiredEnergyAE, Actionable.SIMULATE, PowerMultiplier.ONE);
        if (can < be.requiredEnergyAE) {
            be.clearMissingItem();
            be.isCrafting = false;
            be.isRunning = false;
            be.energyPrepaid = false;
            beginFlushBuffer(be);
            return;
        }

        es.extractAEPower(be.requiredEnergyAE, Actionable.MODULATE, PowerMultiplier.ONE);
        be.energyPrepaid = true;

        be.clearMissingItem();
        be.isCrafting = false;
        be.isRunning = true;
        be.currentInstruction = 0;
        be.tickDelayLeft = 0;
        AutoBuilderPreviewOps.resetGhostToHome(be);
        be.setChanged();
    }

    public static void beginFlushBuffer(AutoBuilderBE be) {
        if (be.buffer.isEmpty()) {
            return;
        }
        be.isRunning = false;
        be.isCrafting = false;
        be.energyPrepaid = false;
        be.tickDelayLeft = 0;
        be.buffer.beginFlush();
        AutoBuilderPreviewOps.resetGhostToHome(be);
        be.setChanged();
    }

    public static void recalculateRequiredEnergy(AutoBuilderBE be) {
        if (be.getLevel() == null || be.code == null || be.code.isEmpty()) {
            return;
        }

        be.requiredEnergyAE = 0.0D;
        BlockPos cursor = AutoBuilderPreviewOps.homePos(be);
        for (String inst : be.code) {
            if (inst == null || inst.isEmpty()) {
                continue;
            }
            if (inst.startsWith("Z|")) {
                continue;
            }
            if (inst.equals("H")) {
                cursor = AutoBuilderPreviewOps.homePos(be);
                continue;
            }
            if (inst.length() == 1) {
                char c = inst.charAt(0);
                if (c == 'X') {
                    be.requiredEnergyAE += calcStepCostAE(be, cursor);
                } else if ("FBLRUD".indexOf(c) >= 0) {
                    cursor = AutoBuilderPreviewOps.stepRelative(be, cursor, c);
                }
                continue;
            }
            if (inst.startsWith("P|")) {
                be.requiredEnergyAE += calcStepCostAE(be, cursor);
            } else if (inst.startsWith("PEQ|") || inst.startsWith("PNE|")) {
                be.requiredEnergyAE += calcStepCostAE(be, cursor);
            } else if (inst.startsWith("XEQ|") || inst.startsWith("XNE|")) {
                be.requiredEnergyAE += calcStepCostAE(be, cursor);
            }
        }
    }

    static double calcStepCostAE(AutoBuilderBE be, BlockPos target) {
        double dx = target.getX() - be.getBlockPos().getX();
        double dy = target.getY() - be.getBlockPos().getY();
        double dz = target.getZ() - be.getBlockPos().getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) * CrazyConfig.COMMON.AutobuilderCostMult.get();
    }

    static int stepsFromCards(int cards, int configMax) {
        int c = Mth.clamp(cards, 0, 6);
        int max = Math.max(1, configMax);
        return 1 + ((max - 1) * c) / 6;
    }

    static int calcStepsPerTick(AutoBuilderBE be) {
        int cards = be.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        int maxFromConfig = CrazyConfig.COMMON.AutobuilderSpeed.get();
        return stepsFromCards(cards, maxFromConfig);
    }

    static void triggerRedstonePulse(AutoBuilderBE be) {
        if (be.getLevel() == null || be.getLevel().isClientSide) {
            return;
        }

        be.isPulsing = true;
        be.redstonePulseTicks = 2;
        be.setChanged();
        be.getLevel().updateNeighborsAt(be.getBlockPos(), be.getBlockState().getBlock());
    }

    static boolean hasCreativeSupply(AutoBuilderBE be) {
        var grid = be.getMainNode().getGrid();
        if (grid == null) {
            return false;
        }
        return !grid.getMachines(AutoBuilderCreativeSupplyBE.class).isEmpty();
    }

    static boolean executeBreak(AutoBuilderBE be) {
        var grid = be.getMainNode().getGrid();
        BlockPos pos = be.getGhostRenderPos();

        if (be.getLevel() == null || pos == null) {
            return false;
        }

        boolean didDestroy = false;

        if (grid != null) {
            BlockState state = be.getLevel().getBlockState(pos);
            if (!state.isAir() && BuilderCoordMath.isBreakable(state, be.getLevel(), pos)) {
                var drops = getSilkTouchDrops(state, (ServerLevel) be.getLevel(), pos);
                long inserted = 0;
                for (var drop : drops) {
                    inserted += StorageHelper.poweredInsert(
                            grid.getEnergyService(),
                            grid.getStorageService().getInventory(),
                            AEItemKey.of(drop.getItem()),
                            1,
                            IActionSource.ofMachine(be),
                            Actionable.MODULATE
                    );
                }
                if (inserted > 0 || drops.isEmpty()) {
                    if (be.getLevel().destroyBlock(pos, false)) {
                        didDestroy = true;
                    }
                }
            }

            var fs = be.getLevel().getFluidState(pos);
            if (!fs.isEmpty()) {
                if (fs.isSource()) {
                    StorageHelper.poweredInsert(
                            grid.getEnergyService(),
                            grid.getStorageService().getInventory(),
                            AEFluidKey.of(fs.getType()),
                            1000,
                            IActionSource.ofMachine(be),
                            Actionable.MODULATE
                    );
                }
                if (be.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) {
                    didDestroy = true;
                }
            }
        }

        if (didDestroy) {
            be.tickDelayLeft = Math.max(be.tickDelayLeft, CrazyConfig.COMMON.AutobuilderMineDelay.get());
            be.setChanged();
        }

        return didDestroy;
    }

    static List<ItemStack> getSilkTouchDrops(BlockState state, ServerLevel level, BlockPos pos) {
        ItemStack silkTool = new ItemStack(Items.DIAMOND_PICKAXE);
        var enchReg = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        silkTool.enchant(enchReg.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);

        var lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.TOOL, silkTool)
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .withParameter(LootContextParams.BLOCK_STATE, state);

        return state.getDrops(lootParams);
    }
}