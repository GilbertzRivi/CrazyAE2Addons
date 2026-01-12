package net.oktawia.crazyae2addonslite.mixins;

import appeng.api.stacks.GenericStack;
import net.pedroksl.advanced_ae.common.logic.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ExecutingCraftingJob.class)
public interface AAEExecutingCraftingJobAccessor {

    @Accessor("waitingFor")
    ListCraftingInventory getWaitingFor();

    @Accessor("finalOutput")
    GenericStack getFinalOutput();
}