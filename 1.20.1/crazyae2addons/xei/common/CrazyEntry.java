package net.oktawia.crazyae2addons.xei.common;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record CrazyEntry(ResourceLocation structureId, Component name, List<ItemStack> requiredItems, ItemStack output) {}
