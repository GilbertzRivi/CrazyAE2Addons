package net.oktawia.crazyae2addons.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.core.definitions.AEBlocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class CpuPrioHost extends ItemMenuHost {
    public CpuPrioHost(Player player, int slot, ItemStack stack) { super(player, slot, stack); }

    public ItemStack getMainMenuIcon() {
        return new ItemStack(AEBlocks.CRAFTING_UNIT.asItem());
    }
}
