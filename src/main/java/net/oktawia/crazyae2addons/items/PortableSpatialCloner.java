package net.oktawia.crazyae2addons.items;

import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.structuretool.*;
import net.oktawia.crazyae2addons.util.StructureToolKeys;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PortableSpatialCloner extends AbstractStructureCaptureToolItem {

    private static final String CLONER_UNDO_DIMENSION_KEY = "clonerUndoDimension";
    private static final String CLONER_UNDO_BLOCKS_KEY = "clonerUndoBlocks";

    private static final String CLONER_UNDO_POS_X_KEY = "x";
    private static final String CLONER_UNDO_POS_Y_KEY = "y";
    private static final String CLONER_UNDO_POS_Z_KEY = "z";
    private static final String CLONER_UNDO_STATE_KEY = "state";

    private static final String CLONER_UNDO_REFUNDS_KEY = "refunds";
    private static final String CLONER_UNDO_REFUND_STACK_KEY = "stack";
    private static final String CLONER_UNDO_REFUND_COUNT_KEY = "count";

    private static final int CLONER_UNDO_CLEAR_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public PortableSpatialCloner(Item.Properties properties) {
        super(
                CrazyConfig.COMMON.PORTABLE_SPATIAL_CLONER_BASE_INTERNAL_POWER_CAPACITY::get,
                DEFAULT_UPGRADE_SLOTS + 1,
                properties
        );
    }

    public static ItemStack findHeld(@Nullable Player player) {
        return StructureToolUtil.findHeld(player, PortableSpatialCloner.class);
    }

    public static ItemStack findActive(@Nullable Player player) {
        return StructureToolUtil.findActive(player, PortableSpatialCloner.class);
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
    protected boolean removeCapturedBlocks() {
        return false;
    }

    @Override
    protected Component getCaptureSuccessMessage() {
        return Component.translatable(LangDefs.STRUCTURE_COPIED_AND_SAVED.getTranslationKey());
    }

    @Override
    protected Component getStoredStructureActionNotImplementedMessage() {
        return Component.translatable(LangDefs.STRUCTURE_PASTED.getTranslationKey());
    }

    @Override
    protected boolean isToolEnabled() {
        return CrazyConfig.COMMON.PORTABLE_SPATIAL_CLONER_ENABLED.get();
    }

    @Override
    protected double getPowerPerBlockCapture() {
        return CrazyConfig.COMMON.PORTABLE_SPATIAL_CLONER_COST.get();
    }

    @Override
    protected double getPowerPerBlockPaste() {
        return CrazyConfig.COMMON.PORTABLE_SPATIAL_CLONER_COST.get();
    }

    @Override
    protected int getMaxStructureSize() {
        return CrazyConfig.COMMON.PORTABLE_SPATIAL_CLONER_MAX_STRUCTURE_SIZE.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND && isToolEnabled()) {
            if (!level.isClientSide()) {
                undoLastClonerPaste((ServerLevel) level, player, stack);
            }

            return InteractionResultHolder.success(stack);
        }

        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (context.getHand() == InteractionHand.OFF_HAND && isToolEnabled() && player != null) {
            if (!level.isClientSide()) {
                undoLastClonerPaste((ServerLevel) level, player, context.getItemInHand());
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    @Override
    protected void onUseWithStoredStructure(ServerLevel level, Player player, ItemStack stack) {
        var hit = StructureToolUtil.rayTrace(level, player, 50.0D);

        if (hit.getType() != HitResult.Type.BLOCK) {
            showHud(player, Component.translatable(LangDefs.NO_BLOCK_IN_RANGE.getTranslationKey()));
            return;
        }

        BlockPos pasteOrigin = hit.getBlockPos().relative(hit.getDirection());
        pasteBestEffort(level, player, stack, pasteOrigin);
    }

    @Override
    protected void onUseOnWithStoredStructure(ServerLevel level, Player player, ItemStack stack, BlockPos clickedFacePos) {
        pasteBestEffort(level, player, stack, clickedFacePos);
    }

    protected void pasteBestEffort(ServerLevel level, Player player, ItemStack toolStack, BlockPos origin) {
        String id = StructureToolStackState.getStructureId(toolStack);

        if (id.isBlank()) {
            return;
        }

        CompoundTag savedTag;

        try {
            savedTag = ClonerStructureLibraryStore.loadSelectedOrMigrateLegacy(
                    level.getServer(),
                    player.getUUID(),
                    toolStack
            );
        } catch (IOException exception) {
            showHud(player, Component.translatable(LangDefs.FAILED_TO_LOAD_STRUCTURE.getTranslationKey()));
            return;
        }

        if (savedTag == null) {
            showHud(player, Component.translatable(LangDefs.STORED_STRUCTURE_NOT_FOUND.getTranslationKey()));

            StructureToolStackState.clearSelectedClonerLibraryEntry(toolStack);
            StructureToolStackState.clearSelection(toolStack);
            StructureToolStackState.resetPreviewSideMap(toolStack);

            TemplateUtil.setTemplateOffset(toolStack.getOrCreateTag(), BlockPos.ZERO);
            TemplateUtil.setEnergyOrigin(toolStack.getOrCreateTag(), BlockPos.ZERO);

            if (player instanceof ServerPlayer serverPlayer) {
                StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
            }

            return;
        }

        if (isTemplateEmpty(savedTag)) {
            clearStoredStructure(level, player, toolStack, id);

            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_STORED_STRUCTURE_EMPTY.getTranslationKey())),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_INVALID_STRUCTURE_CLEARED.getTranslationKey()))
            );

            return;
        }

        if (!checkStructureSizeLimit(player, savedTag)) {
            return;
        }

        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(savedTag);
        double requiredPower = StructureToolUtil.calculatePreviewStructurePower(
                savedTag,
                energyOrigin,
                getPowerPerBlockPaste()
        );

        if (!tryUsePower(player, toolStack, requiredPower)) {
            showNotEnoughPower(player, toolStack, requiredPower);
            return;
        }

        BlockPos templateOffset = TemplateUtil.getTemplateOffset(savedTag);
        List<TemplateUtil.BlockInfo> rawBlocks = TemplateUtil.parseRawBlocksFromTag(savedTag);
        Map<BlockPos, CompoundTag> metadataByPos = parseMetadataByPos(savedTag);

        List<ClonerUndoPlacedBlock> undoPlacedBlocks = new ArrayList<>();

        int placed = 0;
        int skipped = 0;

        for (TemplateUtil.BlockInfo blockInfo : rawBlocks) {
            BlockPos localPos = blockInfo.pos();

            BlockPos worldPos = origin
                    .subtract(energyOrigin)
                    .offset(templateOffset)
                    .offset(localPos);

            BlockState stateToPlace = blockInfo.state();
            CompoundTag rawBeTag = blockInfo.blockEntityTag();
            CompoundTag blockMetadata = metadataByPos.get(localPos);

            List<ItemStack> refundStacks = new ArrayList<>();
            ClonerPasteContext pasteContext = createTrackingPasteContext(
                    level,
                    player,
                    toolStack,
                    refundStacks
            );

            PlacementPlan selectedPlan = null;

            for (StructureCloneExtension extension : StructureToolExtensions.clonerExtensions()) {
                Optional<PlacementPlan> extensionPlan = extension.buildPlacementPlan(
                        level,
                        player,
                        stateToPlace,
                        rawBeTag,
                        blockMetadata,
                        pasteContext
                );

                if (extensionPlan.isPresent()) {
                    selectedPlan = extensionPlan.get();
                    break;
                }
            }

            boolean success;

            if (selectedPlan != null) {
                success = placePlannedBlockBestEffort(
                        level,
                        worldPos,
                        selectedPlan,
                        player,
                        toolStack
                );

                if (success) {
                    for (ItemStack cost : selectedPlan.consumedStacks()) {
                        if (!cost.isEmpty()) {
                            refundStacks.add(cost.copy());
                        }
                    }
                }
            } else {
                success = placeRegularBlockBestEffort(
                        level,
                        worldPos,
                        stateToPlace,
                        rawBeTag,
                        blockMetadata,
                        player,
                        toolStack
                );

                if (success) {
                    ItemStack required = getRequiredBlockItem(stateToPlace);

                    if (!required.isEmpty()) {
                        refundStacks.add(required.copy());
                    }
                }
            }

            if (success) {
                BlockEntity placedBlockEntity = level.getBlockEntity(worldPos);

                for (StructureCloneExtension extension : StructureToolExtensions.clonerExtensions()) {
                    extension.onBlockPlaced(level, worldPos, placedBlockEntity, blockMetadata);
                }

                BlockState finalState = level.getBlockState(worldPos);

                undoPlacedBlocks.add(new ClonerUndoPlacedBlock(
                        worldPos.immutable(),
                        finalState.toString(),
                        List.copyOf(aggregateRefundStacks(refundStacks))
                ));

                placed++;
            } else {
                if (!refundStacks.isEmpty()) {
                    refundStacksToAeThenInventory(level, player, toolStack, refundStacks);
                }

                skipped++;
            }
        }

        if (placed > 0) {
            storeClonerUndoPaste(toolStack, level, undoPlacedBlocks);

            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    cyan(Component.translatable(LangDefs.STRUCTURE_PASTED.getTranslationKey())),
                    skipped > 0
                            ? red(Component.translatable(LangDefs.STRUCTURE_GADGET_PLACED_SKIPPED.getTranslationKey(), placed, skipped))
                            : cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_PLACED_SKIPPED.getTranslationKey(), placed, skipped)),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_HINT.getTranslationKey()))
            );
        } else {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.FAILED_TO_PASTE_STRUCTURE.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_SKIPPED.getTranslationKey(), skipped))
            );
        }
    }

    private ClonerPasteContext createTrackingPasteContext(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            List<ItemStack> consumedStacks
    ) {
        return new TrackingPasteContext(level, player, toolStack, consumedStacks);
    }

    private void undoLastClonerPaste(ServerLevel level, Player player, ItemStack toolStack) {
        CompoundTag tag = toolStack.getTag();

        if (tag == null || !tag.contains(CLONER_UNDO_BLOCKS_KEY, Tag.TAG_LIST)) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_NOTHING_TO_UNDO.getTranslationKey()))
            );
            return;
        }

        String undoDimension = tag.getString(CLONER_UNDO_DIMENSION_KEY);
        String currentDimension = level.dimension().location().toString();

        if (!currentDimension.equals(undoDimension)) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_OTHER_DIMENSION.getTranslationKey()))
            );
            return;
        }

        List<ClonerUndoPlacedBlock> undoBlocks = readClonerUndoPaste(toolStack);

        if (undoBlocks.isEmpty()) {
            clearClonerUndoPaste(toolStack);

            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_NOTHING_PLACED.getTranslationKey()))
            );
            return;
        }

        if (!areUndoPlacedBlocksUnchanged(level, undoBlocks)) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_WORLD_CHANGED.getTranslationKey()))
            );
            return;
        }

        List<ItemStack> refundStacks = collectRefundStacks(undoBlocks);
        boolean shouldRefundItems = !player.isCreative() && !refundStacks.isEmpty();

        if (shouldRefundItems && !canStoreRefundStacks(level, player, toolStack, refundStacks)) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_NO_SPACE.getTranslationKey()))
            );
            return;
        }

        removeUndoPlacedBlocks(level, undoBlocks);

        ClonerRefundResult refundResult = ClonerRefundResult.success(false);

        if (shouldRefundItems) {
            refundResult = refundStacksToAeThenInventory(level, player, toolStack, refundStacks);

            if (!refundResult.success()) {
                showHud(
                        player,
                        HUD_TIME_MEDIUM,
                        red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                        red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_NO_SPACE.getTranslationKey()))
                );
                return;
            }
        }

        clearClonerUndoPaste(toolStack);

        if (shouldRefundItems) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_COPY_PASTE_UNDONE.getTranslationKey())),
                    cyan(Component.translatable(
                            refundResult.insertedIntoMe()
                                    ? LangDefs.STRUCTURE_GADGET_ITEMS_REFUNDED_TO_ME.getTranslationKey()
                                    : LangDefs.STRUCTURE_GADGET_ITEMS_REFUNDED.getTranslationKey()
                    ))
            );
        } else {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_COPY_PASTE_UNDONE.getTranslationKey()))
            );
        }
    }

    private void storeClonerUndoPaste(
            ItemStack toolStack,
            ServerLevel level,
            List<ClonerUndoPlacedBlock> placedBlocks
    ) {
        CompoundTag tag = toolStack.getOrCreateTag();

        tag.putString(CLONER_UNDO_DIMENSION_KEY, level.dimension().location().toString());

        ListTag blocksTag = new ListTag();

        for (ClonerUndoPlacedBlock placedBlock : placedBlocks) {
            CompoundTag blockTag = new CompoundTag();

            blockTag.putInt(CLONER_UNDO_POS_X_KEY, placedBlock.pos().getX());
            blockTag.putInt(CLONER_UNDO_POS_Y_KEY, placedBlock.pos().getY());
            blockTag.putInt(CLONER_UNDO_POS_Z_KEY, placedBlock.pos().getZ());
            blockTag.putString(CLONER_UNDO_STATE_KEY, placedBlock.stateSignature());

            ListTag refundList = new ListTag();

            for (ItemStack refundStack : placedBlock.refundStacks()) {
                if (refundStack.isEmpty()) {
                    continue;
                }

                CompoundTag refundEntry = new CompoundTag();
                ItemStack single = refundStack.copy();

                int count = Math.max(1, refundStack.getCount());
                single.setCount(1);

                refundEntry.put(CLONER_UNDO_REFUND_STACK_KEY, single.save(new CompoundTag()));
                refundEntry.putInt(CLONER_UNDO_REFUND_COUNT_KEY, count);

                refundList.add(refundEntry);
            }

            blockTag.put(CLONER_UNDO_REFUNDS_KEY, refundList);
            blocksTag.add(blockTag);
        }

        tag.put(CLONER_UNDO_BLOCKS_KEY, blocksTag);
    }

    private List<ClonerUndoPlacedBlock> readClonerUndoPaste(ItemStack toolStack) {
        CompoundTag tag = toolStack.getTag();

        if (tag == null || !tag.contains(CLONER_UNDO_BLOCKS_KEY, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag blocksTag = tag.getList(CLONER_UNDO_BLOCKS_KEY, Tag.TAG_COMPOUND);
        List<ClonerUndoPlacedBlock> out = new ArrayList<>();

        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);

            BlockPos pos = new BlockPos(
                    blockTag.getInt(CLONER_UNDO_POS_X_KEY),
                    blockTag.getInt(CLONER_UNDO_POS_Y_KEY),
                    blockTag.getInt(CLONER_UNDO_POS_Z_KEY)
            );

            String stateSignature = blockTag.getString(CLONER_UNDO_STATE_KEY);
            List<ItemStack> refundStacks = new ArrayList<>();

            if (blockTag.contains(CLONER_UNDO_REFUNDS_KEY, Tag.TAG_LIST)) {
                ListTag refundList = blockTag.getList(CLONER_UNDO_REFUNDS_KEY, Tag.TAG_COMPOUND);

                for (int j = 0; j < refundList.size(); j++) {
                    CompoundTag refundEntry = refundList.getCompound(j);

                    if (!refundEntry.contains(CLONER_UNDO_REFUND_STACK_KEY, Tag.TAG_COMPOUND)) {
                        continue;
                    }

                    ItemStack stack = ItemStack.of(refundEntry.getCompound(CLONER_UNDO_REFUND_STACK_KEY));
                    int count = Math.max(1, refundEntry.getInt(CLONER_UNDO_REFUND_COUNT_KEY));

                    if (!stack.isEmpty()) {
                        stack.setCount(count);
                        refundStacks.add(stack);
                    }
                }
            }

            out.add(new ClonerUndoPlacedBlock(
                    pos,
                    stateSignature,
                    List.copyOf(refundStacks)
            ));
        }

        return out;
    }

    private void clearClonerUndoPaste(ItemStack toolStack) {
        CompoundTag tag = toolStack.getTag();

        if (tag == null) {
            return;
        }

        tag.remove(CLONER_UNDO_DIMENSION_KEY);
        tag.remove(CLONER_UNDO_BLOCKS_KEY);
    }

    private boolean areUndoPlacedBlocksUnchanged(
            ServerLevel level,
            List<ClonerUndoPlacedBlock> undoBlocks
    ) {
        for (ClonerUndoPlacedBlock undoBlock : undoBlocks) {
            BlockState currentState = level.getBlockState(undoBlock.pos());

            if (!currentState.toString().equals(undoBlock.stateSignature())) {
                return false;
            }
        }

        return true;
    }

    private void removeUndoPlacedBlocks(
            ServerLevel level,
            List<ClonerUndoPlacedBlock> undoBlocks
    ) {
        BlockState air = Blocks.AIR.defaultBlockState();

        for (ClonerUndoPlacedBlock undoBlock : undoBlocks) {
            level.removeBlockEntity(undoBlock.pos());
        }

        for (ClonerUndoPlacedBlock undoBlock : undoBlocks) {
            if (level.getBlockState(undoBlock.pos()).isAir()) {
                continue;
            }

            level.setBlock(
                    undoBlock.pos(),
                    air,
                    CLONER_UNDO_CLEAR_FLAGS,
                    0
            );
        }
    }

    private List<ItemStack> collectRefundStacks(List<ClonerUndoPlacedBlock> undoBlocks) {
        List<ItemStack> out = new ArrayList<>();

        for (ClonerUndoPlacedBlock undoBlock : undoBlocks) {
            for (ItemStack stack : undoBlock.refundStacks()) {
                if (!stack.isEmpty()) {
                    out.add(stack.copy());
                }
            }
        }

        return aggregateRefundStacks(out);
    }

    private List<ItemStack> aggregateRefundStacks(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>();

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack copy = stack.copy();
            boolean merged = false;

            for (ItemStack existing : out) {
                if (ItemStack.isSameItemSameTags(existing, copy)) {
                    existing.grow(copy.getCount());
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                out.add(copy);
            }
        }

        return out;
    }

    private boolean canStoreRefundStacks(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            List<ItemStack> refundStacks
    ) {
        List<ItemStack> inventoryRemainders = new ArrayList<>();

        for (ItemStack refundStack : aggregateRefundStacks(refundStacks)) {
            int count = refundStack.getCount();
            long inserted = simulateInsertIntoMe(level, player, toolStack, refundStack, count);

            int remaining = count - (int) Math.min(count, inserted);

            if (remaining > 0) {
                ItemStack remainder = refundStack.copy();
                remainder.setCount(remaining);
                inventoryRemainders.add(remainder);
            }
        }

        return canFitInInventory(player, inventoryRemainders);
    }

    private ClonerRefundResult refundStacksToAeThenInventory(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            List<ItemStack> refundStacks
    ) {
        boolean insertedIntoMe = false;

        for (ItemStack refundStack : aggregateRefundStacks(refundStacks)) {
            int count = refundStack.getCount();
            long inserted = insertIntoMe(level, player, toolStack, refundStack, count, Actionable.MODULATE);

            if (inserted > 0) {
                insertedIntoMe = true;
            }

            int remaining = count - (int) Math.min(count, inserted);

            while (remaining > 0) {
                ItemStack part = refundStack.copy();
                int partCount = Math.min(remaining, part.getMaxStackSize());

                part.setCount(partCount);

                boolean added = player.getInventory().add(part);

                if (!added || !part.isEmpty()) {
                    player.getInventory().setChanged();
                    return ClonerRefundResult.failure(insertedIntoMe);
                }

                remaining -= partCount;
            }
        }

        player.getInventory().setChanged();
        return ClonerRefundResult.success(insertedIntoMe);
    }

    private boolean canFitInInventory(Player player, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            return true;
        }

        List<ItemStack> simulated = new ArrayList<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            simulated.add(player.getInventory().getItem(i).copy());
        }

        for (ItemStack wanted : stacks) {
            if (wanted.isEmpty()) {
                continue;
            }

            int remaining = wanted.getCount();

            for (ItemStack slot : simulated) {
                if (remaining <= 0) {
                    break;
                }

                if (slot.isEmpty()) {
                    continue;
                }

                if (!ItemStack.isSameItemSameTags(slot, wanted)) {
                    continue;
                }

                int max = Math.min(slot.getMaxStackSize(), wanted.getMaxStackSize());
                int space = max - slot.getCount();

                if (space <= 0) {
                    continue;
                }

                int inserted = Math.min(space, remaining);
                slot.grow(inserted);
                remaining -= inserted;
            }

            for (int i = 0; i < simulated.size(); i++) {
                if (remaining <= 0) {
                    break;
                }

                ItemStack slot = simulated.get(i);

                if (!slot.isEmpty()) {
                    continue;
                }

                int inserted = Math.min(wanted.getMaxStackSize(), remaining);
                ItemStack copy = wanted.copy();

                copy.setCount(inserted);
                simulated.set(i, copy);
                remaining -= inserted;
            }

            if (remaining > 0) {
                return false;
            }
        }

        return true;
    }

    private long simulateInsertIntoMe(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            ItemStack wanted,
            long amount
    ) {
        return insertIntoMe(level, player, toolStack, wanted, amount, Actionable.SIMULATE);
    }

    private long insertIntoMe(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            ItemStack wanted,
            long amount,
            Actionable mode
    ) {
        if (wanted.isEmpty() || amount <= 0) {
            return 0;
        }

        MEStorage storage = getConnectedMeStorage(level, toolStack, player);

        if (storage == null) {
            return 0;
        }

        IEnergyService energy = getConnectedMeEnergy(level, toolStack, player);

        if (energy == null) {
            return 0;
        }

        AEItemKey key = AEItemKey.of(wanted);

        if (key == null) {
            return 0;
        }

        try {
            return StorageHelper.poweredInsert(
                    energy,
                    storage,
                    key,
                    amount,
                    getPasteActionSource(player),
                    mode
            );
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void clearStoredStructure(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            String id
    ) {
        StructureToolStackState.clearSelectedClonerLibraryEntry(toolStack);
        StructureToolStackState.clearSelection(toolStack);
        StructureToolStackState.resetPreviewSideMap(toolStack);

        TemplateUtil.setTemplateOffset(toolStack.getOrCreateTag(), BlockPos.ZERO);
        TemplateUtil.setEnergyOrigin(toolStack.getOrCreateTag(), BlockPos.ZERO);

        if (player instanceof ServerPlayer serverPlayer) {
            StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
        }
    }

    protected boolean placeRegularBlockBestEffort(
            ServerLevel level,
            BlockPos worldPos,
            BlockState stateToPlace,
            @Nullable CompoundTag rawBeTag,
            @Nullable CompoundTag blockMetadata,
            Player player,
            ItemStack toolStack
    ) {
        BlockState existing = level.getBlockState(worldPos);

        if (hasCollision(existing, stateToPlace)) {
            return false;
        }

        if (!player.isCreative()) {
            ItemStack required = getRequiredBlockItem(stateToPlace);

            if (required.isEmpty()) {
                return false;
            }

            if (countAvailableForPaste(level, player, toolStack, required) < 1) {
                return false;
            }
        }

        if (!placeBlockAndLoadTag(level, worldPos, stateToPlace, null)) {
            return false;
        }

        if (!player.isCreative()) {
            ItemStack required = getRequiredBlockItem(stateToPlace);

            return consumeForPaste(level, player, toolStack, required, 1);
        }

        return true;
    }

    protected boolean placePlannedBlockBestEffort(
            ServerLevel level,
            BlockPos worldPos,
            PlacementPlan plan,
            Player player,
            ItemStack toolStack
    ) {
        if (!plan.shouldPlace()) {
            return false;
        }

        BlockState stateToPlace = plan.stateToPlace();

        if (stateToPlace == null) {
            return false;
        }

        BlockState existing = level.getBlockState(worldPos);

        if (hasCollision(existing, stateToPlace)) {
            return false;
        }

        if (!placeBlockAndLoadTag(level, worldPos, stateToPlace, plan.blockEntityTag())) {
            return false;
        }

        if (!player.isCreative()) {
            for (ItemStack cost : plan.consumedStacks()) {
                if (!consumeForPaste(level, player, toolStack, cost, cost.getCount())) {
                    return false;
                }
            }
        }

        return true;
    }

    protected boolean placeBlockAndLoadTag(
            ServerLevel level,
            BlockPos worldPos,
            BlockState stateToPlace,
            @Nullable CompoundTag rawBeTag
    ) {
        BlockState oldState = level.getBlockState(worldPos);

        if (level.getBlockEntity(worldPos) != null) {
            level.removeBlockEntity(worldPos);
        }

        if (!oldState.isAir()) {
            level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
        }

        if (!level.setBlock(worldPos, stateToPlace, 3)) {
            return false;
        }

        if (rawBeTag != null) {
            BlockEntity blockEntity = level.getBlockEntity(worldPos);

            if (blockEntity != null) {
                CompoundTag beTag = rawBeTag.copy();

                beTag.putInt("x", worldPos.getX());
                beTag.putInt("y", worldPos.getY());
                beTag.putInt("z", worldPos.getZ());

                try {
                    blockEntity.load(beTag);
                    blockEntity.setChanged();
                } catch (Throwable ignored) {
                }
            }
        }

        level.sendBlockUpdated(worldPos, oldState, stateToPlace, 3);
        return true;
    }

    protected boolean hasCollision(BlockState existing, BlockState target) {
        return !(existing.isAir() || existing.canBeReplaced() || existing.equals(target));
    }

    protected ItemStack getRequiredBlockItem(BlockState state) {
        Item item = state.getBlock().asItem();

        if (item == ItemStack.EMPTY.getItem() || item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);

        stack.setCount(1);
        stack.setTag(null);

        return stack;
    }

    protected long countAvailableForPaste(ServerLevel level, Player player, ItemStack toolStack, ItemStack wanted) {
        if (wanted.isEmpty()) {
            return 0;
        }

        long total = countInPlayerInventory(player, wanted);

        total += simulateExtractFromMe(level, player, toolStack, wanted, Integer.MAX_VALUE);

        return total;
    }

    protected boolean canReserveForPaste(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            Map<Item, Integer> reserved,
            ItemStack wanted,
            int amount
    ) {
        if (wanted.isEmpty() || amount <= 0) {
            return false;
        }

        long available = countAvailableForPaste(level, player, toolStack, wanted);
        int alreadyReserved = reserved.getOrDefault(wanted.getItem(), 0);

        if (available < alreadyReserved + amount) {
            return false;
        }

        reserved.put(wanted.getItem(), alreadyReserved + amount);

        return true;
    }

    protected boolean consumeForPaste(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            ItemStack wanted,
            int amount
    ) {
        if (wanted.isEmpty() || amount <= 0) {
            return true;
        }

        int inInv = countInPlayerInventory(player, wanted);
        int fromInv = Math.min(inInv, amount);
        int left = amount - fromInv;

        if (left > 0) {
            long simulated = simulateExtractFromMe(level, player, toolStack, wanted, left);

            if (simulated < left) {
                return false;
            }
        }

        if (fromInv > 0) {
            int removed = consumeFromPlayerInventoryPartial(player, wanted, fromInv);

            if (removed < fromInv) {
                return false;
            }
        }

        if (left > 0) {
            long extracted = extractFromMe(level, player, toolStack, wanted, left, Actionable.MODULATE);

            return extracted >= left;
        }

        return true;
    }

    protected int countInPlayerInventory(Player player, ItemStack wanted) {
        if (wanted.isEmpty()) {
            return 0;
        }

        int found = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() == wanted.getItem()) {
                found += stack.getCount();
            }
        }

        return found;
    }

    protected int consumeFromPlayerInventoryPartial(Player player, ItemStack wanted, int amount) {
        if (wanted.isEmpty() || amount <= 0) {
            return 0;
        }

        int remaining = amount;
        int removed = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() != wanted.getItem()) {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());

            stack.shrink(taken);

            removed += taken;
            remaining -= taken;

            if (remaining <= 0) {
                player.getInventory().setChanged();
                return removed;
            }
        }

        player.getInventory().setChanged();
        return removed;
    }

    protected @Nullable IGrid getConnectedGridForPaste(ServerLevel level, ItemStack toolStack, @Nullable Player player) {
        return getLinkedGrid(toolStack, level, player);
    }

    protected @Nullable MEStorage getConnectedMeStorage(ServerLevel level, ItemStack toolStack, @Nullable Player player) {
        IGrid grid = getConnectedGridForPaste(level, toolStack, player);

        if (grid == null) {
            return null;
        }

        var storageService = grid.getStorageService();

        if (storageService == null) {
            return null;
        }

        return storageService.getInventory();
    }

    protected @Nullable IEnergyService getConnectedMeEnergy(ServerLevel level, ItemStack toolStack, @Nullable Player player) {
        IGrid grid = getConnectedGridForPaste(level, toolStack, player);

        if (grid == null) {
            return null;
        }

        return grid.getEnergyService();
    }

    protected IActionSource getPasteActionSource(Player player) {
        return IActionSource.empty();
    }

    protected long simulateExtractFromMe(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            ItemStack wanted,
            long amount
    ) {
        return extractFromMe(level, player, toolStack, wanted, amount, Actionable.SIMULATE);
    }

    protected long extractFromMe(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            ItemStack wanted,
            long amount,
            Actionable mode
    ) {
        if (wanted.isEmpty() || amount <= 0) {
            return 0;
        }

        MEStorage storage = getConnectedMeStorage(level, toolStack, player);

        if (storage == null) {
            return 0;
        }

        IEnergyService energy = getConnectedMeEnergy(level, toolStack, player);

        if (energy == null) {
            return 0;
        }

        AEItemKey key = AEItemKey.of(wanted);

        if (key == null) {
            return 0;
        }

        try {
            return StorageHelper.poweredExtraction(
                    energy,
                    storage,
                    key,
                    amount,
                    getPasteActionSource(player),
                    mode
            );
        } catch (Throwable ignored) {
            return 0;
        }
    }

    protected Map<BlockPos, CompoundTag> parseMetadataByPos(CompoundTag savedTag) {
        Map<BlockPos, CompoundTag> out = new HashMap<>();

        if (!savedTag.contains(StructureToolKeys.CLONE_METADATA_KEY, Tag.TAG_COMPOUND)) {
            return out;
        }

        CompoundTag metadata = savedTag.getCompound(StructureToolKeys.CLONE_METADATA_KEY);

        if (!metadata.contains(StructureToolKeys.CLONE_METADATA_BLOCKS_KEY, Tag.TAG_LIST)) {
            return out;
        }

        ListTag blocks = metadata.getList(StructureToolKeys.CLONE_METADATA_BLOCKS_KEY, Tag.TAG_COMPOUND);

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockEntry = blocks.getCompound(i);

            if (!blockEntry.contains(StructureToolKeys.CLONE_KEY_POS, Tag.TAG_COMPOUND)) {
                continue;
            }

            CompoundTag posTag = blockEntry.getCompound(StructureToolKeys.CLONE_KEY_POS);
            BlockPos pos = new BlockPos(
                    posTag.getInt("x"),
                    posTag.getInt("y"),
                    posTag.getInt("z")
            );

            out.put(pos, blockEntry);
        }

        return out;
    }

    private final class TrackingPasteContext implements ClonerPasteContext {
        private final ServerLevel level;
        private final Player player;
        private final ItemStack toolStack;
        private final List<ItemStack> consumedStacks;

        private TrackingPasteContext(
                ServerLevel level,
                Player player,
                ItemStack toolStack,
                List<ItemStack> consumedStacks
        ) {
            this.level = level;
            this.player = player;
            this.toolStack = toolStack;
            this.consumedStacks = consumedStacks;
        }

        @Override
        public long countAvailableForPaste(ItemStack wanted) {
            return PortableSpatialCloner.this.countAvailableForPaste(level, player, toolStack, wanted);
        }

        @Override
        public boolean canReserveForPaste(
                Map<Item, Integer> reserved,
                ItemStack wanted,
                int amount
        ) {
            return PortableSpatialCloner.this.canReserveForPaste(
                    level,
                    player,
                    toolStack,
                    reserved,
                    wanted,
                    amount
            );
        }

        @Override
        public boolean consumeForPaste(ItemStack wanted, int amount) {
            boolean success = PortableSpatialCloner.this.consumeForPaste(
                    level,
                    player,
                    toolStack,
                    wanted,
                    amount
            );

            if (success && !wanted.isEmpty() && amount > 0) {
                ItemStack copy = wanted.copy();

                copy.setCount(amount);
                consumedStacks.add(copy);
            }

            return success;
        }

        @Override
        public boolean placeBlockAndLoadTag(
                BlockPos pos,
                BlockState state,
                @Nullable CompoundTag rawBeTag
        ) {
            return PortableSpatialCloner.this.placeBlockAndLoadTag(
                    level,
                    pos,
                    state,
                    rawBeTag
            );
        }

        @Override
        public boolean hasCollision(BlockState existing, BlockState target) {
            return PortableSpatialCloner.this.hasCollision(existing, target);
        }

        @Override
        public ItemStack getRequiredBlockItem(BlockState state) {
            return PortableSpatialCloner.this.getRequiredBlockItem(state);
        }
    }

    private final class PasteContext implements ClonerPasteContext {
        private final ServerLevel level;
        private final Player player;
        private final ItemStack toolStack;

        private PasteContext(ServerLevel level, Player player, ItemStack toolStack) {
            this.level = level;
            this.player = player;
            this.toolStack = toolStack;
        }

        @Override
        public long countAvailableForPaste(ItemStack wanted) {
            return PortableSpatialCloner.this.countAvailableForPaste(level, player, toolStack, wanted);
        }

        @Override
        public boolean canReserveForPaste(
                Map<Item, Integer> reserved,
                ItemStack wanted,
                int amount
        ) {
            return PortableSpatialCloner.this.canReserveForPaste(
                    level,
                    player,
                    toolStack,
                    reserved,
                    wanted,
                    amount
            );
        }

        @Override
        public boolean consumeForPaste(ItemStack wanted, int amount) {
            return PortableSpatialCloner.this.consumeForPaste(
                    level,
                    player,
                    toolStack,
                    wanted,
                    amount
            );
        }

        @Override
        public boolean placeBlockAndLoadTag(
                BlockPos pos,
                BlockState state,
                @Nullable CompoundTag rawBeTag
        ) {
            return PortableSpatialCloner.this.placeBlockAndLoadTag(
                    level,
                    pos,
                    state,
                    rawBeTag
            );
        }

        @Override
        public boolean hasCollision(BlockState existing, BlockState target) {
            return PortableSpatialCloner.this.hasCollision(existing, target);
        }

        @Override
        public ItemStack getRequiredBlockItem(BlockState state) {
            return PortableSpatialCloner.this.getRequiredBlockItem(state);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag advancedTooltips) {
        super.appendHoverText(stack, level, tooltip, advancedTooltips);

        if (!CrazyConfig.COMMON.PORTABLE_SPATIAL_CLONER_ENABLED.get()) {
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED.getTranslationKey())
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED_CONFIG.getTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    protected String saveCapturedStructure(
            ServerLevel level,
            Player player,
            ItemStack stack,
            CompoundTag savedTag
    ) throws IOException {
        ClonerStructureLibraryStore.Entry entry = ClonerStructureLibraryStore.saveForCurrentSelection(
                level.getServer(),
                player.getUUID(),
                stack,
                savedTag
        );

        StructureToolStackState.setSelectedClonerLibraryEntry(
                stack,
                player.getUUID(),
                entry.id()
        );

        return entry.id();
    }

    private record ClonerUndoPlacedBlock(
            BlockPos pos,
            String stateSignature,
            List<ItemStack> refundStacks
    ) {
    }

    private record ClonerRefundResult(
            boolean success,
            boolean insertedIntoMe
    ) {
        private static ClonerRefundResult success(boolean insertedIntoMe) {
            return new ClonerRefundResult(true, insertedIntoMe);
        }

        private static ClonerRefundResult failure(boolean insertedIntoMe) {
            return new ClonerRefundResult(false, insertedIntoMe);
        }
    }
}