package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.AmpereMeterMenu;

import java.util.List;

public class AmpereMeterScreen<C extends AmpereMeterMenu> extends AEBaseScreen<C> implements CrazyScreen {
    private static final String NAME = "ampere_meter";
    public ToggleButton direction;

    static {
        CrazyScreen.i18n(NAME, "left_to_right", "Send power from left to right");
        CrazyScreen.i18n(NAME, "right_to_left", "Send power from right to left");
        CrazyScreen.i18n(NAME, "transferring", "Transferring: %s %s");
    }

    public AmpereMeterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        direction = new ToggleButton(Icon.ARROW_RIGHT, Icon.ARROW_LEFT, this::toggleDirection);
        direction.setTooltipOn(List.of(l10n(NAME, "left_to_right")));
        direction.setTooltipOff(List.of(l10n(NAME, "right_to_left")));
        this.widgets.add("direction", direction);
    }

    private void toggleDirection(boolean dir) {
        this.direction.setState(!dir);
        this.getMenu().changeDirection(dir);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        direction.setState(getMenu().direction);
        setTextContent("energy", l10n(NAME, "transferring", getMenu().transfer, getMenu().unit));
    }
}
