package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.RecipeFabricatorMenu;

public class RecipeFabricatorScreen<C extends RecipeFabricatorMenu> extends UpgradeableScreen<C> {

    public RecipeFabricatorScreen(C menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        int p = getMenu().progress != null ? getMenu().progress : 0;
        int d = getMenu().duration != null ? getMenu().duration : 10;
        int pct = d > 0 ? (int) Math.round(100.0 * p / d) : 0;

        setTextContent("progress", Component.literal("Progress: " + pct + "%"));
    }
}
