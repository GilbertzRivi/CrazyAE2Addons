package net.oktawia.crazyae2addons.compat.gtceu;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.logic.structuretool.PortableSpatialClonerHost;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolPreviewDispatcher;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStructureStore;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.util.NbtUtil;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PortableSpatialClonerGTCEu extends PortableSpatialCloner {

    public PortableSpatialClonerGTCEu(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected MenuType<?> getToolMenuType() {
        return CrazyMenuRegistrar.PORTABLE_SPATIAL_CLONER_MENU.get();
    }

    @Override
    public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new PortableSpatialClonerHost(player, inventorySlot, stack);
    }

    @Override
    protected boolean collectAdditionalBlockMetadata(
            @Nullable CompoundTag rawBeTag,
            BlockEntity be,
            Player player,
            RequirementSink requirements,
            CompoundTag blockEntry
    ) {
        return GTCEuStructureCaptureCompat.collectAdditionalBlockMetadata(
                rawBeTag,
                be,
                player,
                requirements,
                blockEntry
        );
    }

    @Override
    protected void onUseWithStoredStructure(ServerLevel level, Player player, ItemStack stack) {
        var hit = StructureToolUtil.rayTrace(level, player, 50.0D);

        if (hit.getType() != HitResult.Type.BLOCK) {
            showHud(player, Component.translatable(LangDefs.NO_BLOCK_IN_RANGE.getTranslationKey()));
            return;
        }

        BlockPos pasteOrigin = hit.getBlockPos().relative(hit.getDirection());
        pasteBestEffortGreg(level, player, stack, pasteOrigin);
    }

    @Override
    protected void onUseOnWithStoredStructure(ServerLevel level, Player player, ItemStack stack, BlockPos clickedFacePos) {
        pasteBestEffortGreg(level, player, stack, clickedFacePos);
    }

    private void pasteBestEffortGreg(ServerLevel level, Player player, ItemStack toolStack, BlockPos origin) {
        String id = StructureToolStackState.getStructureId(toolStack);
        if (id.isBlank()) {
            return;
        }

        CompoundTag savedTag;
        try {
            savedTag = StructureToolStructureStore.load(level.getServer(), id);
        } catch (IOException exception) {
            showHud(player, Component.translatable(LangDefs.FAILED_TO_LOAD_STRUCTURE.getTranslationKey()));
            return;
        }

        if (savedTag == null) {
            showHud(player, Component.translatable(LangDefs.STORED_STRUCTURE_NOT_FOUND.getTranslationKey()));
            StructureToolStackState.clearStructure(toolStack);
            StructureToolStackState.clearSelection(toolStack);
            StructureToolStackState.resetPreviewSideMap(toolStack);
            TemplateUtil.setTemplateOffset(toolStack.getOrCreateTag(), BlockPos.ZERO);
            TemplateUtil.setEnergyOrigin(toolStack.getOrCreateTag(), BlockPos.ZERO);

            if (player instanceof ServerPlayer serverPlayer) {
                StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
            }
            return;
        }

        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(savedTag);
        double requiredPower = StructureToolUtil.calculatePreviewStructurePower(
                savedTag,
                energyOrigin,
                POWER_PER_BLOCK_PASTE
        );

        if (!tryUsePower(player, toolStack, requiredPower)) {
            showNotEnoughPower(player, toolStack, requiredPower);
            return;
        }

        BlockPos templateOffset = TemplateUtil.getTemplateOffset(savedTag);
        List<TemplateUtil.BlockInfo> rawBlocks = TemplateUtil.parseRawBlocksFromTag(savedTag);
        Map<BlockPos, CompoundTag> metadataByPos = parseMetadataByPos(savedTag);

        int placed = 0;
        int skipped = 0;

        for (TemplateUtil.BlockInfo blockInfo : rawBlocks) {
            BlockPos localPos = blockInfo.pos();
            BlockPos worldPos = origin
                    .subtract(energyOrigin)
                    .offset(templateOffset)
                    .offset(localPos);

            BlockState originalState = blockInfo.state();
            CompoundTag rawBeTag = blockInfo.blockEntityTag();
            CompoundTag blockMetadata = metadataByPos.get(localPos);

            boolean success;

            if (isAe2CableBusTag(rawBeTag) && !isGregPipeTag(rawBeTag)) {
                BlockState existing = level.getBlockState(worldPos);
                if (hasCollision(existing, originalState)) {
                    skipped++;
                    continue;
                }

                success = super.placeCableBusBestEffort(
                        level,
                        worldPos,
                        originalState,
                        rawBeTag,
                        blockMetadata,
                        player,
                        toolStack
                );
            } else if (!isGregBlockEntityTag(rawBeTag)) {
                BlockState existing = level.getBlockState(worldPos);
                if (hasCollision(existing, originalState)) {
                    skipped++;
                    continue;
                }

                success = super.placeRegularBlockBestEffort(
                        level,
                        worldPos,
                        originalState,
                        rawBeTag,
                        blockMetadata,
                        player,
                        toolStack
                );
            } else {
                CompoundTag gregMeta = getGregMetadata(blockMetadata);
                PlacementPlan plan;

                if (isGregPipeTag(rawBeTag)) {
                    plan = buildGregPipePlacementPlan(level, player, toolStack, originalState, rawBeTag, gregMeta);
                } else {
                    plan = buildGenericGregPlacementPlan(level, player, toolStack, originalState, rawBeTag, gregMeta);
                }

                if (!plan.shouldPlace()) {
                    skipped++;
                    continue;
                }

                BlockState existing = level.getBlockState(worldPos);
                if (hasCollision(existing, plan.stateToPlace())) {
                    skipped++;
                    continue;
                }

                if (!placeBlockAndLoadTag(level, worldPos, plan.stateToPlace(), plan.blockEntityTag())) {
                    skipped++;
                    continue;
                }

                boolean consumedAll = true;
                if (!player.isCreative()) {
                    for (ItemStack cost : plan.consumedStacks()) {
                        if (!consumeForPaste(level, player, toolStack, cost, cost.getCount())) {
                            consumedAll = false;
                            break;
                        }
                    }
                }

                if (!consumedAll) {
                    skipped++;
                    continue;
                }

                scheduleGregPostPlacementInit(level, worldPos, plan.blockEntityTag());
                success = true;
            }

            if (success) {
                placed++;
            } else {
                skipped++;
            }
        }

        if (placed > 0) {
            showHud(
                    player,
                    80,
                    cyan(Component.translatable(LangDefs.STRUCTURE_PASTED.getTranslationKey())),
                    cyan(Component.literal("Placed: " + placed)),
                    red(Component.literal("Skipped: " + skipped))
            );
        } else {
            showHud(
                    player,
                    80,
                    red(Component.translatable(LangDefs.FAILED_TO_PASTE_STRUCTURE.getTranslationKey())),
                    red(Component.literal("Skipped: " + skipped))
            );
        }
    }

    private void scheduleGregPostPlacementInit(ServerLevel level, BlockPos worldPos, @Nullable CompoundTag blockEntityTag) {
        if (blockEntityTag == null) {
            return;
        }

        if (!isGregBlockEntityTag(blockEntityTag)) {
            return;
        }

        GTCEuPasteCompat.scheduleSinglePostPlacementInit(level, worldPos, blockEntityTag);
    }

    private PlacementPlan buildGregPipePlacementPlan(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            BlockState pipeState,
            @Nullable CompoundTag rawBeTag,
            CompoundTag gregMeta
    ) {
        if (rawBeTag == null) {
            return PlacementPlan.none();
        }

        CompoundTag pipeData = gregMeta.getCompound(GTCEuKeys.CLONE_KEY_GREG_PIPE);
        CompoundTag coverData = gregMeta.getCompound(GTCEuKeys.CLONE_KEY_GREG_COVER);

        ItemStack pipeItem = normalizeSingle(getRequiredBlockItem(pipeState));
        String frameMaterial = pipeData.getString("frameMaterial");

        ItemStack frameItem = normalizeSingle(GTCEuUtil.getFrameItem(frameMaterial));
        BlockState frameState = GTCEuUtil.getFrameState(frameMaterial);

        if (player.isCreative()) {
            CompoundTag filteredCover = filterGregCoverForPlacement(level, player, toolStack, coverData, null, true);
            CompoundTag beTag = createWhitelistedGregPipeTag(rawBeTag, pipeData, filteredCover);
            return new PlacementPlan(true, pipeState, beTag, List.of());
        }

        Map<Item, Integer> reserved = new LinkedHashMap<>();
        List<ItemStack> costs = new ArrayList<>();

        boolean canPlacePipe = !pipeItem.isEmpty()
                && canReserveForPaste(level, player, toolStack, reserved, pipeItem, 1);

        if (canPlacePipe) {
            costs.add(pipeItem);

            CompoundTag effectivePipeData = pipeData.copy();

            if (!frameItem.isEmpty()) {
                if (canReserveForPaste(level, player, toolStack, reserved, frameItem, 1)) {
                    costs.add(frameItem);
                } else {
                    effectivePipeData.remove("frameMaterial");
                }
            } else {
                effectivePipeData.remove("frameMaterial");
            }

            CompoundTag filteredCover = filterGregCoverForPlacement(level, player, toolStack, coverData, reserved, false, costs);
            CompoundTag beTag = createWhitelistedGregPipeTag(rawBeTag, effectivePipeData, filteredCover);

            return new PlacementPlan(true, pipeState, beTag, costs);
        }

        if (!frameItem.isEmpty() && frameState != null
                && countAvailableForPaste(level, player, toolStack, frameItem) > 0) {
            return new PlacementPlan(true, frameState, null, List.of(frameItem));
        }

        return PlacementPlan.none();
    }

    private PlacementPlan buildGenericGregPlacementPlan(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            BlockState stateToPlace,
            @Nullable CompoundTag rawBeTag,
            CompoundTag gregMeta
    ) {
        if (rawBeTag == null) {
            return PlacementPlan.none();
        }

        CompoundTag machineData = gregMeta.getCompound(GTCEuKeys.CLONE_KEY_GREG_MACHINE);
        CompoundTag coverData = gregMeta.getCompound(GTCEuKeys.CLONE_KEY_GREG_COVER);

        ItemStack baseItem = normalizeSingle(getRequiredBlockItem(stateToPlace));
        if (baseItem.isEmpty() && !player.isCreative()) {
            return PlacementPlan.none();
        }

        if (player.isCreative()) {
            CompoundTag filteredCover = filterGregCoverForPlacement(level, player, toolStack, coverData, null, true);
            CompoundTag beTag = createWhitelistedGregMachineTag(rawBeTag, machineData, filteredCover);
            return new PlacementPlan(true, stateToPlace, beTag, List.of());
        }

        Map<Item, Integer> reserved = new LinkedHashMap<>();
        if (!canReserveForPaste(level, player, toolStack, reserved, baseItem, 1)) {
            return PlacementPlan.none();
        }

        List<ItemStack> costs = new ArrayList<>();
        costs.add(baseItem);

        CompoundTag filteredCover = filterGregCoverForPlacement(level, player, toolStack, coverData, reserved, false, costs);
        CompoundTag beTag = createWhitelistedGregMachineTag(rawBeTag, machineData, filteredCover);

        return new PlacementPlan(true, stateToPlace, beTag, costs);
    }

    private CompoundTag filterGregCoverForPlacement(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            CompoundTag coverTag,
            @Nullable Map<Item, Integer> reserved,
            boolean creative
    ) {
        return filterGregCoverForPlacement(level, player, toolStack, coverTag, reserved, creative, null);
    }

    private CompoundTag filterGregCoverForPlacement(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            CompoundTag coverTag,
            @Nullable Map<Item, Integer> reserved,
            boolean creative,
            @Nullable List<ItemStack> costs
    ) {
        CompoundTag filteredCover = new CompoundTag();

        if (coverTag == null || coverTag.isEmpty()) {
            return filteredCover;
        }

        for (String sideKey : coverTag.getAllKeys()) {
            Tag sideTag = coverTag.get(sideKey);
            if (sideTag == null) {
                continue;
            }

            List<ItemStack> attachItems = new ArrayList<>();
            GTCEuUtil.collectGregAttachItems(sideTag, item -> attachItems.add(normalizeSingle(item)));

            boolean keepSide = true;

            if (!creative && !attachItems.isEmpty()) {
                for (ItemStack attachItem : attachItems) {
                    if (!canReserveForPaste(level, player, toolStack, reserved, attachItem, 1)) {
                        keepSide = false;
                        break;
                    }
                }
            }

            if (keepSide) {
                filteredCover.put(sideKey, sideTag.copy());

                if (!creative && costs != null) {
                    costs.addAll(attachItems);
                }
            }
        }

        return filteredCover;
    }

    private boolean isGregBlockEntityTag(@Nullable CompoundTag tag) {
        if (tag == null) {
            return false;
        }

        String id = tag.getString("id");
        return !id.isBlank() && id.startsWith(GTCEuKeys.GTCEU_ID_PREFIX);
    }

    private boolean isGregPipeTag(@Nullable CompoundTag tag) {
        if (tag == null) {
            return false;
        }

        String id = tag.getString("id");
        return GTCEuKeys.GT_FLUID_PIPE_ID.equals(id)
                || GTCEuKeys.GT_ITEM_PIPE_ID.equals(id)
                || GTCEuKeys.GT_CABLE_ID.equals(id);
    }

    private ItemStack normalizeSingle(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        copy.setTag(null);
        return copy;
    }

    private CompoundTag getGregMetadata(@Nullable CompoundTag blockMetadata) {
        if (blockMetadata == null) {
            return new CompoundTag();
        }

        if (!blockMetadata.contains(GTCEuKeys.CLONE_KEY_GREG, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }

        return blockMetadata.getCompound(GTCEuKeys.CLONE_KEY_GREG);
    }

    private CompoundTag createWhitelistedGregPipeTag(
            CompoundTag rawBeTag,
            CompoundTag pipeData,
            CompoundTag filteredCover
    ) {
        CompoundTag out = new CompoundTag();

        NbtUtil.copyStringIfPresent(rawBeTag, out, "id");
        NbtUtil.copyIntIfPresent(pipeData, out, "connections");
        NbtUtil.copyIntIfPresent(pipeData, out, "blockedConnections");
        NbtUtil.copyIntIfPresent(pipeData, out, "paintingColor");
        NbtUtil.copyStringIfPresent(pipeData, out, "frameMaterial");

        if (!filteredCover.isEmpty()) {
            out.put("cover", filteredCover.copy());
        }

        return out;
    }

    private CompoundTag createWhitelistedGregMachineTag(
            CompoundTag rawBeTag,
            CompoundTag machineData,
            CompoundTag filteredCover
    ) {
        CompoundTag out = new CompoundTag();

        NbtUtil.copyStringIfPresent(rawBeTag, out, "id");
        NbtUtil.copyTagIfPresent(machineData, out, "ownerUUID");
        NbtUtil.copyStringIfPresent(machineData, out, "workingMode");
        NbtUtil.copyStringIfPresent(machineData, out, "voidingMode");
        NbtUtil.copyByteIfPresent(machineData, out, "batchEnabled");
        NbtUtil.copyByteIfPresent(machineData, out, "isWorkingEnabled");
        NbtUtil.copyByteIfPresent(machineData, out, "workingEnabled");
        NbtUtil.copyByteIfPresent(machineData, out, "isMuffled");
        NbtUtil.copyByteIfPresent(machineData, out, "isDistinct");
        NbtUtil.copyIntIfPresent(machineData, out, "paintingColor");
        NbtUtil.copyIntIfPresent(machineData, out, "currentParallel");
        NbtUtil.copyTagIfPresent(machineData, out, "circuitInventory");

        if (machineData.contains("recipeLogic", Tag.TAG_COMPOUND)) {
            out.put("recipeLogic", machineData.getCompound("recipeLogic").copy());
        }

        if (machineData.contains("dataStick", Tag.TAG_COMPOUND)) {
            out.put("dataStick", machineData.getCompound("dataStick").copy());
        }

        if (!filteredCover.isEmpty()) {
            out.put("cover", filteredCover.copy());
        }

        return out;
    }

    private record PlacementPlan(
            boolean shouldPlace,
            @Nullable BlockState stateToPlace,
            @Nullable CompoundTag blockEntityTag,
            List<ItemStack> consumedStacks
    ) {
        private static PlacementPlan none() {
            return new PlacementPlan(false, null, null, List.of());
        }
    }
}
