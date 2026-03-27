package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.entities.PenroseInjectionPortBE;
import net.oktawia.crazyae2addons.menus.PenroseInjectionPortMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class PenroseInjectionPortScreen<C extends PenroseInjectionPortMenu> extends AEBaseScreen<C> {

    private AETextField rateField;
    private IconButton confirmButton;

    private boolean initialized = false;

    public PenroseInjectionPortScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();

        this.widgets.add("rate", rateField);
        this.widgets.add("confirm", confirmButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            int rate = getMenu().desiredRate;
            if (rate > 0) {
                rateField.setValue(Integer.toString(rate));
            } else {
                rateField.setValue("");
            }
            initialized = true;
        }
    }

    private void setupGui() {
        rateField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        rateField.setMaxLength(8);
        rateField.setBordered(false);

        rateField.setPlaceholder(Component.translatable(
                "gui.crazyae2addons.penrose_injection_port_rate_hint",
                PenroseInjectionPortBE.MAX_RATE
        ));

        rateField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));

        rateField.setTooltip(Tooltip.create(Component.translatable(
                "gui.crazyae2addons.penrose_injection_port_rate_tooltip"
        )));

        confirmButton = new IconButton(Icon.ENTER, btn -> {
            String val = rateField.getValue();

            if (val.isEmpty() || !val.chars().allMatch(Character::isDigit)) {
                flashFieldError();
                return;
            }

            int parsed;
            try {
                parsed = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                flashFieldError();
                return;
            }

            if (parsed < 0) parsed = 0;
            if (parsed > PenroseInjectionPortBE.MAX_RATE) parsed = PenroseInjectionPortBE.MAX_RATE;

            flashFieldOk();
            getMenu().setDesiredRate(parsed);
        });

        confirmButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.crazyae2addons.penrose_injection_port_submit"
        )));
    }

    private void flashFieldOk() {
        rateField.setTextColor(0x00FF00);
        Utils.asyncDelay(() -> rateField.setTextColor(0xFFFFFF), 1);
    }

    private void flashFieldError() {
        rateField.setTextColor(0xFF0000);
        Utils.asyncDelay(() -> rateField.setTextColor(0xFFFFFF), 1);
    }
}
