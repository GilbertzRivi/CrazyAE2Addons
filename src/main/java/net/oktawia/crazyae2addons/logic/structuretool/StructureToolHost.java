package net.oktawia.crazyae2addons.logic.structuretool;

import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ISubMenuHost;
import appeng.blockentity.networking.WirelessAccessPointBlockEntity;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class StructureToolHost extends ItemMenuHost implements IActionHost {

    private final IGrid targetGrid;
    @Nullable
    private IWirelessAccessPoint myWap;

    public StructureToolHost(Player player, int inventorySlot, ItemStack stack) {
        super(player, inventorySlot, stack);
        if (!(getItemStack().getItem() instanceof WirelessTerminalItem wirelessTerminalItem)) {
            throw new IllegalArgumentException("Can only use this class with subclasses of WirelessTerminalItem");
        }

        this.targetGrid = wirelessTerminalItem.getLinkedGrid(getItemStack(), player.level(), player);
    }

    public boolean hasStoredStructure() {
        return StructureToolStackState.hasStructure(getItemStack());
    }

    public byte[] getStructureBytes() {
        String id = StructureToolStackState.getStructureId(getItemStack());
        if (id.isBlank()) {
            return null;
        }

        MinecraftServer server = getPlayer().getServer();
        if (server == null) {
            return null;
        }

        try {
            var tag = StructureToolStructureStore.load(server, id);
            return tag == null ? null : TemplateUtil.compressNbt(tag);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void setStructureBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        MinecraftServer server = getPlayer().getServer();
        if (server == null) {
            return;
        }

        try {
            String id = StructureToolStackState.getStructureId(getItemStack());
            if (id.isBlank()) {
                id = UUID.randomUUID().toString();
                StructureToolStackState.setStructureId(getItemStack(), id);
            }

            var tag = TemplateUtil.decompressNbt(bytes);
            StructureToolStructureStore.save(server, id, tag);
        } catch (Exception ignored) {
        }
    }


    @Override
    public IGridNode getActionableNode() {
        this.getWap();
        if (this.myWap != null) {
            return this.myWap.getActionableNode();
        }
        return null;
    }

    public void getWap() {
        if (this.targetGrid != null) {
            for (var wap : this.targetGrid.getMachines(WirelessAccessPointBlockEntity.class)) {
                this.myWap = wap;
            }
        }
    }
}