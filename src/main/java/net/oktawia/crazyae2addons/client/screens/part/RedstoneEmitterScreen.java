package net.oktawia.crazyae2addons.client.screens.part;

import appeng.client.gui.Icon;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.part.RedstoneEmitterMenu;
import net.oktawia.crazyae2addons.util.Utils;

public class RedstoneEmitterScreen<C extends RedstoneEmitterMenu> extends AEBaseScreen<C> {

    private static final int SUCCESS_COLOR = 0x00FF00;
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    private final AETextField nameField;

    private boolean initialized = false;

    public RedstoneEmitterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.nameField = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.nameField.setBordered(false);
        this.nameField.setMaxLength(16);
        this.nameField.setPlaceholder(Component.translatable(LangDefs.NAME.getTranslationKey()));

        IconButton confirmButton = new IconButton(Icon.ENTER, button -> save());
        confirmButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable(LangDefs.APPLY.getTranslationKey())
        ));

        this.widgets.add("name", this.nameField);
        this.widgets.add("confirm", confirmButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            this.nameField.setValue(getMenu().name);
            initialized = true;
        }
    }

    private void save() {
        this.nameField.setTextColor(SUCCESS_COLOR);
        Utils.asyncDelay(() -> this.nameField.setTextColor(DEFAULT_COLOR), 1);
        getMenu().changeName(this.nameField.getValue().trim());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.nameField.isMouseOver(mouseX, mouseY)) {
            this.nameField.setValue("");
            this.nameField.setFocused(true);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256) {
            onClose();
            return true;
        }

        if (key == 257 || key == 335) {
            save();
            return true;
        }

        this.nameField.keyPressed(key, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.nameField.charTyped(codePoint, modifiers)) {
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }
}