package net.oktawia.crazyae2addons.compat.gtceu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolPreviewDispatcher;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStructureStore;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class PortableSpatialStorageGTCEu extends PortableSpatialStorage {

    private static final double POWER_PER_BLOCK_PASTE = 1.0;

    public PortableSpatialStorageGTCEu(Item.Properties properties) {
        super(properties);
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
        BlockHitResult hit = rayTrace(level, player, 50.0D);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pasteOrigin = hit.getBlockPos().relative(hit.getDirection());
            pasteWithGreg(level, player, stack, pasteOrigin);
        } else {
            showHud(player, Component.translatable(LangDefs.NO_BLOCK_IN_RANGE.getTranslationKey()));
        }
    }

    @Override
    protected void onUseOnWithStoredStructure(ServerLevel level, Player player, ItemStack stack, BlockPos clickedFacePos) {
        pasteWithGreg(level, player, stack, clickedFacePos);
    }

    private double calculateStructurePower(CompoundTag templateTag, BlockPos localOrigin, double baseCostPerBlock) {
        return StructureToolUtil.calculatePreviewStructurePower(templateTag, localOrigin, baseCostPerBlock);
    }

    private void pasteWithGreg(ServerLevel level, Player player, ItemStack stack, BlockPos origin) {
        String id = StructureToolStackState.getStructureId(stack);
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
            StructureToolStackState.clearStructure(stack);
            StructureToolStackState.clearSelection(stack);
            StructureToolStackState.resetPreviewSideMap(stack);
            TemplateUtil.setTemplateOffset(stack.getOrCreateTag(), BlockPos.ZERO);
            TemplateUtil.setEnergyOrigin(stack.getOrCreateTag(), BlockPos.ZERO);

            if (player instanceof ServerPlayer serverPlayer) {
                StructureToolPreviewDispatcher.sendPreviewToPlayer(serverPlayer, null);
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

        GTCEuPasteCompat.schedulePostPlacementInit(level, placementOrigin, savedTag);

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
}