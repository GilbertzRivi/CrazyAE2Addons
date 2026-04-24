package net.oktawia.crazyae2addons.mixins.cancelallcrafting;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftingCPUMenu;
import net.oktawia.crazyae2addons.logic.interfaces.ICancelAllCraftingMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CraftingCPUMenu.class, remap = false)
public abstract class MixinCraftingCPUMenu implements ICancelAllCraftingMenu {

    @Shadow
    @Final
    private IGrid grid;

    @Override
    public void cancelAllCrafting() {
        grid.getCraftingService().getCpus().forEach(ICraftingCPU::cancelJob);
    }
}