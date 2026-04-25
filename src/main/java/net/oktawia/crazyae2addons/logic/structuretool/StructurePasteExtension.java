package net.oktawia.crazyae2addons.logic.structuretool;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

public interface StructurePasteExtension {
    void onTemplatePasted(ServerLevel level, BlockPos placementOrigin, CompoundTag templateTag);
}