package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.MobFarmControllerMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class MobFarmControllerScreen<C extends MobFarmControllerMenu> extends UpgradeableScreen<C> {
    public MobFarmControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        var prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        prevBtn.setTooltip(Tooltip.create(Component.literal("Enable/Disable preview")));
        this.widgets.add("prevbtn", prevBtn);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        this.setTextContent("dmgitem", Component.literal("Item to use:"));
        setTextContent("prev", Component.literal("Preview: " + getMenu().preview));
    }
}