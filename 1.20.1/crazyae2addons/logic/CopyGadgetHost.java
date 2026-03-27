package net.oktawia.crazyae2addons.logic;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CopyGadgetHost extends GadgetHost {

    public CopyGadgetHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);
    }
}