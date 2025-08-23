package net.oktawia.crazyae2addons.xei.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ResearchEntry(
        ResourceLocation recipeId,
        List<ItemStack> inputs,
        ItemStack driveOrOutput,
        boolean requiresStabilizer,
        int[] size,
        String label,
        ResourceLocation unlockKey
) {}
