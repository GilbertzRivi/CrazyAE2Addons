package net.oktawia.crazyae2addons.menus.item;

import appeng.menu.SlotSemantics;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.RestrictedInputSlot;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.wct.WCTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addons.logic.wireless.WirelessEmitterTerminalItemLogicHost;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;

public class WirelessEmitterTerminalMenu extends EmitterTerminalMenu {

    public static final MenuType<WirelessEmitterTerminalMenu> TYPE =
            MenuTypeBuilder.create(WirelessEmitterTerminalMenu::new, WirelessEmitterTerminalItemLogicHost.class)
                    .build("wireless_emitter_terminal");

    public WirelessEmitterTerminalMenu(int id, Inventory ip, WirelessEmitterTerminalItemLogicHost host) {
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
        return ((WirelessEmitterTerminalItemLogicHost) emitterHost).getItemStack().getItem() instanceof ItemWUT;
    }
}
