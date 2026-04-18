package net.oktawia.crazyae2addons.client.screens.block.penrose;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseMassEmitterMenu;

public class PenroseMassEmitterScreen<C extends PenroseMassEmitterMenu> extends AEBaseScreen<C> {

    private AETextField massOnField;
    private AETextField massOffField;
    private IconButton confirmButton;
    private boolean initialized = false;

    public PenroseMassEmitterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("mass_on", massOnField);
        this.widgets.add("mass_off", massOffField);
        this.widgets.add("confirm", confirmButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            massOnField.setValue(Double.toString(getMenu().getHost().getMassOnPercent()));
            massOffField.setValue(Double.toString(getMenu().getHost().getMassOffPercent()));
            initialized = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (massOnField.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(LangDefs.PENROSE_MASS_EMITTER_ON_TOOLTIP.getTranslationKey()),
                    mouseX,
                    mouseY
            );
        } else if (massOffField.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    font,
                    Component.translatable(LangDefs.PENROSE_MASS_EMITTER_OFF_TOOLTIP.getTranslationKey()),
                    mouseX,
                    mouseY
            );
        }
    }

    private static boolean isValidPercentLive(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        int dots = 0;
        for (char c : value.toCharArray()) {
            if (c == '.') {
                if (++dots > 1) {
                    return false;
                }
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidPercentFinal(String value) {
        return value != null && !value.isEmpty() && !value.equals(".") && isValidPercentLive(value);
    }

    private void apply() {
        String onVal = massOnField.getValue();
        String offVal = massOffField.getValue();

        boolean onOk = isValidPercentFinal(onVal);
        boolean offOk = isValidPercentFinal(offVal);

        if (!onOk) {
            flashErr(massOnField);
        } else {
            flashOk(massOnField);
        }

        if (!offOk) {
            flashErr(massOffField);
        } else {
            flashOk(massOffField);
        }

        if (!onOk || !offOk) {
            return;
        }

        getMenu().setMassOnPercent(Double.parseDouble(onVal));
        getMenu().setMassOffPercent(Double.parseDouble(offVal));
    }

    private void setupGui() {
        massOnField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        massOnField.setMaxLength(32);
        massOnField.setBordered(false);
        massOnField.setPlaceholder(Component.translatable(
                LangDefs.PENROSE_MASS_EMITTER_ON_PLACEHOLDER.getTranslationKey()
        ));
        massOnField.setFilter(PenroseMassEmitterScreen::isValidPercentLive);

        massOffField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        massOffField.setMaxLength(32);
        massOffField.setBordered(false);
        massOffField.setPlaceholder(Component.translatable(
                LangDefs.PENROSE_MASS_EMITTER_OFF_PLACEHOLDER.getTranslationKey()
        ));
        massOffField.setFilter(PenroseMassEmitterScreen::isValidPercentLive);

        confirmButton = new IconButton(Icon.ENTER, btn -> apply());
        confirmButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.SUBMIT.getTranslationKey())
        ));
    }

    private void flashOk(AETextField field) {
        field.setTextColor(0x00FF00);
        Utils.asyncDelay(() -> field.setTextColor(0xFFFFFF), 1);
    }

    private void flashErr(AETextField field) {
        field.setTextColor(0xFF0000);
        Utils.asyncDelay(() -> field.setTextColor(0xFFFFFF), 1);
    }
}