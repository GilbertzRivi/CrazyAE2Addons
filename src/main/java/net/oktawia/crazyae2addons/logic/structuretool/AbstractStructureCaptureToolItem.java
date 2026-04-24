package net.oktawia.crazyae2addons.logic.structuretool;

import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.localization.Tooltips;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.util.SettingsFrom;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.ShowHudMessagePacket;
import net.oktawia.crazyae2addons.util.NbtUtil;
import net.oktawia.crazyae2addons.util.StructureToolKeys;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractStructureCaptureToolItem extends WirelessTerminalItem implements IMenuItem, IUpgradeableObject {

    protected static final int DEFAULT_BASE_POWER = 200_000;
    protected static final int DEFAULT_UPGRADE_SLOTS = 4;
    protected static final double POWER_PER_BLOCK_PASTE = 1.0D;
    protected static final String CURRENT_POWER_NBT_KEY = "internalCurrentPower";
    private static final String WAS_HELD_IN_HAND_NBT_KEY = "wasHeldInHand";

    private static final int HUD_COLOR_CYAN = 0x55FFFF;
    private static final int HUD_COLOR_RED = 0xFF4040;
    private static final int HUD_TIME_SHORT = 60;
    private static final int HUD_TIME_MEDIUM = 80;

    private final int upgradeSlots;

    protected AbstractStructureCaptureToolItem(int basePower, int upgradeSlots, Item.Properties properties) {
        super(() -> basePower, properties.stacksTo(1));
        this.upgradeSlots = upgradeSlots;
    }

    @FunctionalInterface
    public interface RequirementSink {
        void add(ItemStack stack);
    }

    protected abstract MenuType<?> getToolMenuType();

    protected abstract boolean removeCapturedBlocks();

    protected abstract Component getCaptureSuccessMessage();

    protected abstract Component getStoredStructureActionNotImplementedMessage();

    protected double getPowerPerBlockCapture() {
        return 1.0D;
    }

    protected boolean collectAdditionalBlockMetadata(
            @Nullable CompoundTag rawBeTag,
            BlockEntity be,
            Player player,
            RequirementSink requirements,
            CompoundTag blockEntry
    ) {
        return false;
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
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
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        boolean hasStructure = StructureToolStackState.hasStructure(stack);

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
                    showHud(player, Component.translatable(LangDefs.CORNER_A_SELECTED.getTranslationKey()));
                }
            } else if (selectionB == null) {
                StructureToolStackState.setSelectionB(stack, clickedPos.immutable());
                StructureToolStackState.setSourceFacing(stack, player.getDirection());

                if (!level.isClientSide()) {
                    showHud(player, Component.translatable(LangDefs.CORNER_B_SELECTED.getTranslationKey()));
                }
            } else {
                StructureToolStackState.clearSelection(stack);

                if (!level.isClientSide()) {
                    showHud(player, Component.translatable(LangDefs.SELECTION_RESTARTED.getTranslationKey()));
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (hasStructure) {
            if (!level.isClientSide()) {
                onUseOnWithStoredStructure((ServerLevel) level, player, stack, clickedPos.relative(context.getClickedFace()));
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

        BlockPos origin = b;
        StructureToolStackState.setOrigin(stack, origin);

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

        BlockPos size = max.subtract(min).offset(1, 1, 1);

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, false, Blocks.STRUCTURE_VOID);

        CompoundTag savedTag = TemplateUtil.stripAirFromTag(template.save(new CompoundTag()));
        TemplateUtil.setTemplateOffset(savedTag, BlockPos.ZERO);

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
                getPowerPerBlockCapture()
        );

        if (!tryUsePower(player, stack, requiredPower)) {
            showNotEnoughPower(player, stack, requiredPower);
            return;
        }

        String id = UUID.randomUUID().toString();

        try {
            StructureToolStructureStore.save(level.getServer(), id, savedTag);
            StructureToolStackState.setStructureId(stack, id);
            StructureToolStackState.clearSelection(stack);
            StructureToolStackState.resetPreviewSideMap(stack);
        } catch (IOException exception) {
            showHud(player, Component.translatable(LangDefs.FAILED_TO_SAVE_STRUCTURE.getTranslationKey()));
            return;
        }

        if (removeCapturedBlocks()) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    for (int x = min.getX(); x <= max.getX(); x++) {
                        BlockPos worldPos = new BlockPos(x, y, z);
                        level.removeBlockEntity(worldPos);
                        level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, savedTag);
        }

        showHud(player, getCaptureSuccessMessage());
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
                    BlockEntity be = level.getBlockEntity(worldPos);
                    CompoundTag rawBeTag = rawBeTags.get(localPos);

                    if (be instanceof CableBusBlockEntity) {
                        collectCableBusRequirements(rawBeTag, requirements);
                    } else {
                        addBaseBlockRequirement(level, worldPos, requirements);
                    }

                    if (be == null) {
                        continue;
                    }

                    CompoundTag blockEntry = new CompoundTag();
                    blockEntry.put(StructureToolKeys.CLONE_KEY_POS, writeBlockPos(localPos));

                    boolean hasAnyData = false;

                    if (be instanceof AEBaseBlockEntity abbe) {
                        CompoundTag settings = new CompoundTag();
                        try {
                            abbe.exportSettings(SettingsFrom.MEMORY_CARD, settings, null);
                        } catch (Throwable ignored) {
                        }

                        if (!settings.isEmpty()) {
                            blockEntry.put(StructureToolKeys.CLONE_KEY_SETTINGS, settings);
                            hasAnyData = true;
                        }
                    }

                    if (be instanceof IUpgradeableObject uo) {
                        IUpgradeInventory upgrades = uo.getUpgrades();
                        if (!upgrades.isEmpty()) {
                            for (ItemStack upgrade : upgrades) {
                                if (!upgrade.isEmpty()) {
                                    requirements.addDefault(upgrade);
                                }
                            }

                            upgrades.writeToNBT(blockEntry, StructureToolKeys.CLONE_KEY_UPGRADES);
                            hasAnyData = true;
                        }
                    }

                    if (be instanceof CableBusBlockEntity cbbe) {
                        CompoundTag partsTag = new CompoundTag();

                        for (Direction dir : Direction.values()) {
                            var part = cbbe.getPart(dir);
                            if (part == null) {
                                continue;
                            }

                            CompoundTag partEntry = new CompoundTag();
                            boolean hasPartData = false;

                            CompoundTag partSettings = new CompoundTag();
                            try {
                                part.exportSettings(SettingsFrom.MEMORY_CARD, partSettings);
                            } catch (Throwable ignored) {
                            }

                            if (!partSettings.isEmpty()) {
                                partEntry.put(StructureToolKeys.CLONE_KEY_SETTINGS, partSettings);
                                hasPartData = true;
                            }

                            if (part instanceof IUpgradeableObject partUpgradable) {
                                IUpgradeInventory upgrades = partUpgradable.getUpgrades();
                                if (!upgrades.isEmpty()) {
                                    for (ItemStack upgrade : upgrades) {
                                        if (!upgrade.isEmpty()) {
                                            requirements.addDefault(upgrade);
                                        }
                                    }

                                    upgrades.writeToNBT(partEntry, StructureToolKeys.CLONE_KEY_UPGRADES);
                                    hasPartData = true;
                                }
                            }

                            if (hasPartData) {
                                partsTag.put(TemplateUtil.directionKey(dir), partEntry);
                            }
                        }

                        if (!partsTag.isEmpty()) {
                            blockEntry.put(StructureToolKeys.CLONE_KEY_PARTS, partsTag);
                            hasAnyData = true;
                        }
                    }

                    if (collectAdditionalBlockMetadata(rawBeTag, be, player, requirements::add, blockEntry)) {
                        hasAnyData = true;
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

    private static void collectCableBusRequirements(@Nullable CompoundTag rawBeTag, RequirementAccumulator requirements) {
        if (rawBeTag == null) {
            return;
        }

        for (String key : StructureToolKeys.AE2_CABLE_BUS_KEYS) {
            if (!rawBeTag.contains(key)) {
                continue;
            }

            collectNestedSavedItemStacks(rawBeTag.get(key), requirements);
        }
    }

    private static void collectNestedSavedItemStacks(@Nullable Tag tag, RequirementAccumulator requirements) {
        if (tag == null) {
            return;
        }

        if (tag instanceof CompoundTag compoundTag) {
            ItemStack stack = NbtUtil.tryReadSavedItemStack(compoundTag);
            if (!stack.isEmpty()) {
                requirements.addDefault(stack);
                return;
            }

            for (String key : compoundTag.getAllKeys()) {
                collectNestedSavedItemStacks(compoundTag.get(key), requirements);
            }
            return;
        }

        if (tag instanceof ListTag listTag) {
            for (int i = 0; i < listTag.size(); i++) {
                collectNestedSavedItemStacks(listTag.get(i), requirements);
            }
        }
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
}