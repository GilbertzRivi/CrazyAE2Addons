package net.oktawia.crazyae2addons.logic.viewcell;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class TagViewCellHost extends ItemMenuHost {

    public TagViewCellHost(Player player, @Nullable Integer inventorySlot, ItemStack stack) {
        super(player, inventorySlot, stack);
    }
}