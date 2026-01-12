package net.oktawia.crazyae2addonslite.mixins;

import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.crafting.inv.ListCraftingInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ExecutingCraftingJob.class)
public interface ExecutingCraftingJobAccessor {

    @Accessor("waitingFor")
    ListCraftingInventory getWaitingFor();

    @Accessor("finalOutput")
    GenericStack getFinalOutput();

}