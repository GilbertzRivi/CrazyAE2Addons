package net.oktawia.crazyae2addons.client.screens.part;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.part.ChunkyFluidP2PTunnelMenu;

public class ChunkyFluidP2PTunnelScreen<C extends ChunkyFluidP2PTunnelMenu> extends AEBaseScreen<C> {

    private final AETextField value;
    private boolean initialized = false;

    public ChunkyFluidP2PTunnelScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.value = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.value.setBordered(false);
        this.value.setMaxLength(10);
        this.value.setResponder(this::onValueChanged);

        this.widgets.add("value", value);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!initialized) {
            value.setValue(String.valueOf(getMenu().value));
            initialized = true;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && value.isMouseOver(mouseX, mouseY)) {
            value.setValue("");
            value.setFocused(true);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onValueChanged(String input) {
        if (input == null || input.isBlank()) {
            return;
        }
        if (!input.chars().allMatch(Character::isDigit)) {
            return;
        }

        int parsed = Integer.parseInt(input);
        getMenu().syncValue(parsed);
    }
}