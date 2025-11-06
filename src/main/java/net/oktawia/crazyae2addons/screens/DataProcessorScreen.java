package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.DataProcessorMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class DataProcessorScreen<C extends DataProcessorMenu> extends AEBaseScreen<C> {
    private AETextField watchedInput;
    private IconButton confirm;
    private String lastSyncedWatched = "";

    public DataProcessorScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        watchedInput = new AETextField(this.style, this.font, 0, 0, 0, 0);
        watchedInput.setBordered(false);
        watchedInput.setPlaceholder(Component.literal("Watched variable"));
        this.widgets.add("watched_input", watchedInput);

        confirm = new IconButton(Icon.ENTER, (b) -> getMenu().onClientSelectedIndex(watchedInput.getValue()));
        this.widgets.add("confirm", confirm);

        lastSyncedWatched = this.menu.syncedWatchedVar;
        watchedInput.setValue(lastSyncedWatched);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        String synced = this.menu.syncedWatchedVar;
        if (!watchedInput.isFocused() && !synced.equals(lastSyncedWatched)) {
            lastSyncedWatched = synced;
            watchedInput.setValue(synced);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean wasFocused = watchedInput.isFocused();
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (wasFocused && !watchedInput.isFocused()) {
            getMenu().onClientSelectedIndex(watchedInput.getValue().trim());
        }
        return handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (watchedInput.isFocused() && (keyCode == 257 || keyCode == 335)) {
            getMenu().onClientSelectedIndex(watchedInput.getValue().trim());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
