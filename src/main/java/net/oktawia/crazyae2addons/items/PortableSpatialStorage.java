package net.oktawia.crazyae2addons.items;

import appeng.api.config.Actionable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.structuretool.AbstractStructureCaptureToolItem;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolExtensions;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolPreviewDispatcher;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStructureStore;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class PortableSpatialStorage extends AbstractStructureCaptureToolItem {

    private static final String STORAGE_UNDO_TYPE_KEY = "storageUndoType";
    private static final String STORAGE_UNDO_DIMENSION_KEY = "storageUndoDimension";
    private static final String STORAGE_UNDO_ENERGY_KEY = "storageUndoEnergy";

    private static final String STORAGE_UNDO_TYPE_CUT = "cut";
    private static final String STORAGE_UNDO_TYPE_PASTE = "paste";

    private static final String STORAGE_UNDO_ORIGIN_X_KEY = "storageUndoOriginX";
    private static final String STORAGE_UNDO_ORIGIN_Y_KEY = "storageUndoOriginY";
    private static final String STORAGE_UNDO_ORIGIN_Z_KEY = "storageUndoOriginZ";

    private static final String STORAGE_UNDO_MIN_X_KEY = "storageUndoMinX";
    private static final String STORAGE_UNDO_MIN_Y_KEY = "storageUndoMinY";
    private static final String STORAGE_UNDO_MIN_Z_KEY = "storageUndoMinZ";

    private static final String STORAGE_UNDO_MAX_X_KEY = "storageUndoMaxX";
    private static final String STORAGE_UNDO_MAX_Y_KEY = "storageUndoMaxY";
    private static final String STORAGE_UNDO_MAX_Z_KEY = "storageUndoMaxZ";

    public PortableSpatialStorage(Item.Properties properties) {
        super(
                CrazyConfig.COMMON.PORTABLE_SPATIAL_STORAGE_BASE_INTERNAL_POWER_CAPACITY::get,
                DEFAULT_UPGRADE_SLOTS,
                properties
        );
    }

    public static BlockHitResult rayTrace(net.minecraft.world.level.Level level, Player player, double maxDistance) {
        return StructureToolUtil.rayTrace(level, player, maxDistance);
    }

    @Override
    protected MenuType<?> getToolMenuType() {
        return CrazyMenuRegistrar.PORTABLE_SPATIAL_STORAGE_MENU.get();
    }

    @Override
    protected boolean removeCapturedBlocks() {
        return true;
    }

    @Override
    protected Component getCaptureSuccessMessage() {
        return Component.translatable(LangDefs.STRUCTURE_CUT_AND_SAVED.getTranslationKey());
    }

    @Override
    protected Component getStoredStructureActionNotImplementedMessage() {
        return Component.translatable(LangDefs.STRUCTURE_PASTED.getTranslationKey());
    }

    @Override
    protected boolean isToolEnabled() {
        return CrazyConfig.COMMON.PORTABLE_SPATIAL_STORAGE_ENABLED.get();
    }

    @Override
    protected double getPowerPerBlockCapture() {
        return CrazyConfig.COMMON.PORTABLE_SPATIAL_STORAGE_COST.get();
    }

    @Override
    protected double getPowerPerBlockPaste() {
        return CrazyConfig.COMMON.PORTABLE_SPATIAL_STORAGE_COST.get();
    }

    @Override
    protected int getMaxStructureSize() {
        return CrazyConfig.COMMON.PORTABLE_SPATIAL_STORAGE_MAX_STRUCTURE_SIZE.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND && isToolEnabled()) {
            if (!level.isClientSide()) {
                undoLastStorageAction((ServerLevel) level, player, stack);
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
                undoLastStorageAction((ServerLevel) level, player, context.getItemInHand());
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useOn(context);
    }

    @Override
    protected void afterStructureCaptured(
            ServerLevel level,
            Player player,
            ItemStack stack,
            CapturedStructureResult result
    ) {
        storeUndoCut(
                stack,
                level,
                result.origin(),
                result.min(),
                result.max(),
                result.usedPower()
        );
    }

    @Override
    protected void onUseWithStoredStructure(ServerLevel level, Player player, ItemStack stack) {
        BlockHitResult hit = rayTrace(level, player, 50.0D);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pasteOrigin = hit.getBlockPos().relative(hit.getDirection());
            paste(level, player, stack, pasteOrigin);
        } else {
            showHud(player, Component.translatable(LangDefs.NO_BLOCK_IN_RANGE.getTranslationKey()));
        }
    }

    @Override
    protected void onUseOnWithStoredStructure(ServerLevel level, Player player, ItemStack stack, BlockPos clickedFacePos) {
        paste(level, player, stack, clickedFacePos);
    }

    private double calculateStructurePower(CompoundTag templateTag, BlockPos localOrigin, double baseCostPerBlock) {
        return StructureToolUtil.calculatePreviewStructurePower(templateTag, localOrigin, baseCostPerBlock);
    }

    private void paste(ServerLevel level, Player player, ItemStack stack, BlockPos origin) {
        pasteInternal(
                level,
                player,
                stack,
                origin,
                true,
                true,
                true,
                true
        );
    }

    private boolean pasteInternal(
            ServerLevel level,
            Player player,
            ItemStack stack,
            BlockPos origin,
            boolean consumePower,
            boolean clearAfterPaste,
            boolean recordUndoPaste,
            boolean showSuccess
    ) {
        String id = StructureToolStackState.getStructureId(stack);

        if (id.isBlank()) {
            return false;
        }

        CompoundTag savedTag;

        try {
            savedTag = StructureToolStructureStore.load(level.getServer(), id);
        } catch (IOException exception) {
            showHud(player, Component.translatable(LangDefs.FAILED_TO_LOAD_STRUCTURE.getTranslationKey()));
            return false;
        }

        if (savedTag == null) {
            showHud(player, Component.translatable(LangDefs.STORED_STRUCTURE_NOT_FOUND.getTranslationKey()));
            clearStoredStructure(level, player, stack, id);
            return false;
        }

        if (isTemplateEmpty(savedTag)) {
            clearStoredStructure(level, player, stack, id);

            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_STORED_STRUCTURE_EMPTY.getTranslationKey())),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_INVALID_STRUCTURE_CLEARED.getTranslationKey()))
            );

            return false;
        }

        if (!checkStructureSizeLimit(player, savedTag)) {
            return false;
        }

        if (hasPlacementCollision(level, savedTag, origin)) {
            showHud(player, Component.translatable(LangDefs.PASTE_COLLISION.getTranslationKey()));
            return false;
        }

        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(savedTag);
        double requiredPower = calculateStructurePower(savedTag, energyOrigin, getPowerPerBlockPaste());
        double usedPower = 0.0D;

        if (consumePower && !player.isCreative()) {
            if (!tryUsePower(player, stack, requiredPower)) {
                showNotEnoughPower(player, stack, requiredPower);
                return false;
            }

            usedPower = requiredPower;
        }

        StructureTemplate template = new StructureTemplate();
        template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), savedTag);

        BlockPos templateOffset = TemplateUtil.getTemplateOffset(savedTag);
        BlockPos placementOrigin = origin.subtract(energyOrigin).offset(templateOffset);

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true);

        boolean placed = template.placeInWorld(level, placementOrigin, placementOrigin, settings, level.random, 3);

        if (!placed) {
            showHud(player, Component.translatable(LangDefs.FAILED_TO_PASTE_STRUCTURE.getTranslationKey()));
            return false;
        }

        StructureToolExtensions.notifyTemplatePasted(level, placementOrigin, savedTag);

        if (recordUndoPaste) {
            BlockBounds bounds = computePlacedBounds(savedTag, origin);

            if (bounds != null) {
                storeUndoPaste(
                        stack,
                        level,
                        origin,
                        bounds.min(),
                        bounds.max(),
                        usedPower
                );
            }
        }

        if (clearAfterPaste) {
            clearStoredStructure(level, player, stack, id);
        }

        if (showSuccess) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    cyan(Component.translatable(LangDefs.STRUCTURE_PASTED.getTranslationKey())),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_HINT.getTranslationKey()))
            );
        }

        return true;
    }

    private void undoLastStorageAction(ServerLevel level, Player player, ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains(STORAGE_UNDO_TYPE_KEY)) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_NOTHING_TO_UNDO.getTranslationKey()))
            );
            return;
        }

        String dimension = tag.getString(STORAGE_UNDO_DIMENSION_KEY);
        String currentDimension = level.dimension().location().toString();

        if (!currentDimension.equals(dimension)) {
            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_OTHER_DIMENSION.getTranslationKey()))
            );
            return;
        }

        String type = tag.getString(STORAGE_UNDO_TYPE_KEY);

        if (STORAGE_UNDO_TYPE_CUT.equals(type)) {
            undoCut(level, player, stack);
            return;
        }

        if (STORAGE_UNDO_TYPE_PASTE.equals(type)) {
            undoPaste(level, player, stack);
            return;
        }

        clearStorageUndo(stack);

        showHud(
                player,
                HUD_TIME_MEDIUM,
                red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO_INVALID_CLEARED.getTranslationKey()))
        );
    }

    private void undoCut(ServerLevel level, Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        BlockPos origin = readUndoOrigin(tag);
        double refund = tag.getDouble(STORAGE_UNDO_ENERGY_KEY);

        if (!StructureToolStackState.hasStructure(stack)) {
            clearStorageUndo(stack);

            showHud(
                    player,
                    HUD_TIME_MEDIUM,
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                    red(Component.translatable(LangDefs.STRUCTURE_GADGET_CANNOT_UNDO_CUT.getTranslationKey())),
                    cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_STORED_STRUCTURE_MISSING.getTranslationKey()))
            );

            return;
        }

        boolean success = pasteInternal(
                level,
                player,
                stack,
                origin,
                false,
                true,
                false,
                false
        );

        if (!success) {
            return;
        }

        refundEnergy(stack, refund);
        clearStorageUndo(stack);

        showHud(
                player,
                HUD_TIME_MEDIUM,
                cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_CUT_UNDONE.getTranslationKey())),
                cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_ENERGY_REFUNDED.getTranslationKey()))
        );
    }

    private void undoPaste(ServerLevel level, Player player, ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();

        BlockPos min = readUndoMin(tag);
        BlockPos max = readUndoMax(tag);
        BlockPos origin = readUndoOrigin(tag);
        double refund = tag.getDouble(STORAGE_UNDO_ENERGY_KEY);

        CapturedStructureResult result = captureStructureFromBounds(
                level,
                player,
                stack,
                min,
                max,
                origin,
                false,
                true,
                !player.isCreative(),
                false
        );

        if (result == null) {
            return;
        }

        refundEnergy(stack, refund);

        showHud(
                player,
                HUD_TIME_MEDIUM,
                cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_UNDO.getTranslationKey())),
                cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_PASTE_UNDONE.getTranslationKey())),
                cyan(Component.translatable(LangDefs.STRUCTURE_GADGET_STRUCTURE_CUT_BACK.getTranslationKey()))
        );
    }

    private void refundEnergy(ItemStack stack, double amount) {
        if (amount <= 0.0D) {
            return;
        }

        try {
            injectAEPower(stack, amount, Actionable.MODULATE);
        } catch (Throwable ignored) {
        }
    }

    private void storeUndoCut(
            ItemStack stack,
            ServerLevel level,
            BlockPos origin,
            BlockPos min,
            BlockPos max,
            double usedPower
    ) {
        CompoundTag tag = stack.getOrCreateTag();

        tag.putString(STORAGE_UNDO_TYPE_KEY, STORAGE_UNDO_TYPE_CUT);
        tag.putString(STORAGE_UNDO_DIMENSION_KEY, level.dimension().location().toString());
        tag.putDouble(STORAGE_UNDO_ENERGY_KEY, usedPower);

        writeUndoOrigin(tag, origin);
        writeUndoMin(tag, min);
        writeUndoMax(tag, max);
    }

    private void storeUndoPaste(
            ItemStack stack,
            ServerLevel level,
            BlockPos origin,
            BlockPos min,
            BlockPos max,
            double usedPower
    ) {
        CompoundTag tag = stack.getOrCreateTag();

        tag.putString(STORAGE_UNDO_TYPE_KEY, STORAGE_UNDO_TYPE_PASTE);
        tag.putString(STORAGE_UNDO_DIMENSION_KEY, level.dimension().location().toString());
        tag.putDouble(STORAGE_UNDO_ENERGY_KEY, usedPower);

        writeUndoOrigin(tag, origin);
        writeUndoMin(tag, min);
        writeUndoMax(tag, max);
    }

    private static void clearStorageUndo(ItemStack stack) {
        CompoundTag tag = stack.getTag();

        if (tag == null) {
            return;
        }

        tag.remove(STORAGE_UNDO_TYPE_KEY);
        tag.remove(STORAGE_UNDO_DIMENSION_KEY);
        tag.remove(STORAGE_UNDO_ENERGY_KEY);

        tag.remove(STORAGE_UNDO_ORIGIN_X_KEY);
        tag.remove(STORAGE_UNDO_ORIGIN_Y_KEY);
        tag.remove(STORAGE_UNDO_ORIGIN_Z_KEY);

        tag.remove(STORAGE_UNDO_MIN_X_KEY);
        tag.remove(STORAGE_UNDO_MIN_Y_KEY);
        tag.remove(STORAGE_UNDO_MIN_Z_KEY);

        tag.remove(STORAGE_UNDO_MAX_X_KEY);
        tag.remove(STORAGE_UNDO_MAX_Y_KEY);
        tag.remove(STORAGE_UNDO_MAX_Z_KEY);
    }

    private static void writeUndoOrigin(CompoundTag tag, BlockPos pos) {
        tag.putInt(STORAGE_UNDO_ORIGIN_X_KEY, pos.getX());
        tag.putInt(STORAGE_UNDO_ORIGIN_Y_KEY, pos.getY());
        tag.putInt(STORAGE_UNDO_ORIGIN_Z_KEY, pos.getZ());
    }

    private static void writeUndoMin(CompoundTag tag, BlockPos pos) {
        tag.putInt(STORAGE_UNDO_MIN_X_KEY, pos.getX());
        tag.putInt(STORAGE_UNDO_MIN_Y_KEY, pos.getY());
        tag.putInt(STORAGE_UNDO_MIN_Z_KEY, pos.getZ());
    }

    private static void writeUndoMax(CompoundTag tag, BlockPos pos) {
        tag.putInt(STORAGE_UNDO_MAX_X_KEY, pos.getX());
        tag.putInt(STORAGE_UNDO_MAX_Y_KEY, pos.getY());
        tag.putInt(STORAGE_UNDO_MAX_Z_KEY, pos.getZ());
    }

    private static BlockPos readUndoOrigin(CompoundTag tag) {
        return new BlockPos(
                tag.getInt(STORAGE_UNDO_ORIGIN_X_KEY),
                tag.getInt(STORAGE_UNDO_ORIGIN_Y_KEY),
                tag.getInt(STORAGE_UNDO_ORIGIN_Z_KEY)
        );
    }

    private static BlockPos readUndoMin(CompoundTag tag) {
        return new BlockPos(
                tag.getInt(STORAGE_UNDO_MIN_X_KEY),
                tag.getInt(STORAGE_UNDO_MIN_Y_KEY),
                tag.getInt(STORAGE_UNDO_MIN_Z_KEY)
        );
    }

    private static BlockPos readUndoMax(CompoundTag tag) {
        return new BlockPos(
                tag.getInt(STORAGE_UNDO_MAX_X_KEY),
                tag.getInt(STORAGE_UNDO_MAX_Y_KEY),
                tag.getInt(STORAGE_UNDO_MAX_Z_KEY)
        );
    }

    private @Nullable BlockBounds computePlacedBounds(CompoundTag templateTag, BlockPos origin) {
        List<TemplateUtil.BlockInfo> blocks = TemplateUtil.parseBlocksFromTag(templateTag);

        if (blocks.isEmpty()) {
            return null;
        }

        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(templateTag);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;

        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (TemplateUtil.BlockInfo blockInfo : blocks) {
            BlockPos localPos = blockInfo.pos();

            BlockPos worldPos = new BlockPos(
                    origin.getX() + localPos.getX() - energyOrigin.getX(),
                    origin.getY() + localPos.getY() - energyOrigin.getY(),
                    origin.getZ() + localPos.getZ() - energyOrigin.getZ()
            );

            minX = Math.min(minX, worldPos.getX());
            minY = Math.min(minY, worldPos.getY());
            minZ = Math.min(minZ, worldPos.getZ());

            maxX = Math.max(maxX, worldPos.getX());
            maxY = Math.max(maxY, worldPos.getY());
            maxZ = Math.max(maxZ, worldPos.getZ());
        }

        return new BlockBounds(
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ)
        );
    }

    private void clearStoredStructure(
            ServerLevel level,
            Player player,
            ItemStack stack,
            String id
    ) {
        try {
            StructureToolStructureStore.delete(level.getServer(), id);
        } catch (IOException ignored) {
        }

        StructureToolStackState.clearStructure(stack);
        StructureToolStackState.clearSelection(stack);
        StructureToolStackState.resetPreviewSideMap(stack);

        TemplateUtil.setTemplateOffset(stack.getOrCreateTag(), BlockPos.ZERO);
        TemplateUtil.setEnergyOrigin(stack.getOrCreateTag(), BlockPos.ZERO);

        if (player instanceof ServerPlayer serverPlayer) {
            StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
        }
    }

    private boolean hasPlacementCollision(ServerLevel level, CompoundTag templateTag, BlockPos origin) {
        List<TemplateUtil.BlockInfo> blocks = TemplateUtil.parseBlocksFromTag(templateTag);
        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(templateTag);

        int minBuildY = level.getMinBuildHeight();
        int maxBuildY = level.getMaxBuildHeight();

        for (TemplateUtil.BlockInfo blockInfo : blocks) {
            BlockPos localPos = blockInfo.pos();

            BlockPos worldPos = new BlockPos(
                    origin.getX() + localPos.getX() - energyOrigin.getX(),
                    origin.getY() + localPos.getY() - energyOrigin.getY(),
                    origin.getZ() + localPos.getZ() - energyOrigin.getZ()
            );

            if (worldPos.getY() < minBuildY || worldPos.getY() >= maxBuildY) {
                return true;
            }

            BlockState existing = level.getBlockState(worldPos);

            if (existing.isAir() || existing.canBeReplaced()) {
                continue;
            }

            return true;
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag advancedTooltips) {
        super.appendHoverText(stack, level, tooltip, advancedTooltips);

        if (!CrazyConfig.COMMON.PORTABLE_SPATIAL_STORAGE_ENABLED.get()) {
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED.getTranslationKey())
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED_CONFIG.getTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private record BlockBounds(BlockPos min, BlockPos max) {
    }
}