package net.oktawia.crazyae2addons.items;

import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.util.SettingsFrom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.structuretool.AbstractStructureCaptureToolItem;
import net.oktawia.crazyae2addons.logic.structuretool.PortableSpatialClonerHost;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolPreviewDispatcher;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStructureStore;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.util.NbtUtil;
import net.oktawia.crazyae2addons.util.StructureToolKeys;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortableSpatialCloner extends AbstractStructureCaptureToolItem {

    public PortableSpatialCloner(Item.Properties properties) {
        super(DEFAULT_BASE_POWER, DEFAULT_UPGRADE_SLOTS + 1, properties);
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

            BlockState stateToPlace = blockInfo.state();
            CompoundTag rawBeTag = blockInfo.blockEntityTag();
            CompoundTag blockMetadata = metadataByPos.get(localPos);

            BlockState existing = level.getBlockState(worldPos);
            if (hasCollision(existing, stateToPlace)) {
                skipped++;
                continue;
            }

            boolean success;
            if (isAe2CableBusTag(rawBeTag)) {
                success = placeCableBusBestEffort(level, worldPos, stateToPlace, rawBeTag, blockMetadata, player, toolStack);
            } else {
                success = placeRegularBlockBestEffort(level, worldPos, stateToPlace, rawBeTag, blockMetadata, player, toolStack);
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

    protected boolean placeRegularBlockBestEffort(
            ServerLevel level,
            BlockPos worldPos,
            BlockState stateToPlace,
            @Nullable CompoundTag rawBeTag,
            @Nullable CompoundTag blockMetadata,
            Player player,
            ItemStack toolStack
    ) {
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

        applyAe2BlockMetadataAfterPlacement(level, worldPos, blockMetadata);

        if (!player.isCreative()) {
            ItemStack required = getRequiredBlockItem(stateToPlace);
            if (!consumeForPaste(level, player, toolStack, required, 1)) {
                return false;
            }
        }

        return true;
    }

    protected boolean placeCableBusBestEffort(
            ServerLevel level,
            BlockPos worldPos,
            BlockState stateToPlace,
            CompoundTag rawBeTag,
            @Nullable CompoundTag blockMetadata,
            Player player,
            ItemStack toolStack
    ) {
        CableBusPlacementPlan plan = buildCableBusPlacementPlan(level, player, toolStack, rawBeTag);
        if (!plan.shouldPlace()) {
            return false;
        }

        if (!placeBlockAndLoadTag(level, worldPos, stateToPlace, plan.filteredTag())) {
            return false;
        }

        applyAe2CableBusMetadataAfterPlacement(level, worldPos, blockMetadata);

        if (!player.isCreative()) {
            for (ItemStack cost : plan.consumedStacks()) {
                if (!consumeForPaste(level, player, toolStack, cost, 1)) {
                    return false;
                }
            }
        }

        return true;
    }

    protected CableBusPlacementPlan buildCableBusPlacementPlan(
            ServerLevel level,
            Player player,
            ItemStack toolStack,
            CompoundTag rawBeTag
    ) {
        CompoundTag filtered = createMinimalAeCableBusBaseTag(rawBeTag);
        List<ItemStack> toConsume = new ArrayList<>();
        Map<Item, Integer> reserved = new HashMap<>();
        boolean keptAnything = false;

        for (String key : StructureToolKeys.AE2_CABLE_BUS_KEYS) {
            if (!rawBeTag.contains(key, Tag.TAG_COMPOUND)) {
                continue;
            }

            CompoundTag rawSection = rawBeTag.getCompound(key);
            CompoundTag minimalSection = createMinimalAePartTag(rawSection);
            if (minimalSection.isEmpty()) {
                continue;
            }

            ItemStack representative = NbtUtil.tryReadSavedItemStack(minimalSection);
            boolean keepSection = player.isCreative()
                    || (!representative.isEmpty()
                    && canReserveForPaste(level, player, toolStack, reserved, representative, 1));

            if (keepSection) {
                filtered.put(key, minimalSection);
                keptAnything = true;

                if (!player.isCreative() && !representative.isEmpty()) {
                    ItemStack normalized = representative.copy();
                    normalized.setCount(1);
                    normalized.setTag(null);
                    toConsume.add(normalized);
                }
            }
        }

        return keptAnything
                ? new CableBusPlacementPlan(true, filtered, toConsume)
                : new CableBusPlacementPlan(false, filtered, List.of());
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

    protected boolean isAe2CableBusTag(@Nullable CompoundTag rawBeTag) {
        if (rawBeTag == null) {
            return false;
        }

        for (String key : StructureToolKeys.AE2_CABLE_BUS_KEYS) {
            if (rawBeTag.contains(key)) {
                return true;
            }
        }

        return false;
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

    protected boolean consumeFromPlayerInventory(Player player, ItemStack wanted, int amount) {
        return consumeFromPlayerInventoryPartial(player, wanted, amount) >= amount;
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
            BlockPos pos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
            out.put(pos, blockEntry);
        }

        return out;
    }

    protected CompoundTag createMinimalAeCableBusBaseTag(@Nullable CompoundTag rawBeTag) {
        if (rawBeTag == null) {
            return new CompoundTag();
        }

        CompoundTag out = new CompoundTag();
        NbtUtil.copyStringIfPresent(rawBeTag, out, "id");
        return out;
    }

    protected CompoundTag createMinimalAePartTag(@Nullable CompoundTag rawPartTag) {
        if (rawPartTag == null) {
            return new CompoundTag();
        }

        CompoundTag out = new CompoundTag();
        NbtUtil.copyStringIfPresent(rawPartTag, out, "id");
        NbtUtil.copyByteIfPresent(rawPartTag, out, "output");
        return out;
    }

    protected void applyAe2BlockMetadataAfterPlacement(
            ServerLevel level,
            BlockPos worldPos,
            @Nullable CompoundTag blockMetadata
    ) {
        if (blockMetadata == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(worldPos);
        if (be == null) {
            return;
        }

        if (be instanceof AEBaseBlockEntity abbe && blockMetadata.contains(StructureToolKeys.CLONE_KEY_SETTINGS, Tag.TAG_COMPOUND)) {
            try {
                abbe.importSettings(SettingsFrom.MEMORY_CARD, blockMetadata.getCompound(StructureToolKeys.CLONE_KEY_SETTINGS), null);
            } catch (Throwable ignored) {
            }
        }

        if (be instanceof IUpgradeableObject upgradable && blockMetadata.contains(StructureToolKeys.CLONE_KEY_UPGRADES)) {
            try {
                upgradable.getUpgrades().readFromNBT(blockMetadata, StructureToolKeys.CLONE_KEY_UPGRADES);
            } catch (Throwable ignored) {
            }
        }

        be.setChanged();
        BlockState state = level.getBlockState(worldPos);
        level.sendBlockUpdated(worldPos, state, state, 3);
    }

    protected void applyAe2CableBusMetadataAfterPlacement(
            ServerLevel level,
            BlockPos worldPos,
            @Nullable CompoundTag blockMetadata
    ) {
        if (blockMetadata == null) {
            return;
        }

        BlockEntity be = level.getBlockEntity(worldPos);
        if (!(be instanceof CableBusBlockEntity cbbe)) {
            return;
        }

        if (!blockMetadata.contains(StructureToolKeys.CLONE_KEY_PARTS, Tag.TAG_COMPOUND)) {
            cbbe.setChanged();
            BlockState state = level.getBlockState(worldPos);
            level.sendBlockUpdated(worldPos, state, state, 3);
            return;
        }

        CompoundTag partsTag = blockMetadata.getCompound(StructureToolKeys.CLONE_KEY_PARTS);

        for (Direction dir : Direction.values()) {
            String key = TemplateUtil.directionKey(dir);
            if (!partsTag.contains(key, Tag.TAG_COMPOUND)) {
                continue;
            }

            CompoundTag partEntry = partsTag.getCompound(key);
            var part = cbbe.getPart(dir);
            if (part == null) {
                continue;
            }

            if (partEntry.contains(StructureToolKeys.CLONE_KEY_SETTINGS, Tag.TAG_COMPOUND)) {
                try {
                    part.importSettings(SettingsFrom.MEMORY_CARD, partEntry.getCompound(StructureToolKeys.CLONE_KEY_SETTINGS), null);
                } catch (Throwable ignored) {
                }
            }

            if (part instanceof IUpgradeableObject partUpgradable && partEntry.contains(StructureToolKeys.CLONE_KEY_UPGRADES)) {
                try {
                    partUpgradable.getUpgrades().readFromNBT(partEntry, StructureToolKeys.CLONE_KEY_UPGRADES);
                } catch (Throwable ignored) {
                }
            }
        }

        cbbe.setChanged();
        BlockState state = level.getBlockState(worldPos);
        level.sendBlockUpdated(worldPos, state, state, 3);
    }

    protected record CableBusPlacementPlan(
            boolean shouldPlace,
            CompoundTag filteredTag,
            List<ItemStack> consumedStacks
    ) {
    }
}