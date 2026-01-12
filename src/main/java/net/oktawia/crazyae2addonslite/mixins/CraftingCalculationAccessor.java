package net.oktawia.crazyae2addonslite.mixins;

import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.crafting.CraftingCalculation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CraftingCalculation.class)
public interface CraftingCalculationAccessor {
    @Accessor("simRequester")
    ICraftingSimulationRequester getSimRequester();
}
