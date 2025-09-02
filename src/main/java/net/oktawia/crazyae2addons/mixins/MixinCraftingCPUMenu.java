package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftingCPUMenu;
import net.oktawia.crazyae2addons.interfaces.ICraftingMenuCancellAll;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = CraftingCPUMenu.class, remap = false)
public abstract class MixinCraftingCPUMenu implements ICraftingMenuCancellAll {

    @Shadow @Final private IGrid grid;

    @Unique
    public void cancellAllCrafting(){
        grid.getCraftingService().getCpus().forEach(ICraftingCPU::cancelJob);
    }

}
