package net.oktawia.crazyae2addons.screens;

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
import net.oktawia.crazyae2addons.menus.PenroseMassEmitterMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

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
            massOnField.setValue(Double.toString(getMenu().mass_on_percent));
            massOffField.setValue(Double.toString(getMenu().mass_off_percent));
            initialized = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (massOnField.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("gui.crazyae2addons.penrose_mass_emitter_on_tooltip"),
                    mouseX, mouseY
            );
        } else if (massOffField.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("gui.crazyae2addons.penrose_mass_emitter_off_tooltip"),
                    mouseX, mouseY
            );
        }
    }

    private void flashOk(AETextField field) {
        field.setTextColor(0x00FF00);
        Utils.asyncDelay(() -> field.setTextColor(0xFFFFFF), 1);
    }

    private void flashErr(AETextField field) {
        field.setTextColor(0xFF0000);
        Utils.asyncDelay(() -> field.setTextColor(0xFFFFFF), 1);
    }

    private static boolean isValidPercentTextLive(String s) {
        // filter do wpisywania: pozwól na pusty (żeby można było kasować)
        if (s == null) return false;
        if (s.isEmpty()) return true;

        int dots = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                dots++;
                if (dots > 1) return false;
                continue;
            }
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    private static boolean isValidPercentTextFinal(String s) {
        if (s == null || s.isEmpty()) return false;
        if (!isValidPercentTextLive(s)) return false;
        if (s.equals(".")) return false;
        return true;
    }

    private void apply() {
        String onVal = massOnField.getValue();
        String offVal = massOffField.getValue();

        boolean onOk = isValidPercentTextFinal(onVal);
        boolean offOk = isValidPercentTextFinal(offVal);

        if (!onOk) flashErr(massOnField); else flashOk(massOnField);
        if (!offOk) flashErr(massOffField); else flashOk(massOffField);

        if (!onOk || !offOk) return;

        double onParsed = Double.parseDouble(onVal);
        double offParsed = Double.parseDouble(offVal);

        getMenu().setMassOnPercent(onParsed);
        getMenu().setMassOffPercent(offParsed);
    }

    private void setupGui() {
        massOnField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        massOnField.setMaxLength(32);
        massOnField.setBordered(false);
        massOnField.setPlaceholder(Component.translatable("gui.crazyae2addons.penrose_mass_emitter_on_placeholder"));
        massOnField.setFilter(PenroseMassEmitterScreen::isValidPercentTextLive);

        massOffField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        massOffField.setMaxLength(32);
        massOffField.setBordered(false);
        massOffField.setPlaceholder(Component.translatable("gui.crazyae2addons.penrose_mass_emitter_off_placeholder"));
        massOffField.setFilter(PenroseMassEmitterScreen::isValidPercentTextLive);

        confirmButton = new IconButton(Icon.ENTER, btn -> apply());
        confirmButton.setTooltip(Tooltip.create(
                Component.translatable("gui.crazyae2addons.penrose_mass_emitter_submit")));
    }
}
