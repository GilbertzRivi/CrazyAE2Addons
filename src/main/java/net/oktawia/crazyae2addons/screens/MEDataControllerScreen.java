package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.MEDataControllerMenu;

public class MEDataControllerScreen<C extends MEDataControllerMenu> extends AEBaseScreen<C> implements CrazyScreen {
    private static final String NAME = "me_data_controller";

    static {
        CrazyScreen.i18n(NAME, "stored_variables", "Currently Stored variables:");
        CrazyScreen.i18n(NAME, "variable_count", "%d/%d");
    }

    public MEDataControllerScreen(
            MEDataControllerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super((C) menu, playerInventory, title, style);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent("info1", l10n(NAME, "stored_variables"));
        setTextContent("info2", l10n(NAME, "variable_count", getMenu().variableNum, getMenu().maxVariables));
    }
}
