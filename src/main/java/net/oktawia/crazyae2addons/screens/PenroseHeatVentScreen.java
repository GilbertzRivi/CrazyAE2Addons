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
import net.oktawia.crazyae2addons.menus.PenroseHeatVentMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class PenroseHeatVentScreen<C extends PenroseHeatVentMenu> extends AEBaseScreen<C> {

    private AETextField coolingField;
    private IconButton confirmButton;
    private boolean initialized = false;

    public PenroseHeatVentScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();

        this.widgets.add("cooling", coolingField);
        this.widgets.add("confirm", confirmButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            int cooling = getMenu().desiredCooling;
            coolingField.setValue(cooling > 0 ? Integer.toString(cooling) : "");
            initialized = true;
        }

        setTextContent("cost_value",
                Component.translatable("gui.crazyae2addons.penrose_heat_vent_cost_value",
                        Utils.shortenNumber(getMenu().costPerTick)));
    }

    private void setupGui() {
        coolingField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        coolingField.setMaxLength(10);
        coolingField.setBordered(false);

        coolingField.setPlaceholder(Component.translatable(
                "gui.crazyae2addons.penrose_heat_vent_cooling_hint"
        ));

        coolingField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));

        coolingField.setTooltip(Tooltip.create(Component.translatable(
                "gui.crazyae2addons.penrose_heat_vent_cooling_tooltip"
        )));

        confirmButton = new IconButton(Icon.ENTER, btn -> {
            String val = coolingField.getValue();

            if (val.isEmpty() || !val.chars().allMatch(Character::isDigit)) {
                flashError(coolingField);
                return;
            }

            int parsed;
            try {
                parsed = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                flashError(coolingField);
                return;
            }

            if (parsed < 0) parsed = 0;

            flashOk(coolingField);
            getMenu().setDesiredCooling(parsed);
        });

        confirmButton.setTooltip(Tooltip.create(Component.translatable(
                "gui.crazyae2addons.penrose_heat_vent_submit_tooltip"
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
