package net.oktawia.crazyae2addons.menus;

import appeng.api.stacks.AEItemKey;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.FakeSlot;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.parts.NBTExportBusPart;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;

public class NBTExportBusMenu extends AEBaseMenu {
    public static final String SEND_DATA = "SendData";

    @GuiSync(92)
    public boolean newData;
    @GuiSync(31)
    public String data;

    public NBTExportBusPart host;

    public NBTExportBusMenu(int id, Inventory playerInventory, NBTExportBusPart host) {
        super(CrazyMenuRegistrar.NBT_EXPORT_BUS_MENU.get(), id, playerInventory, host);
        registerClientAction(SEND_DATA, String.class, this::updateData);
        this.createPlayerInventorySlots(playerInventory);
        this.addSlot(new FakeSlot(host.inv.createMenuWrapper(), 0), SlotSemantics.CONFIG);
        this.host = host;
        this.data = host.data;
        this.newData = false;
        this.host.setMenu(this);
    }

    public void updateData(String data){
        this.data = data;
        this.host.data = data;
        this.host.getHost().markForSave();
        if (isClientSide()){
            sendClientAction(SEND_DATA, data);
        }
    }

    public void loadNBT(){
        var key = host.inv.getKey(0);
        if (key instanceof AEItemKey ik){
            if (ik.getTag() != null){
                this.data = ik.getTag().toString();
                this.newData = true;
            }
        }
    }
}