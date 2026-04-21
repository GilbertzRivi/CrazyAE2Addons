package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStackState;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStructureStore;
import net.oktawia.crazyae2addons.logic.cutpaste.PortableSpatialStorageHost;
import net.oktawia.crazyae2addons.logic.cutpaste.PortableSpatialStoragePreviewDispatcher;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PortableSpatialStorage extends WirelessTerminalItem implements IMenuItem {

    public PortableSpatialStorage(Properties properties) {
        super(() -> 200000, properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player.isShiftKeyDown()) {
            MenuOpener.open(
                    CrazyMenuRegistrar.PORTABLE_SPATIAL_STORAGE_MENU.get(),
                    player,
                    MenuLocators.forHand(player, hand)
            );
            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide() && CutPasteStackState.hasStructure(stack)) {
            ServerLevel serverLevel = (ServerLevel) level;
            BlockHitResult hit = rayTrace(serverLevel, player, 50.0D);

            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pasteOrigin = hit.getBlockPos().relative(hit.getDirection());
                paste(serverLevel, player, stack, pasteOrigin);
            } else {
                player.displayClientMessage(
                        Component.translatable(LangDefs.NO_BLOCK_IN_RANGE.getTranslationKey()),
                        true
                );
            }

            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide()) {
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

        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide() && CutPasteStackState.hasStructure(stack)) {
                paste(
                        (ServerLevel) level,
                        player,
                        stack,
                        clickedPos.relative(context.getClickedFace())
                );
            }
            return InteractionResult.SUCCESS;
        }

        if (CutPasteStackState.hasStructure(stack)) {
            player.displayClientMessage(
                    Component.translatable(LangDefs.PASTE_OR_CLEAR_FIRST.getTranslationKey()),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        BlockPos selectionA = CutPasteStackState.getSelectionA(stack);
        BlockPos selectionB = CutPasteStackState.getSelectionB(stack);

        if (selectionA == null) {
            CutPasteStackState.setSelectionA(stack, clickedPos.immutable());
            player.displayClientMessage(
                    Component.translatable(LangDefs.CORNER_A_SELECTED.getTranslationKey()),
                    true
            );
        } else if (selectionB == null) {
            CutPasteStackState.setSelectionB(stack, clickedPos.immutable());
            CutPasteStackState.setOrigin(stack, clickedPos.immutable());
            CutPasteStackState.setSourceFacing(stack, player.getDirection());

            player.displayClientMessage(
                    Component.translatable(LangDefs.CORNER_B_SELECTED.getTranslationKey()),
                    true
            );
        } else {
            CutPasteStackState.clearSelection(stack);
            CutPasteStackState.setSelectionA(stack, clickedPos.immutable());

            player.displayClientMessage(
                    Component.translatable(LangDefs.SELECTION_RESTARTED.getTranslationKey()),
                    true
            );
        }

        return InteractionResult.SUCCESS;
    }

    private void tryCut(ServerLevel level, Player player, ItemStack stack) {
        BlockPos a = CutPasteStackState.getSelectionA(stack);
        BlockPos b = CutPasteStackState.getSelectionB(stack);
        BlockPos origin = CutPasteStackState.getOrigin(stack);

        if (a == null || b == null || origin == null) {
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

        BlockPos size = max.subtract(min).offset(1, 1, 1);

        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, false, Blocks.STRUCTURE_VOID);

        String id = UUID.randomUUID().toString();
        CompoundTag savedTag;

        try {
            savedTag = template.save(new CompoundTag());
            CutPasteStructureStore.save(level.getServer(), id, savedTag);
            CutPasteStackState.setStructureId(stack, id);
            CutPasteStackState.clearSelection(stack);
        } catch (IOException exception) {
            player.displayClientMessage(
                    Component.translatable(LangDefs.FAILED_TO_SAVE_STRUCTURE.getTranslationKey()),
                    true
            );
            return;
        }

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    level.removeBlock(new BlockPos(x, y, z), false);
                }
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            PortableSpatialStoragePreviewDispatcher.sendPreviewToPlayer(serverPlayer, savedTag);
        }

        player.displayClientMessage(
                Component.translatable(LangDefs.STRUCTURE_CUT_AND_SAVED.getTranslationKey()),
                true
        );
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
            player.displayClientMessage(
                    Component.translatable(LangDefs.FAILED_TO_LOAD_STRUCTURE.getTranslationKey()),
                    true
            );
            return;
        }

        if (savedTag == null) {
            player.displayClientMessage(
                    Component.translatable(LangDefs.STORED_STRUCTURE_NOT_FOUND.getTranslationKey()),
                    true
            );
            CutPasteStackState.clearStructure(stack);
            CutPasteStackState.clearSelection(stack);

            if (player instanceof ServerPlayer serverPlayer) {
                PortableSpatialStoragePreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
            }
            return;
        }

        if (hasPlacementCollision(level, savedTag, origin)) {
            player.displayClientMessage(
                    Component.translatable(LangDefs.PASTE_COLLISION.getTranslationKey()),
                    true
            );
            return;
        }

        StructureTemplate template = new StructureTemplate();
        template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), savedTag);

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(true);

        boolean placed = template.placeInWorld(level, origin, origin, settings, level.random, 3);
        if (!placed) {
            player.displayClientMessage(
                    Component.translatable(LangDefs.FAILED_TO_PASTE_STRUCTURE.getTranslationKey()),
                    true
            );
            return;
        }

        try {
            CutPasteStructureStore.delete(level.getServer(), id);
        } catch (IOException ignored) {
        }

        CutPasteStackState.clearStructure(stack);
        CutPasteStackState.clearSelection(stack);

        if (player instanceof ServerPlayer serverPlayer) {
            PortableSpatialStoragePreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
        }

        player.displayClientMessage(
                Component.translatable(LangDefs.STRUCTURE_PASTED.getTranslationKey()),
                true
        );
    }

    private boolean hasPlacementCollision(ServerLevel level, CompoundTag templateTag, BlockPos origin) {
        List<TemplateUtil.BlockInfo> blocks = TemplateUtil.parseBlocksFromTag(templateTag);

        for (TemplateUtil.BlockInfo blockInfo : blocks) {
            BlockPos worldPos = origin.offset(blockInfo.pos());
            BlockState existing = level.getBlockState(worldPos);

            if (existing.isAir() || existing.canBeReplaced() || existing.equals(blockInfo.state())) {
                continue;
            }

            return true;
        }

        return false;
    }

    private static BlockHitResult rayTrace(ServerLevel level, Player player, double maxDistance) {
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