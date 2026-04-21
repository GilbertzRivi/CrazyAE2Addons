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
import net.oktawia.crazyae2addons.client.screens.part.EmitterTerminalScreen;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.menus.item.WirelessEmitterTerminalMenu;

import java.util.ArrayList;
import java.util.List;

public class WirelessEmitterTerminalScreen extends EmitterTerminalScreen<WirelessEmitterTerminalMenu>
        implements IUniversalTerminalCapable {

    public WirelessEmitterTerminalScreen(WirelessEmitterTerminalMenu menu, Inventory playerInventory,
                                         Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        if (menu.isWUT()) {
            this.addToLeftToolbar(new CycleTerminalButton(btn -> this.cycleTerminal()));
        }

        var singularityBg = style.getImage("singularityBackground");
        if (singularityBg != null) {
            this.widgets.add("singularityBackground", new BackgroundPanel(singularityBg));
        }

        this.widgets.add("upgrades", new UpgradesPanel(
                menu.getSlots(SlotSemantics.UPGRADE),
                this::getCompatibleUpgrades));
    }

    private List<Component> getCompatibleUpgrades() {
        var list = new ArrayList<Component>();
        list.add(GuiText.CompatibleUpgrades.text());
        list.addAll(Upgrades.getTooltipLinesForMachine(CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get()));
        return list;
    }

    @Override
    public void storeState() {
    }
}
