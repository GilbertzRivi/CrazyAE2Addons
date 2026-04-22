package net.oktawia.crazyae2addons.items;

import appeng.api.config.Actionable;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.localization.Tooltips;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.gtceu.GTCEuPasteCompat;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStackState;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStructureStore;
import net.oktawia.crazyae2addons.logic.cutpaste.PortableSpatialStorageHost;
import net.oktawia.crazyae2addons.logic.cutpaste.PortableSpatialStoragePreviewDispatcher;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.ShowHudMessagePacket;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PortableSpatialStorage extends WirelessTerminalItem implements IMenuItem, IUpgradeableObject {

    private static final int BASE_POWER = 200_000;
    private static final int UPGRADE_SLOTS = 4;
    private static final double POWER_PER_BLOCK_CUT = 1.0;
    private static final double POWER_PER_BLOCK_PASTE = 1.0;
    private static final String CURRENT_POWER_NBT_KEY = "internalCurrentPower";
    private static final String ENERGY_ORIGIN_NBT_KEY = "energyOrigin";
    private static final String WAS_HELD_IN_HAND_NBT_KEY = "wasHeldInHand";

    private static final int HUD_COLOR_CYAN = 0x55FFFF;
    private static final int HUD_COLOR_RED = 0xFF4040;
    private static final int HUD_TIME_SHORT = 60;
    private static final int HUD_TIME_MEDIUM = 80;

    public PortableSpatialStorage(Item.Properties properties) {
        super(() -> BASE_POWER, properties.stacksTo(1));
    }

    public static ItemStack findHeld(@Nullable Player player) {
        if (player == null) return ItemStack.EMPTY;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof PortableSpatialStorage) return mainHand;

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof PortableSpatialStorage) return offHand;

        return ItemStack.EMPTY;
    }

    public static ItemStack findActive(@Nullable Player player) {
        if (player == null) return ItemStack.EMPTY;

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof PortableSpatialStorage && CutPasteStackState.hasStructure(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof PortableSpatialStorage && CutPasteStackState.hasStructure(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public double getChargeRate(ItemStack stack) {
        return 800.0D + 800.0D * Upgrades.getEnergyCardMultiplier(getUpgrades(stack));
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, UPGRADE_SLOTS, this::onUpgradesChanged);
    }

    private void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades) {
        setAEMaxPowerMultiplier(stack, 1 + Upgrades.getEnergyCardMultiplier(upgrades));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag advancedTooltips) {
        final CompoundTag tag = stack.getTag();
        double internalCurrentPower = 0;
        final double internalMaxPower = this.getAEMaxPower(stack);

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

    private boolean isHeldInHand(Player player, ItemStack stack) {
        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    private boolean tryUsePower(Player player, ItemStack stack, double amount) {
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

    private static ShowHudMessagePacket.Line cyan(Component text) {
        return new ShowHudMessagePacket.Line(text, HUD_COLOR_CYAN);
    }

    private static ShowHudMessagePacket.Line red(Component text) {
        return new ShowHudMessagePacket.Line(text, HUD_COLOR_RED);
    }

    private void showHud(Player player, int durationTicks, ShowHudMessagePacket.Line... lines) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NetworkHandler.sendToPlayer(serverPlayer, new ShowHudMessagePacket(durationTicks, List.of(lines))
        );
    }

    private void showHud(Player player, Component text) {
        showHud(player, HUD_TIME_SHORT, cyan(text));
    }

    private void showNotEnoughPower(Player player, ItemStack stack, double required) {
        int current = (int) Math.floor(getAECurrentPower(stack));
        int needed = (int) Math.ceil(required);

        showHud(
                player,
                HUD_TIME_MEDIUM,
                red(Component.literal("Not enough power")),
                cyan(Component.literal("Need: " + needed + " AE")),
                cyan(Component.literal("Have: " + current + " AE"))
        );
    }

    private static CompoundTag writeBlockPos(BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        return tag;
    }

    private static BlockPos readBlockPos(CompoundTag parent, String key, BlockPos fallback) {
        if (!parent.contains(key, Tag.TAG_COMPOUND)) {
            return fallback;
        }

        CompoundTag tag = parent.getCompound(key);
        return new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    private static double calculatePreviewStructurePower(CompoundTag templateTag, BlockPos localOrigin, double baseCostPerBlock) {
        double total = 0.0D;

        for (TemplateUtil.BlockInfo blockInfo : TemplateUtil.parseBlocksFromTag(templateTag)) {
            BlockPos pos = blockInfo.pos();

            double dx = pos.getX() - localOrigin.getX();
            double dy = pos.getY() - localOrigin.getY();
            double dz = pos.getZ() - localOrigin.getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            total += baseCostPerBlock * distance;
        }

        return Math.max(1.0D, total);
    }

    private double calculateStructurePower(CompoundTag templateTag, BlockPos localOrigin, double baseCostPerBlock) {
        return calculatePreviewStructurePower(templateTag, localOrigin, baseCostPerBlock);
    }

    public static int computeCutPreviewCostAE(Level level, BlockPos a, BlockPos b, BlockPos origin) {
        if (level == null || a == null || b == null || origin == null) {
            return 0;
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

        BlockPos size = max.subtract(min).offset(1, 1, 1);

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, false, Blocks.STRUCTURE_VOID);

        CompoundTag savedTag = template.save(new CompoundTag());
        TemplateUtil.setTemplateOffset(savedTag, BlockPos.ZERO);
        BlockPos localOrigin = origin.subtract(min);

        return (int) Math.ceil(calculatePreviewStructurePower(savedTag, localOrigin, POWER_PER_BLOCK_CUT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            boolean hasStructure = CutPasteStackState.hasStructure(stack);

            if (player.isShiftKeyDown()) {
                openMenu(player, hand);
                return InteractionResultHolder.success(stack);
            }

            if (hasStructure) {
                ServerLevel serverLevel = (ServerLevel) level;
                BlockHitResult hit = rayTrace(serverLevel, player, 50.0D);

                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pasteOrigin = hit.getBlockPos().relative(hit.getDirection());
                    paste(serverLevel, player, stack, pasteOrigin);
                } else {
                    showHud(player, Component.translatable(LangDefs.NO_BLOCK_IN_RANGE.getTranslationKey()));
                }

                return InteractionResultHolder.success(stack);
            }

            if (isWaitingForSecondCorner(stack)) {
                selectSecondCorner((ServerLevel) level, player, stack);
                return InteractionResultHolder.success(stack);
            }

            tryCut((ServerLevel) level, player, stack);
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

        boolean hasStructure = CutPasteStackState.hasStructure(stack);

        if (player.isShiftKeyDown()) {
            if (hasStructure) {
                if (!level.isClientSide()) {
                    openMenu(player, context.getHand());
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }

            BlockPos selectionA = CutPasteStackState.getSelectionA(stack);
            BlockPos selectionB = CutPasteStackState.getSelectionB(stack);

            if (selectionA == null) {
                CutPasteStackState.setSelectionA(stack, clickedPos.immutable());
                if (!level.isClientSide()) {
                    showHud(player, Component.translatable(LangDefs.CORNER_A_SELECTED.getTranslationKey()));
                }
            } else if (selectionB == null) {
                CutPasteStackState.setSelectionB(stack, clickedPos.immutable());
                CutPasteStackState.setOrigin(stack, clickedPos.immutable());
                CutPasteStackState.setSourceFacing(stack, player.getDirection());

                if (!level.isClientSide()) {
                    showHud(player, Component.translatable(LangDefs.CORNER_B_SELECTED.getTranslationKey()));
                }
            } else {
                CutPasteStackState.clearSelection(stack);

                if (!level.isClientSide()) {
                    showHud(player, Component.translatable(LangDefs.SELECTION_RESTARTED.getTranslationKey()));
                }
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (hasStructure) {
            if (!level.isClientSide()) {
                paste(
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

    private static boolean isWaitingForSecondCorner(ItemStack stack) {
        return !CutPasteStackState.hasStructure(stack)
                && CutPasteStackState.getSelectionA(stack) != null
                && CutPasteStackState.getSelectionB(stack) == null;
    }

    private void openMenu(Player player, InteractionHand hand) {
        MenuOpener.open(
                CrazyMenuRegistrar.PORTABLE_SPATIAL_STORAGE_MENU.get(),
                player,
                MenuLocators.forHand(player, hand)
        );
    }

    private void selectSecondCorner(ServerLevel level, Player player, ItemStack stack) {
        BlockHitResult hit = rayTrace(level, player, 50.0D);

        if (hit.getType() != HitResult.Type.BLOCK) {
            showHud(player, Component.translatable(LangDefs.NO_BLOCK_IN_RANGE.getTranslationKey()));
            return;
        }

        BlockPos pos = hit.getBlockPos().immutable();
        CutPasteStackState.setSelectionB(stack, pos);
        CutPasteStackState.setOrigin(stack, pos);
        CutPasteStackState.setSourceFacing(stack, player.getDirection());

        showHud(player, Component.translatable(LangDefs.CORNER_B_SELECTED.getTranslationKey()));
    }

    private void tryCut(ServerLevel level, Player player, ItemStack stack) {
        BlockPos a = CutPasteStackState.getSelectionA(stack);
        BlockPos b = CutPasteStackState.getSelectionB(stack);

        if (a == null || b == null) {
            return;
        }

        BlockPos origin = a;
        CutPasteStackState.setOrigin(stack, origin);

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

        CompoundTag savedTag = template.save(new CompoundTag());
        TemplateUtil.setTemplateOffset(savedTag, BlockPos.ZERO);

        BlockPos localOrigin = origin.subtract(min);
        savedTag.put(ENERGY_ORIGIN_NBT_KEY, writeBlockPos(localOrigin));
        TemplateUtil.setEnergyOrigin(savedTag, localOrigin);
        TemplateUtil.copyPreviewTransformState(savedTag, stack.getOrCreateTag());

        double requiredPower = calculateStructurePower(savedTag, localOrigin, POWER_PER_BLOCK_CUT);

        if (!tryUsePower(player, stack, requiredPower)) {
            showNotEnoughPower(player, stack, requiredPower);
            return;
        }

        String id = UUID.randomUUID().toString();

        try {
            CutPasteStructureStore.save(level.getServer(), id, savedTag);

            CutPasteStackState.setStructureId(stack, id);
            CutPasteStackState.clearSelection(stack);
            CutPasteStackState.resetPreviewSideMap(stack);
        } catch (IOException exception) {
            showHud(player, Component.translatable(LangDefs.FAILED_TO_SAVE_STRUCTURE.getTranslationKey()));
            return;
        }

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    level.removeBlockEntity(new BlockPos(x, y, z));
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PortableSpatialStoragePreviewDispatcher.sendPreviewToPlayer(serverPlayer, savedTag);
        }

        showHud(player, Component.translatable(LangDefs.STRUCTURE_CUT_AND_SAVED.getTranslationKey()));
    }

    private void paste(ServerLevel level, Player player, ItemStack stack, BlockPos origin) {
        String id = CutPasteStackState.getStructureId(stack);
        if (id.isBlank()) {
            return;
        }

        CompoundTag savedTag;
        try {
            savedTag = CutPasteStructureStore.load(level.getServer(), id);
        } catch (IOException exception) {
            showHud(player, Component.translatable(LangDefs.FAILED_TO_LOAD_STRUCTURE.getTranslationKey()));
            return;
        }

        if (savedTag == null) {
            showHud(player, Component.translatable(LangDefs.STORED_STRUCTURE_NOT_FOUND.getTranslationKey()));
            CutPasteStackState.clearStructure(stack);
            CutPasteStackState.clearSelection(stack);
            CutPasteStackState.resetPreviewSideMap(stack);
            TemplateUtil.setTemplateOffset(stack.getOrCreateTag(), BlockPos.ZERO);
            TemplateUtil.setEnergyOrigin(stack.getOrCreateTag(), BlockPos.ZERO);

            if (player instanceof ServerPlayer serverPlayer) {
                PortableSpatialStoragePreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
            }
            return;
        }

        if (hasPlacementCollision(level, savedTag, origin)) {
            showHud(player, Component.translatable(LangDefs.PASTE_COLLISION.getTranslationKey()));
            return;
        }

        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(savedTag);
        double requiredPower = calculateStructurePower(savedTag, energyOrigin, POWER_PER_BLOCK_PASTE);

        if (!tryUsePower(player, stack, requiredPower)) {
            showNotEnoughPower(player, stack, requiredPower);
            return;
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
            return;
        }

        if (IsModLoaded.GTCEU) {
            GTCEuPasteCompat.schedulePostPlacementInit(level, placementOrigin, savedTag);
        }

        try {
            CutPasteStructureStore.delete(level.getServer(), id);
        } catch (IOException ignored) {
        }

        CutPasteStackState.clearStructure(stack);
        CutPasteStackState.clearSelection(stack);
        CutPasteStackState.resetPreviewSideMap(stack);
        TemplateUtil.setTemplateOffset(stack.getOrCreateTag(), BlockPos.ZERO);
        TemplateUtil.setEnergyOrigin(stack.getOrCreateTag(), BlockPos.ZERO);

        if (player instanceof ServerPlayer serverPlayer) {
            PortableSpatialStoragePreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
        }

        showHud(player, Component.translatable(LangDefs.STRUCTURE_PASTED.getTranslationKey()));
    }

    private boolean hasPlacementCollision(ServerLevel level, CompoundTag templateTag, BlockPos origin) {
        List<TemplateUtil.BlockInfo> blocks = TemplateUtil.parseBlocksFromTag(templateTag);
        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(templateTag);

        for (TemplateUtil.BlockInfo blockInfo : blocks) {
            BlockPos localPos = blockInfo.pos();
            BlockPos worldPos = new BlockPos(
                    origin.getX() + localPos.getX() - energyOrigin.getX(),
                    origin.getY() + localPos.getY() - energyOrigin.getY(),
                    origin.getZ() + localPos.getZ() - energyOrigin.getZ()
            );

            BlockState existing = level.getBlockState(worldPos);

            if (existing.isAir() || existing.canBeReplaced() || existing.equals(blockInfo.state())) {
                continue;
            }

            return true;
        }

        return false;
    }

    public static BlockHitResult rayTrace(Level level, Player player, double maxDistance) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

        ClipContext context = new ClipContext(
                eye,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );

        HitResult result = level.clip(context);
        if (result instanceof BlockHitResult blockHit && result.getType() == HitResult.Type.BLOCK) {
            return blockHit;
        }

        return BlockHitResult.miss(end, Direction.getNearest(look.x, look.y, look.z), BlockPos.containing(end));
    }

    @Override
    public ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new PortableSpatialStorageHost(player, inventorySlot, stack);
    }
}