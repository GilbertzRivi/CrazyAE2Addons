package net.oktawia.crazyae2addons.logic.structuretool;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record PlacementPlan(
        boolean shouldPlace,
        @Nullable BlockState stateToPlace,
        @Nullable CompoundTag blockEntityTag,
        List<ItemStack> consumedStacks
) {
    public static PlacementPlan none() {
        return new PlacementPlan(false, null, null, List.of());
    }
}