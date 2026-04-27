package net.oktawia.crazyae2addons.logic.structuretool;

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;
import appeng.core.localization.Tooltips;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.ShowHudMessagePacket;
import net.oktawia.crazyae2addons.util.StructureToolKeys;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;

public abstract class AbstractStructureCaptureToolItem extends WirelessTerminalItem implements IMenuItem, IUpgradeableObject {

    protected static final int DEFAULT_UPGRADE_SLOTS = 4;
    protected static final double POWER_PER_BLOCK_PASTE = 1.0D;
    protected static final String CURRENT_POWER_NBT_KEY = "internalCurrentPower";

    private static final String WAS_HELD_IN_HAND_NBT_KEY = "wasHeldInHand";
    private static final String SELECTION_DIMENSION_NBT_KEY = "selectionDimension";

    private static final int HUD_COLOR_CYAN = 0x55FFFF;
    private static final int HUD_COLOR_RED = 0xFF4040;
    private static final int HUD_TIME_SHORT = 60;
    protected static final int HUD_TIME_MEDIUM = 100;

    private static final int CUT_CLEAR_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final int upgradeSlots;

    protected AbstractStructureCaptureToolItem(
            DoubleSupplier basePowerSupplier,
            int upgradeSlots,
            Item.Properties properties
    ) {
        super(basePowerSupplier, properties.stacksTo(1));
        this.upgradeSlots = upgradeSlots;
    }

    @FunctionalInterface
    public interface RequirementSink {
        void add(ItemStack stack);
    }

    protected record CapturedStructureResult(
            String structureId,
            CompoundTag savedTag,
            BlockPos min,
            BlockPos max,
            BlockPos origin,
            double usedPower
    ) {
    }

    protected abstract MenuType<?> getToolMenuType();

    protected abstract boolean removeCapturedBlocks();

    protected abstract Component getCaptureSuccessMessage();

    protected abstract Component getStoredStructureActionNotImplementedMessage();

    protected boolean isToolEnabled() {
        return true;
    }

    protected int getMaxStructureSize() {
        return -1;
    }

    protected double getPowerPerBlockCapture() {
        return 1.0D;
    }

    protected double getPowerPerBlockPaste() {
        return POWER_PER_BLOCK_PASTE;
    }

    protected void afterStructureCaptured(
            ServerLevel level,
            Player player,
            ItemStack stack,
            CapturedStructureResult result
    ) {
    }

    @Override
    public double getChargeRate(ItemStack stack) {
        return 800.0D + 800.0D * Upgrades.getEnergyCardMultiplier(getUpgrades(stack));
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, upgradeSlots, this::onUpgradesChanged);
    }

    private void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades) {
        setAEMaxPowerMultiplier(stack, 1 + Upgrades.getEnergyCardMultiplier(upgrades));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag advancedTooltips) {
        CompoundTag tag = stack.getTag();
        double internalCurrentPower = 0;
        double internalMaxPower = this.getAEMaxPower(stack);

        if (tag != null) {
            internalCurrentPower = tag.getDouble(CURRENT_POWER_NBT_KEY);
        }

        lines.add(Tooltips.energyStorageComponent(internalCurrentPower, internalMaxPower));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        boolean isHeldNow = isHeldInHand(player, stack);
        CompoundTag tag = stack.getOrCreateTag();
        boolean wasHeldBefore = tag.getBoolean(WAS_HELD_IN_HAND_NBT_KEY);

        if (isHeldNow && !wasHeldBefore) {
            showHud(player, Component.translatable(LangDefs.CORNER_0_SELECTED.getTranslationKey()));
        }

        tag.putBoolean(WAS_HELD_IN_HAND_NBT_KEY, isHeldNow);

        if (isHeldNow) {
            ensureSelectionDimensionOrClear(level, player, stack, true);
        }
    }

    protected boolean isHeldInHand(Player player, ItemStack stack) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    protected boolean tryUsePower(Player player, ItemStack stack, double amount) {
        if (player.isCreative()) {
            return true;
        }

        if (amount <= 0) {
            return true;
        }

        if (getAECurrentPower(stack) + 0.0001D < amount) {
            return false;
        }

        return extractAEPower(stack, amount, Actionable.MODULATE) >= amount - 0.0001D;
    }

    protected static ShowHudMessagePacket.Line cyan(Component text) {
        return new ShowHudMessagePacket.Line(text, HUD_COLOR_CYAN);
    }

    protected static ShowHudMessagePacket.Line red(Component text) {
        return new ShowHudMessagePacket.Line(text, HUD_COLOR_RED);
    }

    protected void showHud(Player player, int durationTicks, ShowHudMessagePacket.Line... lines) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NetworkHandler.sendToPlayer(serverPlayer, new ShowHudMessagePacket(durationTicks, List.of(lines)));
    }

    protected void showHud(Player player, Component text) {
        showHud(player, HUD_TIME_SHORT, cyan(text));
    }

    protected void showNotEnoughPower(Player player, ItemStack stack, double required) {
        int current = (int) Math.floor(getAECurrentPower(stack));
        int needed = (int) Math.ceil(required);

        showHud(
                player,
                HUD_TIME_MEDIUM,
                red(Component.translatable(LangDefs.NOT_ENOUGH_POWER.getTranslationKey())),
                cyan(Component.translatable(LangDefs.NEED_AE.getTranslationKey(), needed)),
                cyan(Component.translatable(LangDefs.HAVE_AE.getTranslationKey(), current))
        );
    }

    protected boolean checkStructureSizeLimit(Player player, CompoundTag templateTag) {
        int maxSize = getMaxStructureSize();

        if (maxSize < 0) {
            return true;
        }

        int blockCount = TemplateUtil.parseRawBlocksFromTag(templateTag).size();

        if (blockCount <= maxSize) {
            return true;
        }

        showHud(
                player,
                HUD_TIME_MEDIUM,
                red(Component.translatable(LangDefs.STRUCTURE_TOO_LARGE.getTranslationKey())),
                cyan(Component.translatable(LangDefs.STRUCTURE_SIZE.getTranslationKey(), blockCount)),
                cyan(Component.translatable(LangDefs.STRUCTURE_SIZE_LIMIT.getTranslationKey(), maxSize))
        );

        return false;
    }

    protected static boolean isTemplateEmpty(CompoundTag templateTag) {
        return TemplateUtil.parseRawBlocksFromTag(templateTag).isEmpty();
    }

    private boolean ensureSelectionDimensionOrClear(
            Level level,
            Player player,
            ItemStack stack,
            boolean notify
    ) {
        if (StructureToolStackState.hasStructure(stack)) {
            clearSelectionDimension(stack);
            return true;
        }

        BlockPos selectionA = StructureToolStackState.getSelectionA(stack);

        if (selectionA == null) {
            clearSelectionDimension(stack);
            return true;
        }

        String currentDimension = level.dimension().location().toString();
        CompoundTag tag = stack.getOrCreateTag();

        if (!tag.contains(SELECTION_DIMENSION_NBT_KEY, Tag.TAG_STRING)) {
            tag.putString(SELECTION_DIMENSION_NBT_KEY, currentDimension);
            return true;
        }

        String selectionDimension = tag.getString(SELECTION_DIMENSION_NBT_KEY);

        if (currentDimension.equals(selectionDimension)) {
            return true;
        }

        clearSelectionState(stack);

        if (player instanceof ServerPlayer serverPlayer) {
            StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
        }

        if (notify) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_SELECTION_CLEARED.getTranslationKey())),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_DIMENSION_CHANGED.getTranslationKey()))
            );
        }

        return false;
    }

    private static void rememberSelectionDimension(ItemStack stack, Level level) {
        stack.getOrCreateTag().putString(
                SELECTION_DIMENSION_NBT_KEY,
                level.dimension().location().toString()
        );
    }

    private static void clearSelectionDimension(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag != null) {
            tag.remove(SELECTION_DIMENSION_NBT_KEY);
        }
    }

    protected static void clearSelectionState(ItemStack stack) {
        StructureToolStackState.clearSelection(stack);
        StructureToolStackState.resetPreviewSideMap(stack);
        clearSelectionDimension(stack);
    }

    protected static boolean shouldSkipStructureToolBlock(
            Level level,
            BlockPos pos,
            BlockState state
    ) {
        if (state.isAir()) {
            return false;
        }

        if (state.is(Blocks.BEDROCK)
                || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_GATEWAY)
                || state.is(Blocks.BARRIER)
                || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                || state.is(Blocks.REPEATING_COMMAND_BLOCK)
                || state.is(Blocks.STRUCTURE_BLOCK)
                || state.is(Blocks.STRUCTURE_VOID)
                || state.is(Blocks.JIGSAW)) {
            return true;
        }

        try {
            return state.getDestroySpeed(level, pos) < 0.0F;
        } catch (Throwable ignored) {
            return false;
        }
    }

    protected static CompoundTag filterUncapturableBlocksFromTemplate(
            Level level,
            BlockPos worldOrigin,
            CompoundTag templateTag
    ) {
        CompoundTag filtered = templateTag.copy();
        List<TemplateUtil.BlockInfo> parsedBlocks = TemplateUtil.parseRawBlocksFromTag(filtered);

        if (parsedBlocks.isEmpty()) {
            return filtered;
        }

        Set<BlockPos> skippedLocalPositions = new HashSet<>();

        for (TemplateUtil.BlockInfo info : parsedBlocks) {
            BlockPos localPos = info.pos();
            BlockPos worldPos = worldOrigin.offset(localPos);

            if (shouldSkipStructureToolBlock(level, worldPos, info.state())) {
                skippedLocalPositions.add(localPos);
            }
        }

        if (skippedLocalPositions.isEmpty()) {
            return filtered;
        }

        removeTemplateBlockEntriesAt(filtered, skippedLocalPositions);
        removeCloneMetadataEntriesAt(filtered, skippedLocalPositions);

        return filtered;
    }

    private static void removeTemplateBlockEntriesAt(
            CompoundTag templateTag,
            Set<BlockPos> skippedLocalPositions
    ) {
        if (!templateTag.contains("blocks", Tag.TAG_LIST)) {
            return;
        }

        ListTag oldBlocks = templateTag.getList("blocks", Tag.TAG_COMPOUND);
        ListTag newBlocks = new ListTag();

        for (int i = 0; i < oldBlocks.size(); i++) {
            CompoundTag blockEntry = oldBlocks.getCompound(i);
            BlockPos localPos = readTemplateBlockPos(blockEntry);

            if (localPos == null || !skippedLocalPositions.contains(localPos)) {
                newBlocks.add(blockEntry.copy());
            }
        }

        templateTag.put("blocks", newBlocks);
    }

    private static void removeCloneMetadataEntriesAt(
            CompoundTag templateTag,
            Set<BlockPos> skippedLocalPositions
    ) {
        if (!templateTag.contains(StructureToolKeys.CLONE_METADATA_KEY, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag cloneMetadata = templateTag.getCompound(StructureToolKeys.CLONE_METADATA_KEY).copy();

        if (!cloneMetadata.contains(StructureToolKeys.CLONE_METADATA_BLOCKS_KEY, Tag.TAG_LIST)) {
            templateTag.put(StructureToolKeys.CLONE_METADATA_KEY, cloneMetadata);
            return;
        }

        ListTag oldBlocks = cloneMetadata.getList(StructureToolKeys.CLONE_METADATA_BLOCKS_KEY, Tag.TAG_COMPOUND);
        ListTag newBlocks = new ListTag();

        for (int i = 0; i < oldBlocks.size(); i++) {
            CompoundTag blockEntry = oldBlocks.getCompound(i);
            BlockPos localPos = readCloneMetadataBlockPos(blockEntry);

            if (localPos == null || !skippedLocalPositions.contains(localPos)) {
                newBlocks.add(blockEntry.copy());
            }
        }

        cloneMetadata.put(StructureToolKeys.CLONE_METADATA_BLOCKS_KEY, newBlocks);
        templateTag.put(StructureToolKeys.CLONE_METADATA_KEY, cloneMetadata);
    }

    private static @Nullable BlockPos readTemplateBlockPos(CompoundTag blockEntry) {
        if (!blockEntry.contains("pos", Tag.TAG_LIST)) {
            return null;
        }

        ListTag posTag = blockEntry.getList("pos", Tag.TAG_INT);

        if (posTag.size() < 3) {
            return null;
        }

        return new BlockPos(
                posTag.getInt(0),
                posTag.getInt(1),
                posTag.getInt(2)
        );
    }

    private static @Nullable BlockPos readCloneMetadataBlockPos(CompoundTag blockEntry) {
        if (!blockEntry.contains(StructureToolKeys.CLONE_KEY_POS, Tag.TAG_COMPOUND)) {
            return null;
        }

        CompoundTag posTag = blockEntry.getCompound(StructureToolKeys.CLONE_KEY_POS);

        return new BlockPos(
                posTag.getInt("x"),
                posTag.getInt("y"),
                posTag.getInt("z")
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!isToolEnabled()) {
            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide()) {
            if (!ensureSelectionDimensionOrClear(level, player, stack, true)) {
                return InteractionResultHolder.success(stack);
            }

            boolean hasStructure = StructureToolStackState.hasStructure(stack);

            if (player.isShiftKeyDown()) {
                openMenu(player, hand);
                return InteractionResultHolder.success(stack);
            }

            if (hasStructure) {
                onUseWithStoredStructure((ServerLevel) level, player, stack);
                return InteractionResultHolder.success(stack);
            }

            if (isWaitingForSecondCorner(stack)) {
                selectSecondCorner((ServerLevel) level, player, stack);
                return InteractionResultHolder.success(stack);
            }

            captureStructure((ServerLevel) level, player, stack);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!isToolEnabled()) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        boolean hasStructure = StructureToolStackState.hasStructure(stack);

        if (!level.isClientSide() && !ensureSelectionDimensionOrClear(level, player, stack, true)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (player.isShiftKeyDown()) {
            if (hasStructure) {
                if (!level.isClientSide()) {
                    openMenu(player, context.getHand());
                }

                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            BlockPos selectionA = StructureToolStackState.getSelectionA(stack);
            BlockPos selectionB = StructureToolStackState.getSelectionB(stack);

            if (selectionA == null) {
                StructureToolStackState.setSelectionA(stack, clickedPos.immutable());

                if (!level.isClientSide()) {
                    rememberSelectionDimension(stack, level);
                    showHud(player, Component.translatable(LangDefs.CORNER_A_SELECTED.getTranslationKey()));
                }
            } else if (selectionB == null) {
                StructureToolStackState.setSelectionB(stack, clickedPos.immutable());
                StructureToolStackState.setSourceFacing(stack, player.getDirection());

                if (!level.isClientSide()) {
                    showHud(player, Component.translatable(LangDefs.CORNER_B_SELECTED.getTranslationKey()));
                }
            } else {
                clearSelectionState(stack);

                if (!level.isClientSide()) {
                    showHud(player, Component.translatable(LangDefs.SELECTION_RESTARTED.getTranslationKey()));
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (hasStructure) {
            if (!level.isClientSide()) {
                onUseOnWithStoredStructure(
                        (ServerLevel) level,
                        player,
                        stack,
                        clickedPos.relative(context.getClickedFace())
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide() && isWaitingForSecondCorner(stack)) {
            selectSecondCorner((ServerLevel) level, player, stack);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    protected void onUseWithStoredStructure(ServerLevel level, Player player, ItemStack stack) {
        showHud(player, getStoredStructureActionNotImplementedMessage());
    }

    protected void onUseOnWithStoredStructure(ServerLevel level, Player player, ItemStack stack, BlockPos clickedFacePos) {
        showHud(player, getStoredStructureActionNotImplementedMessage());
    }

    protected static boolean isWaitingForSecondCorner(ItemStack stack) {
        return !StructureToolStackState.hasStructure(stack)
                && StructureToolStackState.getSelectionA(stack) != null
                && StructureToolStackState.getSelectionB(stack) == null;
    }

    protected void openMenu(Player player, InteractionHand hand) {
        MenuOpener.open(getToolMenuType(), player, MenuLocators.forHand(player, hand));
    }

    protected void selectSecondCorner(ServerLevel level, Player player, ItemStack stack) {
        if (!ensureSelectionDimensionOrClear(level, player, stack, true)) {
            return;
        }

        BlockHitResult hit = StructureToolUtil.rayTrace(level, player, 50.0D);

        if (hit.getType() != HitResult.Type.BLOCK) {
            showHud(player, Component.translatable(LangDefs.NO_BLOCK_IN_RANGE.getTranslationKey()));
            return;
        }

        BlockPos pos = hit.getBlockPos().immutable();
        StructureToolStackState.setSelectionB(stack, pos);
        StructureToolStackState.setSourceFacing(stack, player.getDirection());

        showHud(player, Component.translatable(LangDefs.CORNER_B_SELECTED.getTranslationKey()));
    }

    protected void captureStructure(ServerLevel level, Player player, ItemStack stack) {
        BlockPos a = StructureToolStackState.getSelectionA(stack);
        BlockPos b = StructureToolStackState.getSelectionB(stack);

        if (a == null || b == null) {
            return;
        }

        if (!ensureSelectionDimensionOrClear(level, player, stack, true)) {
            return;
        }

        BlockPos min = new BlockPos(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ())
        );

        BlockPos max = new BlockPos(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())
        );

        captureStructureFromBounds(
                level,
                player,
                stack,
                min,
                max,
                b,
                true,
                removeCapturedBlocks(),
                !player.isCreative(),
                true
        );
    }

    protected @Nullable CapturedStructureResult captureStructureFromBounds(
            ServerLevel level,
            Player player,
            ItemStack stack,
            BlockPos min,
            BlockPos max,
            BlockPos origin,
            boolean consumePower,
            boolean removeBlocks,
            boolean filterUncapturable,
            boolean showSuccess
    ) {
        BlockPos size = max.subtract(min).offset(1, 1, 1);

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, false, Blocks.STRUCTURE_VOID);

        CompoundTag savedTag = template.save(new CompoundTag());

        if (filterUncapturable) {
            savedTag = filterUncapturableBlocksFromTemplate(level, min, savedTag);
        }

        savedTag = TemplateUtil.stripAirFromTag(savedTag);
        TemplateUtil.setTemplateOffset(savedTag, BlockPos.ZERO);

        if (isTemplateEmpty(savedTag)) {
            clearSelectionState(stack);

            TemplateUtil.setTemplateOffset(stack.getOrCreateTag(), BlockPos.ZERO);
            TemplateUtil.setEnergyOrigin(stack.getOrCreateTag(), BlockPos.ZERO);

            if (player instanceof ServerPlayer serverPlayer) {
                StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
            }

            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_SELECTION_EMPTY_OR_SKIPPED.getTranslationKey())),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_NOTHING_CAPTURED.getTranslationKey()))
            );

            return null;
        }

        if (!checkStructureSizeLimit(player, savedTag)) {
            return null;
        }

        CompoundTag metadata = getMetadata(level, player, min, max, savedTag);

        if (!metadata.isEmpty()) {
            savedTag.put(StructureToolKeys.CLONE_METADATA_KEY, metadata);
        }

        BlockPos localOrigin = origin.subtract(min);
        TemplateUtil.setEnergyOrigin(savedTag, localOrigin);
        TemplateUtil.copyPreviewTransformState(savedTag, stack.getOrCreateTag());

        double requiredPower = StructureToolUtil.calculatePreviewStructurePower(
                savedTag,
                localOrigin,
                getPowerPerBlockCapture(),
                getEnergyCostMultiplier()
        );

        double usedPower = 0.0D;

        if (consumePower && !player.isCreative()) {
            if (!tryUsePower(player, stack, requiredPower)) {
                showNotEnoughPower(player, stack, requiredPower);
                return null;
            }

            usedPower = requiredPower;
        }

        String id;

        try {
            id = saveCapturedStructure(level, player, stack, savedTag);
            StructureToolStackState.setStructureId(stack, id);
            clearSelectionState(stack);
        } catch (IOException exception) {
            showHud(player, Component.translatable(LangDefs.FAILED_TO_SAVE_STRUCTURE.getTranslationKey()));
            return null;
        }

        CapturedStructureResult result = new CapturedStructureResult(
                id,
                savedTag,
                min,
                max,
                origin,
                usedPower
        );

        afterStructureCaptured(level, player, stack, result);

        if (removeBlocks) {
            removeCapturedBlocksWithoutDrops(
                    level,
                    min,
                    savedTag,
                    filterUncapturable
            );
        }

        if (player instanceof ServerPlayer serverPlayer) {
            StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, savedTag);
        }

        if (showSuccess) {
            if (removeCapturedBlocks()) {
                showHud(
                        player,
                        HUD_TIME_MEDIUM,
                        cyan(getCaptureSuccessMessage()),
                        cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_HINT.getTranslationKey()))
                );
            } else {
                showHud(player, getCaptureSuccessMessage());
            }
        }

        return result;
    }

    protected double getEnergyCostMultiplier() {
        return 1.0D;
    }

    private static void removeCapturedBlocksWithoutDrops(
            ServerLevel level,
            BlockPos min,
            CompoundTag savedTag,
            boolean skipUncapturable
    ) {
        BlockState air = Blocks.AIR.defaultBlockState();
        List<TemplateUtil.BlockInfo> blocksToRemove = TemplateUtil.parseRawBlocksFromTag(savedTag);

        for (TemplateUtil.BlockInfo info : blocksToRemove) {
            BlockPos worldPos = min.offset(info.pos());

            if (skipUncapturable && shouldSkipStructureToolBlock(level, worldPos, level.getBlockState(worldPos))) {
                continue;
            }

            level.removeBlockEntity(worldPos);
        }

        for (TemplateUtil.BlockInfo info : blocksToRemove) {
            BlockPos worldPos = min.offset(info.pos());
            BlockState currentState = level.getBlockState(worldPos);

            if (currentState.isAir()) {
                continue;
            }

            if (skipUncapturable && shouldSkipStructureToolBlock(level, worldPos, currentState)) {
                continue;
            }

            level.setBlock(
                    worldPos,
                    air,
                    CUT_CLEAR_FLAGS,
                    0
            );
        }
    }

    private CompoundTag getMetadata(ServerLevel level, Player player, BlockPos min, BlockPos max, CompoundTag savedTag) {
        CompoundTag data = new CompoundTag();
        ListTag blocks = new ListTag();
        RequirementAccumulator requirements = new RequirementAccumulator();

        Map<BlockPos, CompoundTag> rawBeTags = new LinkedHashMap<>();

        for (TemplateUtil.BlockInfo info : TemplateUtil.parseRawBlocksFromTag(savedTag)) {
            if (info.blockEntityTag() != null) {
                rawBeTags.put(info.pos(), info.blockEntityTag());
            }
        }

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockPos localPos = worldPos.subtract(min);

                    BlockState state = level.getBlockState(worldPos);

                    if (!player.isCreative() && shouldSkipStructureToolBlock(level, worldPos, state)) {
                        continue;
                    }

                    BlockEntity be = level.getBlockEntity(worldPos);
                    CompoundTag rawBeTag = rawBeTags.get(localPos);

                    boolean requirementsHandled = false;

                    for (StructureCloneExtension extension : StructureToolExtensions.clonerExtensions()) {
                        if (extension.handlesRequirements(state, rawBeTag)) {
                            requirementsHandled = true;
                            break;
                        }
                    }

                    if (!requirementsHandled) {
                        addBaseBlockRequirement(level, worldPos, requirements);
                    }

                    if (be == null) {
                        continue;
                    }

                    CompoundTag blockEntry = new CompoundTag();
                    blockEntry.put(StructureToolKeys.CLONE_KEY_POS, writeBlockPos(localPos));

                    boolean hasAnyData = false;

                    for (StructureCloneExtension extension : StructureToolExtensions.clonerExtensions()) {
                        if (extension.collectMetadata(
                                level,
                                worldPos,
                                be,
                                rawBeTag,
                                requirements::add,
                                blockEntry
                        )) {
                            hasAnyData = true;
                        }
                    }

                    if (hasAnyData) {
                        blocks.add(blockEntry);
                    }
                }
            }
        }

        if (!blocks.isEmpty()) {
            data.put(StructureToolKeys.CLONE_METADATA_BLOCKS_KEY, blocks);
        }

        ListTag requirementList = requirements.toListTag();

        if (!requirementList.isEmpty()) {
            data.put(StructureToolKeys.CLONE_REQUIREMENTS_KEY, requirementList);
        }

        return data;
    }

    private static void addBaseBlockRequirement(ServerLevel level, BlockPos pos, RequirementAccumulator requirements) {
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos,
                false
        );

        ItemStack picked = level.getBlockState(pos).getCloneItemStack(hit, level, pos, null);

        if (!picked.isEmpty()) {
            requirements.addDefault(picked);
            return;
        }

        Item item = level.getBlockState(pos).getBlock().asItem();

        if (item != Items.AIR) {
            requirements.addDefault(new ItemStack(item));
        }
    }

    private static CompoundTag writeBlockPos(BlockPos pos) {
        CompoundTag tag = new CompoundTag();

        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());

        return tag;
    }

    private enum RequirementKind {
        DEFAULT
    }

    private static final class RequirementAccumulator {
        private final Map<Item, RequirementEntry> entries = new LinkedHashMap<>();

        private void add(ItemStack stack, RequirementKind kind) {
            ItemStack normalized = normalize(stack);

            if (normalized.isEmpty()) {
                return;
            }

            int amount = Math.max(1, stack.getCount());
            RequirementEntry existing = entries.get(normalized.getItem());

            if (existing == null) {
                entries.put(normalized.getItem(), new RequirementEntry(normalized, amount));
            } else {
                existing.count += amount;
            }
        }

        private void add(ItemStack stack) {
            add(stack, RequirementKind.DEFAULT);
        }

        private void addDefault(ItemStack stack) {
            add(stack, RequirementKind.DEFAULT);
        }

        private ListTag toListTag() {
            ListTag list = new ListTag();

            for (RequirementEntry entry : entries.values()) {
                CompoundTag row = new CompoundTag();

                row.put(StructureToolKeys.CLONE_KEY_STACK, entry.stack.save(new CompoundTag()));
                row.putInt(StructureToolKeys.CLONE_KEY_COUNT, entry.count);

                list.add(row);
            }

            return list;
        }

        private static ItemStack normalize(ItemStack stack) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack copy = stack.copy();

            copy.setCount(1);
            copy.setTag(null);

            return copy;
        }
    }

    private static final class RequirementEntry {
        private final ItemStack stack;
        private int count;

        private RequirementEntry(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }

    @Override
    public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new StructureToolHost(player, inventorySlot, stack);
    }

    @Nullable
    @Override
    public IGrid getLinkedGrid(ItemStack item, Level level, @Nullable Player sendMessagesTo) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        var linkedPos = getLinkedPosition(item);

        if (linkedPos == null) {
            return null;
        }

        var linkedLevel = serverLevel.getServer().getLevel(linkedPos.dimension());

        if (linkedLevel == null) {
            return null;
        }

        var be = Platform.getTickingBlockEntity(linkedLevel, linkedPos.pos());

        if (!(be instanceof IWirelessAccessPoint accessPoint)) {
            return null;
        }

        return accessPoint.getGrid();
    }

    protected String saveCapturedStructure(
            ServerLevel level,
            Player player,
            ItemStack stack,
            CompoundTag savedTag
    ) throws IOException {
        String id = UUID.randomUUID().toString();
        StructureToolStructureStore.save(level.getServer(), id, savedTag);
        return id;
    }
}