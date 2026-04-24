package net.oktawia.crazyae2addons.menus.item;

import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolHost;

public class PortableSpatialStorageMenu extends AbstractPortableStructureToolMenu {

    public PortableSpatialStorageMenu(int id, Inventory playerInventory, StructureToolHost host) {
        super(CrazyMenuRegistrar.PORTABLE_SPATIAL_STORAGE_MENU.get(), id, playerInventory, host);
    }
}