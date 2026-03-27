package net.oktawia.crazyae2addons.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ViewCellHost extends ItemMenuHost {
    public ViewCellHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);
    }
}
