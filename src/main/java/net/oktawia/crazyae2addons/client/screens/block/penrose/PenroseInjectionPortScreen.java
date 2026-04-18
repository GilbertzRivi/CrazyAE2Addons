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
import net.oktawia.crazyae2addons.entities.penrose.PenroseInjectionPortBE;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseInjectionPortMenu;

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
            int rate = getMenu().getHost().getDesiredRate();
            rateField.setValue(rate > 0 ? Integer.toString(rate) : "");
            initialized = true;
        }
    }

    private void setupGui() {
        rateField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        rateField.setMaxLength(8);
        rateField.setBordered(false);
        rateField.setPlaceholder(Component.translatable(
                LangDefs.PENROSE_INJECTION_PORT_RATE_HINT.getTranslationKey()
        ));
        rateField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        rateField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.PENROSE_INJECTION_PORT_RATE_TOOLTIP.getTranslationKey())
        ));

        confirmButton = new IconButton(Icon.ENTER, btn -> {
            String val = rateField.getValue();
            if (val.isEmpty() || !val.chars().allMatch(Character::isDigit)) {
                flashError();
                return;
            }

            int parsed;
            try {
                parsed = Integer.parseInt(val);
            } catch (NumberFormatException e) {
                flashError();
                return;
            }

            parsed = Math.clamp(parsed, 0, PenroseInjectionPortBE.MAX_RATE);

            flashOk();
            getMenu().setDesiredRate(parsed);
        });
        confirmButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.SUBMIT.getTranslationKey())
        ));
    }

    private void flashOk() {
        rateField.setTextColor(0x00FF00);
        Utils.asyncDelay(() -> rateField.setTextColor(0xFFFFFF), 1);
    }

    private void flashError() {
        rateField.setTextColor(0xFF0000);
        Utils.asyncDelay(() -> rateField.setTextColor(0xFFFFFF), 1);
    }
}