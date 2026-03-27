package net.oktawia.crazyae2addons.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.menus.MobKeySelectorMenu;
import org.jetbrains.annotations.Nullable;

public class MobKeySelectorHost extends ItemMenuHost {
    public MobKeySelectorMenu menu;

    public MobKeySelectorHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);
    }

    public void setMenu(MobKeySelectorMenu menu) { this.menu = menu; }
}
