package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public record FabricationEntry(
        ResourceLocation id,
        List<ItemStack> inputs,
        ItemStack output,
        FluidStack fluidInput,
        FluidStack fluidOutput,
        @Nullable ResourceLocation requiredKey,
        @Nullable String label
) { }
