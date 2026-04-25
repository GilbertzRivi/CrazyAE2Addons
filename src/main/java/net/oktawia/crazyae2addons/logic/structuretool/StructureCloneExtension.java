package net.oktawia.crazyae2addons.logic.structuretool;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface StructureCloneExtension {

    boolean collectMetadata(
            ServerLevel level,
            BlockPos pos,
            BlockEntity be,
            @Nullable CompoundTag rawBeTag,
            AbstractStructureCaptureToolItem.RequirementSink requirements,
            CompoundTag blockEntry
    );

    boolean handlesRequirements(BlockState state, @Nullable CompoundTag rawBeTag);

    Optional<PlacementPlan> buildPlacementPlan(
            ServerLevel level,
            Player player,
            BlockState state,
            @Nullable CompoundTag rawBeTag,
            @Nullable CompoundTag blockMetadata,
            ClonerPasteContext ctx
    );

    void onBlockPlaced(
            ServerLevel level,
            BlockPos pos,
            @Nullable BlockEntity be,
            @Nullable CompoundTag blockMetadata
    );
}