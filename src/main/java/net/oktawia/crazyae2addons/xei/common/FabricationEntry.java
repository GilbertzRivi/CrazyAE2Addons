package net.oktawia.crazyae2addons.xei.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public record FabricationEntry(
        ResourceLocation recipeId,
        ItemStack input,
        ItemStack output,
        @Nullable ResourceLocation requiredKey,
        @Nullable String requiredLabel
) {}
