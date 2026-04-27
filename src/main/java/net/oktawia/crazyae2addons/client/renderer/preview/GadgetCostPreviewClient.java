package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.structuretool.AbstractStructureCaptureToolItem;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.util.TemplateUtil;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GadgetCostPreviewClient {

    private static final int COLOR_OK = 0xFFFFFF;
    private static final int COLOR_CYAN = 0x55FFFF;
    private static final int COLOR_RED = 0xFF4040;
    private static final double POWER_PER_BLOCK_CAPTURE = 1.0D;

    private static final SelectionCostCache CAPTURE_COST_CACHE = new SelectionCostCache();

    private static boolean pasteCostCacheValid = false;
    private static String pasteCostStructureId = "";
    private static PreviewStructure pasteCostStructure = null;
    private static BlockPos pasteCostEnergyOrigin = BlockPos.ZERO;
    private static long pasteCostValue = 0L;

    private static Component currentText = null;
    private static int currentColor = COLOR_OK;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        currentText = null;
        currentColor = COLOR_CYAN;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            resetCaches();
            return;
        }

        ItemStack held = StructureToolUtil.findHeld(
                mc.player,
                PortableSpatialStorage.class,
                PortableSpatialCloner.class
        );

        if (held.isEmpty() || !(held.getItem() instanceof AbstractStructureCaptureToolItem tool)) {
            resetCaches();
            return;
        }

        int energy = (int) Math.floor(tool.getAECurrentPower(held));

        if (StructureToolStackState.hasStructure(held)) {
            CAPTURE_COST_CACHE.invalidate();

            BlockHitResult hit = StructureToolUtil.rayTrace(mc.level, mc.player, 50.0D);
            if (hit.getType() != HitResult.Type.BLOCK) {
                return;
            }

            long cost = computePastePreviewCostAE(held);
            if (cost <= 0) {
                return;
            }

            currentText = Component.translatable(
                    LangDefs.PASTE_COST_PREVIEW.getTranslationKey(),
                    String.format("%,d", cost)
            );
            currentColor = cost > energy ? COLOR_RED : COLOR_CYAN;
            return;
        }

        invalidatePasteCache();

        BlockHitResult hit = StructureToolUtil.rayTrace(mc.level, mc.player, 50.0D);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos selectionA = StructureToolStackState.getSelectionA(held);
        if (selectionA == null) {
            CAPTURE_COST_CACHE.invalidate();
            return;
        }

        BlockPos selectionB = StructureToolStackState.getSelectionB(held);
        BlockPos previewB = selectionB == null ? hit.getBlockPos().immutable() : selectionB.immutable();

        long cost = CAPTURE_COST_CACHE.getOrUpdate(
                mc.level.dimension(),
                mc.level,
                selectionA.immutable(),
                previewB,
                selectionA.immutable(),
                POWER_PER_BLOCK_CAPTURE
        );

        currentText = Component.translatable(
                LangDefs.CUT_COST_PREVIEW.getTranslationKey(),
                String.format("%,d", cost)
        );
        currentColor = cost > energy ? COLOR_RED : COLOR_CYAN;
    }

    private static long computePastePreviewCostAE(ItemStack stack) {
        String structureId = StructureToolStackState.getStructureId(stack);
        if (structureId == null || structureId.isBlank()) {
            invalidatePasteCache();
            return 0L;
        }

        PreviewStructure structure = PortableSpatialStoragePreviewSync.cacheGet(structureId);
        if (structure == null || structure.blocks().isEmpty()) {
            invalidatePasteCache();
            return 0L;
        }

        CompoundTag tag = stack.getTag();
        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(tag);

        if (pasteCostCacheValid
                && structureId.equals(pasteCostStructureId)
                && structure == pasteCostStructure
                && energyOrigin.equals(pasteCostEnergyOrigin)) {
            return pasteCostValue;
        }

        double total = 0.0D;

        for (PreviewBlock previewBlock : structure.blocks()) {
            BlockPos pos = previewBlock.pos();

            double dx = pos.getX() - energyOrigin.getX();
            double dy = pos.getY() - energyOrigin.getY();
            double dz = pos.getZ() - energyOrigin.getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            total += distance;
        }

        long cost = ceilToLongClamped(Math.max(1.0D, total));

        pasteCostCacheValid = true;
        pasteCostStructureId = structureId;
        pasteCostStructure = structure;
        pasteCostEnergyOrigin = energyOrigin.immutable();
        pasteCostValue = cost;

        return cost;
    }

    private static void resetCaches() {
        CAPTURE_COST_CACHE.invalidate();
        invalidatePasteCache();
    }

    private static void invalidatePasteCache() {
        pasteCostCacheValid = false;
        pasteCostStructureId = "";
        pasteCostStructure = null;
        pasteCostEnergyOrigin = BlockPos.ZERO;
        pasteCostValue = 0L;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (currentText == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 + 8;
        int y = screenHeight / 2 - 4;

        gui.drawString(mc.font, currentText, x, y, currentColor, true);
    }

    private static long blockCaptureCostContribution(
            BlockPos pos,
            BlockPos energyOrigin,
            double powerPerBlock
    ) {
        double dx = pos.getX() - energyOrigin.getX();
        double dy = pos.getY() - energyOrigin.getY();
        double dz = pos.getZ() - energyOrigin.getZ();

        return Double.doubleToRawLongBits(Math.sqrt(dx * dx + dy * dy + dz * dz) * powerPerBlock);
    }

    private static double unpackCost(long packedDouble) {
        return Double.longBitsToDouble(packedDouble);
    }

    private static long ceilToLongClamped(double value) {
        if (Double.isNaN(value) || value <= 0.0D) {
            return 0L;
        }

        if (value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return (long) Math.ceil(value);
    }

    private static final class SelectionCostCache {

        private boolean valid = false;

        private ResourceKey<Level> dimension = null;
        private BlockPos selectionA = BlockPos.ZERO;
        private BlockPos energyOrigin = BlockPos.ZERO;
        private CuboidBounds bounds = null;
        private double powerPerBlock = POWER_PER_BLOCK_CAPTURE;

        private long nonAirBlocks = 0L;
        private double totalCost = 0.0D;

        private void invalidate() {
            valid = false;
            dimension = null;
            selectionA = BlockPos.ZERO;
            energyOrigin = BlockPos.ZERO;
            bounds = null;
            powerPerBlock = POWER_PER_BLOCK_CAPTURE;
            nonAirBlocks = 0L;
            totalCost = 0.0D;
        }

        private long getOrUpdate(
                ResourceKey<Level> newDimension,
                Level level,
                BlockPos newSelectionA,
                BlockPos newSelectionB,
                BlockPos newEnergyOrigin,
                double newPowerPerBlock
        ) {
            CuboidBounds newBounds = CuboidBounds.from(newSelectionA, newSelectionB);

            if (!valid
                    || dimension == null
                    || !dimension.equals(newDimension)
                    || !selectionA.equals(newSelectionA)
                    || !energyOrigin.equals(newEnergyOrigin)
                    || Double.compare(powerPerBlock, newPowerPerBlock) != 0
                    || bounds == null) {
                fullRecompute(
                        newDimension,
                        level,
                        newSelectionA,
                        newEnergyOrigin,
                        newPowerPerBlock,
                        newBounds
                );

                return toAeCost();
            }

            if (bounds.equals(newBounds)) {
                return toAeCost();
            }

            CountResult added = countDifference(
                    level,
                    newBounds,
                    bounds,
                    energyOrigin,
                    powerPerBlock
            );

            CountResult removed = countDifference(
                    level,
                    bounds,
                    newBounds,
                    energyOrigin,
                    powerPerBlock
            );

            nonAirBlocks += added.nonAirBlocks;
            nonAirBlocks -= removed.nonAirBlocks;

            totalCost += added.totalCost;
            totalCost -= removed.totalCost;

            if (nonAirBlocks < 0L) {
                nonAirBlocks = 0L;
            }

            if (totalCost < 0.0D || nonAirBlocks == 0L) {
                totalCost = 0.0D;
            }

            bounds = newBounds;

            return toAeCost();
        }

        private void fullRecompute(
                ResourceKey<Level> newDimension,
                Level level,
                BlockPos newSelectionA,
                BlockPos newEnergyOrigin,
                double newPowerPerBlock,
                CuboidBounds newBounds
        ) {
            CountResult result = countCuboid(
                    level,
                    newBounds,
                    newEnergyOrigin,
                    newPowerPerBlock
            );

            valid = true;
            dimension = newDimension;
            selectionA = newSelectionA.immutable();
            energyOrigin = newEnergyOrigin.immutable();
            powerPerBlock = newPowerPerBlock;
            bounds = newBounds;

            nonAirBlocks = result.nonAirBlocks;
            totalCost = result.totalCost;
        }

        private long toAeCost() {
            if (!valid || nonAirBlocks <= 0L) {
                return 0L;
            }

            return ceilToLongClamped(Math.max(1.0D, totalCost));
        }
    }

    private static CountResult countDifference(
            Level level,
            CuboidBounds include,
            CuboidBounds exclude,
            BlockPos energyOrigin,
            double powerPerBlock
    ) {
        CountResult result = new CountResult();

        forEachDifference(
                include,
                exclude,
                cuboid -> result.add(countCuboid(
                        level,
                        cuboid,
                        energyOrigin,
                        powerPerBlock
                ))
        );

        return result;
    }

    private static CountResult countCuboid(
            Level level,
            CuboidBounds bounds,
            BlockPos energyOrigin,
            double powerPerBlock
    ) {
        CountResult result = new CountResult();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                    mutablePos.set(x, y, z);

                    BlockState state = level.getBlockState(mutablePos);
                    if (state.isAir()) {
                        continue;
                    }

                    result.nonAirBlocks++;
                    result.totalCost += unpackCost(blockCaptureCostContribution(
                            mutablePos,
                            energyOrigin,
                            powerPerBlock
                    ));
                }
            }
        }

        return result;
    }

    private static void forEachDifference(
            CuboidBounds source,
            CuboidBounds subtract,
            CuboidConsumer consumer
    ) {
        CuboidBounds intersection = source.intersection(subtract);

        if (intersection == null) {
            consumer.accept(source);
            return;
        }

        if (source.minX() < intersection.minX()) {
            consumer.accept(new CuboidBounds(
                    source.minX(),
                    source.minY(),
                    source.minZ(),
                    intersection.minX() - 1,
                    source.maxY(),
                    source.maxZ()
            ));
        }

        if (source.maxX() > intersection.maxX()) {
            consumer.accept(new CuboidBounds(
                    intersection.maxX() + 1,
                    source.minY(),
                    source.minZ(),
                    source.maxX(),
                    source.maxY(),
                    source.maxZ()
            ));
        }

        int midMinX = intersection.minX();
        int midMaxX = intersection.maxX();

        if (source.minY() < intersection.minY()) {
            consumer.accept(new CuboidBounds(
                    midMinX,
                    source.minY(),
                    source.minZ(),
                    midMaxX,
                    intersection.minY() - 1,
                    source.maxZ()
            ));
        }

        if (source.maxY() > intersection.maxY()) {
            consumer.accept(new CuboidBounds(
                    midMinX,
                    intersection.maxY() + 1,
                    source.minZ(),
                    midMaxX,
                    source.maxY(),
                    source.maxZ()
            ));
        }

        int midMinY = intersection.minY();
        int midMaxY = intersection.maxY();

        if (source.minZ() < intersection.minZ()) {
            consumer.accept(new CuboidBounds(
                    midMinX,
                    midMinY,
                    source.minZ(),
                    midMaxX,
                    midMaxY,
                    intersection.minZ() - 1
            ));
        }

        if (source.maxZ() > intersection.maxZ()) {
            consumer.accept(new CuboidBounds(
                    midMinX,
                    midMinY,
                    intersection.maxZ() + 1,
                    midMaxX,
                    midMaxY,
                    source.maxZ()
            ));
        }
    }

    private interface CuboidConsumer {
        void accept(CuboidBounds bounds);
    }

    private static final class CountResult {
        private long nonAirBlocks;
        private double totalCost;

        private void add(CountResult other) {
            this.nonAirBlocks += other.nonAirBlocks;
            this.totalCost += other.totalCost;
        }
    }

    private record CuboidBounds(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        private static CuboidBounds from(BlockPos a, BlockPos b) {
            return new CuboidBounds(
                    Math.min(a.getX(), b.getX()),
                    Math.min(a.getY(), b.getY()),
                    Math.min(a.getZ(), b.getZ()),
                    Math.max(a.getX(), b.getX()),
                    Math.max(a.getY(), b.getY()),
                    Math.max(a.getZ(), b.getZ())
            );
        }

        private CuboidBounds intersection(CuboidBounds other) {
            int ix1 = Math.max(minX, other.minX);
            int iy1 = Math.max(minY, other.minY);
            int iz1 = Math.max(minZ, other.minZ);

            int ix2 = Math.min(maxX, other.maxX);
            int iy2 = Math.min(maxY, other.maxY);
            int iz2 = Math.min(maxZ, other.maxZ);

            if (ix1 > ix2 || iy1 > iy2 || iz1 > iz2) {
                return null;
            }

            return new CuboidBounds(ix1, iy1, iz1, ix2, iy2, iz2);
        }
    }
}