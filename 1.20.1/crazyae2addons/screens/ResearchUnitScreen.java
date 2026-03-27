package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ProgressBar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.ResearchStationMenu;
import net.oktawia.crazyae2addons.menus.ResearchUnitMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class ResearchUnitScreen<C extends ResearchUnitMenu> extends AEBaseScreen<C> {

    private IconButton prevBtn;

    public ResearchUnitScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.add("coolantBar", new ProgressBar(this.menu.coolantBar, style.getImage("progressCoolant"),
                ProgressBar.Direction.HORIZONTAL,
                Component.translatable("gui.crazyae2addons.research_stored_coolant")));

        prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        this.widgets.add("prevbtn", prevBtn);
    }


    @Override
    public void updateBeforeRender(){
        super.updateBeforeRender();
        prevBtn.setTooltip(Tooltip.create(Component.translatable(getMenu().preview
                ? "gui.crazyae2addons.research_preview_hide"
                : "gui.crazyae2addons.research_preview_show")));
        if (getMenu().formed){
            setTextContent("computation", Component.translatable("gui.crazyae2addons.research_unit_computation").append(String.valueOf(getMenu().computation)).append("/t"));
            setTextContent("coolant", Component.translatable("gui.crazyae2addons.research_unit_coolant").append(String.valueOf(getMenu().computation/4)).append("mb/t"));
            setTextContent("power", Component.translatable("gui.crazyae2addons.research_unit_power").append(String.valueOf(Utils.shortenNumber(getMenu().computation*64))).append("AE/t"));
        } else {
            setTextContent("computation", Component.translatable("gui.crazyae2addons.research_unit_computation").append("0").append("/t"));
            setTextContent("coolant", Component.translatable("gui.crazyae2addons.research_unit_coolant").append("0").append("mb/t"));
            setTextContent("power", Component.translatable("gui.crazyae2addons.research_unit_power").append("0").append("AE/t"));
        }
    }
}