package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.SpawnerExtractorControllerMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class SpawnerExtractorControllerScreen<C extends SpawnerExtractorControllerMenu> extends UpgradeableScreen<C> {
    public SpawnerExtractorControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        var prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        prevBtn.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.spawner_preview_tooltip")));
        this.widgets.add("prevbtn", prevBtn);
    }

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent("prev", Component.translatable("gui.crazyae2addons.spawner_preview_label")
                .append(String.valueOf(getMenu().preview)));
    }
}