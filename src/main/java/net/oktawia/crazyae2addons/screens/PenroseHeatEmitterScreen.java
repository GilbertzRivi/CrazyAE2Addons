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
import net.oktawia.crazyae2addons.menus.PenroseHeatEmitterMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class PenroseHeatEmitterScreen<C extends PenroseHeatEmitterMenu> extends AEBaseScreen<C> {

    private AETextField heatOnField;
    private AETextField heatOffField;

    private IconButton confirmButton;

    private boolean initialized = false;

    public PenroseHeatEmitterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();

        this.widgets.add("heat_on", heatOnField);
        this.widgets.add("heat_off", heatOffField);
        this.widgets.add("confirm", confirmButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            heatOnField.setValue(Long.toString((long) getMenu().heat_on_gk));
            heatOffField.setValue(Long.toString((long) getMenu().heat_off_gk));
            initialized = true;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (heatOnField.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("gui.crazyae2addons.penrose_heat_emitter_on_tooltip"),
                    mouseX, mouseY
            );
        } else if (heatOffField.isMouseOver(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    Minecraft.getInstance().font,
                    Component.translatable("gui.crazyae2addons.penrose_heat_emitter_off_tooltip"),
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

    private void apply() {
        String onVal = heatOnField.getValue();
        String offVal = heatOffField.getValue();

        boolean onOk = !onVal.isEmpty() && onVal.chars().allMatch(Character::isDigit);
        boolean offOk = !offVal.isEmpty() && offVal.chars().allMatch(Character::isDigit);

        if (!onOk) flashErr(heatOnField); else flashOk(heatOnField);
        if (!offOk) flashErr(heatOffField); else flashOk(heatOffField);

        if (!onOk || !offOk) return;

        long onParsed = Long.parseLong(onVal);
        long offParsed = Long.parseLong(offVal);

        getMenu().setHeatOn((double) onParsed);
        getMenu().setHeatOff((double) offParsed);
    }

    private void setupGui() {
        heatOnField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        heatOnField.setMaxLength(32);
        heatOnField.setBordered(false);
        heatOnField.setPlaceholder(Component.translatable("gui.crazyae2addons.penrose_heat_emitter_on_placeholder"));
        heatOnField.setFilter(x -> x.chars().allMatch(Character::isDigit));

        heatOffField = new AETextField(getStyle(), Minecraft.getInstance().font, 0, 0, 0, 0);
        heatOffField.setMaxLength(32);
        heatOffField.setBordered(false);
        heatOffField.setPlaceholder(Component.translatable("gui.crazyae2addons.penrose_heat_emitter_off_placeholder"));
        heatOffField.setFilter(x -> x.chars().allMatch(Character::isDigit));

        confirmButton = new IconButton(Icon.ENTER, btn -> apply());
        confirmButton.setTooltip(Tooltip.create(
                Component.translatable("gui.crazyae2addons.penrose_heat_emitter_submit")));
    }
}
