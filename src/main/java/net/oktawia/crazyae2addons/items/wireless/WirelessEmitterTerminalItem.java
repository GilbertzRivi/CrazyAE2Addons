package net.oktawia.crazyae2addons.items.wireless;

import appeng.api.features.IGridLinkableHandler;
import appeng.api.ids.AEComponents;
import appeng.menu.locator.ItemMenuHostLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class WirelessEmitterTerminalItem extends ItemWT {

    public static final IGridLinkableHandler LINKABLE_HANDLER = new LinkableHandler();

    public WirelessEmitterTerminalItem() {
        super();
    }

    @Override
    public MenuType<?> getMenuType(ItemMenuHostLocator locator, Player player) {
        return WirelessEmitterTerminalMenu.TYPE;
    }

    private static class LinkableHandler implements IGridLinkableHandler {
        @Override
        public boolean canLink(ItemStack stack) {
            return stack.getItem() instanceof WirelessEmitterTerminalItem;
        }

        @Override
        public void link(ItemStack itemStack, GlobalPos pos) {
            itemStack.set(AEComponents.WIRELESS_LINK_TARGET, pos);
        }

        @Override
        public void unlink(ItemStack itemStack) {
            itemStack.remove(AEComponents.WIRELESS_LINK_TARGET);
        }
    }
}
