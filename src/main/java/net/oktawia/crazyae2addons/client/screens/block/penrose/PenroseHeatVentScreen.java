package net.oktawia.crazyae2addons.client.screens.block.penrose;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.entities.penrose.PenroseHeatVentBE;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseHeatVentMenu;

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
            int cooling = (int) Math.round(getMenu().getHost().getDesiredCooling());
            coolingField.setValue(cooling > 0 ? Integer.toString(cooling) : "");
            initialized = true;
        }

        String coolingStr = coolingField.getValue();
        double previewCooling = coolingStr.isEmpty()
                ? 0.0
                : coolingStr.chars().allMatch(Character::isDigit)
                  ? (double) Long.parseLong(coolingStr)
                  : getMenu().getHost().getDesiredCooling();

        setTextContent(
                "cost_value",
                Component.translatable(
                        LangDefs.COST_PER_TICK.getTranslationKey(),
                        Utils.shortenNumber(PenroseHeatVentBE.computeCostForCooling(previewCooling))
                )
        );
    }

    private void setupGui() {
        coolingField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        coolingField.setMaxLength(10);
        coolingField.setBordered(false);
        coolingField.setPlaceholder(Component.translatable(
                LangDefs.PENROSE_HEAT_VENT_COOLING_HINT.getTranslationKey()
        ));
        coolingField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        coolingField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.PENROSE_HEAT_VENT_COOLING_TOOLTIP.getTranslationKey())
        ));

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

            if (parsed < 0) {
                parsed = 0;
            }

            flashOk(coolingField);
            getMenu().setDesiredCooling(parsed);
        });
        confirmButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.SUBMIT.getTranslationKey())
        ));
    }

    private void flashOk(AETextField field) {
        field.setTextColor(0x00FF00);
        Utils.asyncDelay(() -> field.setTextColor(0xFFFFFF), 1);
    }

    private void flashError(AETextField field) {
        field.setTextColor(0xFF0000);
        Utils.asyncDelay(() -> field.setTextColor(0xFFFFFF), 1);
    }
}