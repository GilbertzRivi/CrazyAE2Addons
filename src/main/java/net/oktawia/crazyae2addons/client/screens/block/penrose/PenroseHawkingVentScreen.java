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
import net.oktawia.crazyae2addons.entities.penrose.PenroseHawkingVentBE;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseHawkingVentMenu;

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
            int v = (int) Math.round(getMenu().getHost().getDesiredEvap());
            evapField.setValue(v > 0 ? Integer.toString(v) : "");
            initialized = true;
        }

        String evapStr = evapField.getValue();
        double previewEvap = evapStr.isEmpty()
                ? 0.0
                : evapStr.chars().allMatch(Character::isDigit)
                  ? (double) Long.parseLong(evapStr)
                  : getMenu().getHost().getDesiredEvap();

        setTextContent(
                "cost_value",
                Component.translatable(
                        LangDefs.COST_PER_TICK.getTranslationKey(),
                        Utils.shortenNumber(PenroseHawkingVentBE.computeCostForEvap(previewEvap))
                )
        );
    }

    private void setupGui() {
        evapField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        evapField.setMaxLength(10);
        evapField.setBordered(false);
        evapField.setPlaceholder(Component.translatable(LangDefs.PENROSE_HAWKING_VENT_EVAP_HINT.getTranslationKey()));
        evapField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        evapField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.PENROSE_HAWKING_VENT_EVAP_TOOLTIP.getTranslationKey())
        ));

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

            if (parsed < 0) {
                parsed = 0;
            }

            flashOk(evapField);
            getMenu().setDesiredEvap(parsed);
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