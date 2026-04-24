package net.oktawia.crazyae2addons.client.screens.item;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.item.TagViewCellMenu;

public class TagViewCellScreen<C extends TagViewCellMenu> extends AEBaseScreen<C> {

    private final MultilineTextFieldWidget input;
    private boolean initialized = false;
    private String lastMenuData = null;

    public TagViewCellScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.input = new MultilineTextFieldWidget(
                this.font,
                0, 0,
                50, 80,
                Component.translatable(LangDefs.TAG_VIEW_CELL_INPUT.getTranslationKey())
        );
        this.input.setOnValueChanged(value -> this.getMenu().updateData(value));
        this.widgets.add("data", this.input);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        String menuData = this.getMenu().data == null ? "" : this.getMenu().data;

        if (!this.initialized) {
            this.input.setValue(menuData);
            this.lastMenuData = menuData;
            this.initialized = true;
            return;
        }

        if (!menuData.equals(this.lastMenuData)) {
            this.input.setValue(menuData);
            this.lastMenuData = menuData;
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (this.input.isMouseOver(x, y)) {
            return this.input.mouseScrolled(x, y, delta);
        }

        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (this.input.keyPressed(key, scancode, modifiers)) {
            return true;
        }

        if (key == 256) {
            this.onClose();
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.input.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.input.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.input.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.input.charTyped(codePoint, modifiers)) {
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }
}