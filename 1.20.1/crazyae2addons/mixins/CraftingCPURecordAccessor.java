package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.crafting.ICraftingCPU;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "appeng.menu.me.crafting.CraftingCPURecord", remap = false)
public interface CraftingCPURecordAccessor {
    @Accessor("cpu")
    ICraftingCPU getTheCpu();
}
