package net.oktawia.crazyae2addons.client.screens.item;

import appeng.api.upgrades.Upgrades;
import appeng.client.gui.style.ScreenStyle;
import appeng.core.localization.GuiText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.menus.item.PortableSpatialStorageMenu;

import java.util.ArrayList;
import java.util.List;

public class PortableSpatialStorageScreen<C extends PortableSpatialStorageMenu>
        extends AbstractPortableStructureToolScreen<C> {

    public PortableSpatialStorageScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        initCommonWidgets(style, buildCompatibleUpgrades());
        finishInit();
    }

    @Override
    protected PreviewRect getPreviewRect() {
        return new PreviewRect(
                this.leftPos + 8,
                this.topPos + 24,
                Math.max(32, this.imageWidth - 16),
                Math.max(32, this.imageHeight - 24 - 104)
        );
    }

    @Override
    protected ItemStack findRelevantStack() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = StructureToolUtil.findActive(player, PortableSpatialStorage.class, PortableSpatialCloner.class);
        if (stack.isEmpty()) {
            stack = StructureToolUtil.findHeld(player, PortableSpatialStorage.class, PortableSpatialCloner.class);
        }
        return stack;
    }

    private List<Component> buildCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());
        list.addAll(Upgrades.getTooltipLinesForMachine(CrazyItemRegistrar.PORTABLE_SPATIAL_STORAGE.get()));
        return list;
    }
}