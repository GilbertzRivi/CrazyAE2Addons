package net.oktawia.crazyae2addons.client.screens.item;

import appeng.api.upgrades.Upgrades;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.BackgroundPanel;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import de.mari_023.ae2wtlib.wut.CycleTerminalButton;
import de.mari_023.ae2wtlib.wut.IUniversalTerminalCapable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.screens.part.RedstoneTerminalScreen;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.menus.item.WirelessRedstoneTerminalMenu;

import java.util.ArrayList;
import java.util.List;

public class WirelessRedstoneTerminalScreen extends RedstoneTerminalScreen<WirelessRedstoneTerminalMenu>
        implements IUniversalTerminalCapable {

    public WirelessRedstoneTerminalScreen(
            WirelessRedstoneTerminalMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style
    ) {
        super(menu, playerInventory, title, style);

        if (menu.isWUT()) {
            this.addToLeftToolbar(new CycleTerminalButton(button -> this.cycleTerminal()));
        }

        var singularityBackground = style.getImage("singularityBackground");
        if (singularityBackground != null) {
            this.widgets.add("singularityBackground", new BackgroundPanel(singularityBackground));
        }

        this.widgets.add("upgrades", new UpgradesPanel(
                menu.getSlots(SlotSemantics.UPGRADE),
                this::getCompatibleUpgrades
        ));
    }

    private List<Component> getCompatibleUpgrades() {
        List<Component> lines = new ArrayList<>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.addAll(Upgrades.getTooltipLinesForMachine(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get()));
        return lines;
    }

    @Override
    public void storeState() {
    }
}