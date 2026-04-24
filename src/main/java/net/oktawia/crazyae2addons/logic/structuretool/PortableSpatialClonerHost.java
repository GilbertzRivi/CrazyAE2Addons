package net.oktawia.crazyae2addons.logic.structuretool;

import appeng.api.storage.ISubMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;

public class PortableSpatialClonerHost extends StructureToolHost implements ISubMenuHost {

    public PortableSpatialClonerHost(Player player, int inventorySlot, ItemStack stack) {
        super(player, inventorySlot, stack);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.open(
                CrazyMenuRegistrar.PORTABLE_SPATIAL_CLONER_MENU.get(),
                player,
                MenuLocators.forHand(player, resolveHand(player))
        );
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyItemRegistrar.PORTABLE_SPATIAL_CLONER.get().getDefaultInstance();
    }

    private InteractionHand resolveHand(Player player) {
        ItemStack hostStack = getItemStack();

        if (player.getMainHandItem() == hostStack) {
            return InteractionHand.MAIN_HAND;
        }

        if (player.getOffhandItem() == hostStack) {
            return InteractionHand.OFF_HAND;
        }

        if (ItemStack.isSameItemSameTags(player.getMainHandItem(), hostStack)) {
            return InteractionHand.MAIN_HAND;
        }

        if (ItemStack.isSameItemSameTags(player.getOffhandItem(), hostStack)) {
            return InteractionHand.OFF_HAND;
        }

        return InteractionHand.MAIN_HAND;
    }
}