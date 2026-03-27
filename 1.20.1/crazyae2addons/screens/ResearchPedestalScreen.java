package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.ResearchPedestalMenu;
import net.oktawia.crazyae2addons.menus.ResearchUnitMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class ResearchPedestalScreen<C extends ResearchPedestalMenu> extends AEBaseScreen<C> {

    public ResearchPedestalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Override
    public void updateBeforeRender(){
        super.updateBeforeRender();
        if (getMenu().validConnection){
            setTextContent("computation", Component.translatable("gui.crazyae2addons.research_pedestal_computation").append(String.valueOf(getMenu().computation)).append("/t"));
        } else {
            setTextContent("computation", Component.translatable("gui.crazyae2addons.research_pedestal_invalid"));
        }
    }
}