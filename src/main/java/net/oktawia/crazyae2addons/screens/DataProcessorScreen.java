package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.DataProcessorMenu;
import net.oktawia.crazyae2addons.menus.EjectorMenu;

public class DataProcessorScreen<C extends DataProcessorMenu> extends AEBaseScreen<C> {
    public DataProcessorScreen(
            C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }
}