package net.oktawia.crazyae2addons.mobstorage;

import appeng.client.gui.implementations.UpgradeableScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import appeng.client.gui.style.ScreenStyle;

public class MobFormationPlaneScreen<C extends MobFormationPlaneMenu> extends UpgradeableScreen<C> {
    public MobFormationPlaneScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        widgets.addOpenPriorityButton();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
    }
}

