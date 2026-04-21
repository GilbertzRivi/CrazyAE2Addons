package net.oktawia.crazyae2addons.menus.item;

import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.RestrictedInputSlot;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.wct.WCTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addons.logic.wireless.WirelessRedstoneTerminalItemLogicHost;
import net.oktawia.crazyae2addons.menus.part.RedstoneTerminalMenu;

public class WirelessRedstoneTerminalMenu extends RedstoneTerminalMenu {

    public static final MenuType<WirelessRedstoneTerminalMenu> TYPE =
            MenuTypeBuilder.create(WirelessRedstoneTerminalMenu::new, WirelessRedstoneTerminalItemLogicHost.class)
                    .build("wireless_redstone_terminal");

    public WirelessRedstoneTerminalMenu(int id, Inventory ip, WirelessRedstoneTerminalItemLogicHost host) {
        super(TYPE, id, ip, host);

        this.addSlot(
                new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                        host.getSubInventory(WCTMenuHost.INV_SINGULARITY),
                        0
                ),
                AE2wtlibSlotSemantics.SINGULARITY
        );
    }

    public boolean isWUT() {
        return ((WirelessRedstoneTerminalItemLogicHost) redstoneHost).getItemStack().getItem() instanceof ItemWUT;
    }
}