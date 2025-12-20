package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.ReinforcedMatterCondenserMenu;

public class ReinforcedMatterCondenserScreen<C extends ReinforcedMatterCondenserMenu> extends AEBaseScreen<C> {

    public ReinforcedMatterCondenserScreen(C menu, Inventory playerInventory, Component title,
                                           ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.add("progressBar", new ProgressBar(this.menu, style.getImage("progressBar"),
                ProgressBar.Direction.VERTICAL,
                Component.translatable("gui.crazyae2addons.reinforced_condenser_singularities")));

        widgets.add("progressBar2", new ProgressBar(this.menu.CellProvider, style.getImage("progressBar"),
                ProgressBar.Direction.VERTICAL,
                Component.translatable("gui.crazyae2addons.reinforced_condenser_cells")));
    }
}