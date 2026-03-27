package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.EnergyExporterMenu;

public class EnergyExporterScreen<C extends EnergyExporterMenu> extends UpgradeableScreen<C> {
    public EnergyExporterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        setTextContent("fe",Component.literal(String.format("Output: %s FE/t", getMenu().transfered)));
        if (getMenu().greg){
            setTextContent("volt",Component.literal(String.format("Voltage: %s", getMenu().voltage)));
            setTextContent("amp",Component.literal(String.format("Amperage: %s", getMenu().maxAmps)));
        } else {
            setTextContent("volt",Component.empty());
            setTextContent("amp",Component.empty());
        }
    }
}
