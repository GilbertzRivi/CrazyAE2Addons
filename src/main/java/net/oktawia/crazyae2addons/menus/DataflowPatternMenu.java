package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.DataflowPatternHost;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.TagPacket;
import net.oktawia.crazyae2addons.network.TagPacketToServer;

public class DataflowPatternMenu extends AEBaseMenu {
    private final DataflowPatternHost host;
    public String TAG = "actionTag";
    public DataflowPatternMenu(int id, Inventory playerInventory, DataflowPatternHost host) {
        super(CrazyMenuRegistrar.DATAFLOW_PATTERN_MENU.get(), id, playerInventory, host);
        this.createPlayerInventorySlots(playerInventory);
        this.host = host;
        registerClientAction(TAG, this::sendData);
    }

    public void saveData(CompoundTag collect) {
        if (isClientSide()){
            NetworkHandler.INSTANCE.sendToServer(new TagPacketToServer(collect));
        }
        host.getItemStack().getOrCreateTag().put("flow", collect);
    }

    public void sendData(){
        if (isClientSide()){
            sendClientAction(TAG);
        } else {
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new TagPacket(host.getItemStack().getOrCreateTag().getCompound("flow")));
        }
    }
}
