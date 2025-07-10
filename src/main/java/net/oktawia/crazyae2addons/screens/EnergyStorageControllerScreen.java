package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.EnergyStorageControllerMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class EnergyStorageControllerScreen<C extends EnergyStorageControllerMenu> extends AEBaseScreen<C> implements CrazyScreen {
    private static final String NAME = "energy_storage_controller";

    static {
        CrazyScreen.i18n(NAME, "storing", "Storing: %s/%s AE");
        CrazyScreen.i18n(NAME, "preview", "Preview: %s");
        CrazyScreen.i18n(NAME, "enable_preview", "Enable/Disable preview");
    }

    public EnergyStorageControllerScreen(
            C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        var prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        prevBtn.setTooltip(Tooltip.create(l10n(NAME, "enable_preview")));
        this.widgets.add("prevbtn", prevBtn);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent("energy", l10n(NAME, "storing", Utils.shortenNumber(getMenu().energy), Utils.shortenNumber(getMenu().maxEnergy)));
        setTextContent("prev", l10n(NAME, "preview", getMenu().preview));
    }
}
