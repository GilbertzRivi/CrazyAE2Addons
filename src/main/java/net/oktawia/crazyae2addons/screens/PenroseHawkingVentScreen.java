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
import net.oktawia.crazyae2addons.menus.PenroseHawkingVentMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class PenroseHawkingVentScreen<C extends PenroseHawkingVentMenu> extends AEBaseScreen<C> {

    private AETextField evapField;
    private IconButton confirmButton;
    private boolean initialized = false;

    public PenroseHawkingVentScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();

        this.widgets.add("evap", evapField);
        this.widgets.add("confirm", confirmButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            int v = getMenu().desiredEvap;
            evapField.setValue(v > 0 ? Integer.toString(v) : "");
            initialized = true;
        }

        setTextContent("cost_value",
                Component.translatable("gui.crazyae2addons.penrose_hawking_vent_cost_value",
                        Utils.shortenNumber(getMenu().costPerTick)));

        // opcjonalnie: status (jeśli dodasz do menu np. boolean ventingLocked / hasRedstone)
        // setTextContent("status_value", ...);
    }

    private void setupGui() {
        evapField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        evapField.setMaxLength(10);
        evapField.setBordered(false);

        evapField.setPlaceholder(Component.translatable(
                "gui.crazyae2addons.penrose_hawking_vent_rate_hint"
        ));

        evapField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));

        evapField.setTooltip(Tooltip.create(Component.translatable(
                "gui.crazyae2addons.penrose_hawking_vent_rate_tooltip"
        )));

        confirmButton = new IconButton(Icon.ENTER, btn -> {
            String val = evapField.getValue();

            if (val.isEmpty() || !val.chars().allMatch(Character::isDigit)) {
                flashError(evapField);
                return;
            }

            int parsed;
            try {
                parsed = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                flashError(evapField);
                return;
            }

            if (parsed < 0) parsed = 0;

            flashOk(evapField);
            getMenu().setDesiredEvap(parsed);
        });

        confirmButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.crazyae2addons.penrose_hawking_vent_submit_tooltip"
        )));
    }

    private void flashOk(AETextField f) {
        f.setTextColor(0x00FF00);
        Utils.asyncDelay(() -> f.setTextColor(0xFFFFFF), 1);
    }

    private void flashError(AETextField f) {
        f.setTextColor(0xFF0000);
        Utils.asyncDelay(() -> f.setTextColor(0xFFFFFF), 1);
    }
}
