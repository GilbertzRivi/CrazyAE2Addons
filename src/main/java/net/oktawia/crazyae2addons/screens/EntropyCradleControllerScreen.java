package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.EntropyCradleControllerMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class EntropyCradleControllerScreen<C extends EntropyCradleControllerMenu> extends AEBaseScreen<C> implements CrazyScreen {
    private static final String NAME = "entropy_cradle_controller";

    static {
        CrazyScreen.i18n(NAME, "enable_preview", "Enable/Disable preview");
        CrazyScreen.i18n(NAME, "preview", "Preview: %s");
    }

    public EntropyCradleControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        var prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        prevBtn.setTooltip(Tooltip.create(l10n(NAME, "enable_preview")));
        this.widgets.add("prevbtn", prevBtn);
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent("prev", l10n(NAME, "preview", getMenu().preview));
    }
}
