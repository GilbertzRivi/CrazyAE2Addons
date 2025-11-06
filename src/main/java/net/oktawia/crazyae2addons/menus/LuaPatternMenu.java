package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.LuaPatternHost;
import net.oktawia.crazyae2addons.network.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class LuaPatternMenu extends AEBaseMenu {
    public static final String SEND_DATA = "SendData";
    public static final String REQUEST_DATA = "requestData";
    public static final String RENAME = "renameAction";

    public String program;
    public LuaPatternHost host;
    @GuiSync(239)
    public String name;

    public LuaPatternMenu(int id, Inventory playerInventory, LuaPatternHost host) {
        super(CrazyMenuRegistrar.LUA_PATTERN_MENU.get(), id, playerInventory, host);
        registerClientAction(SEND_DATA, String.class, this::updateData);
        registerClientAction(REQUEST_DATA, this::requestData);
        registerClientAction(RENAME, String.class, this::rename);
        this.host = host;
        this.createPlayerInventorySlots(playerInventory);

        this.name = host.getItemStack().getDisplayName().getString().substring(1, host.getItemStack().getDisplayName().getString().length()-1);

        if (!isClientSide()){
            this.program = host.getProgram();
        }
    }

    public void requestData(){
        if (isClientSide()){
            sendClientAction(REQUEST_DATA);
        } else {
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.TRACKING_CHUNK.with(() -> getPlayer().level().getChunkAt(getPlayer().blockPosition())),
                    new SendLongStringToClientPacket("__RESET__")
            );

            byte[] bytes = program.getBytes(StandardCharsets.UTF_8);
            int maxSize = 1000 * 1000;
            int total = (int) Math.ceil((double) bytes.length / maxSize);

            for (int i = 0; i < total; i++) {
                int start = i * maxSize;
                int end = Math.min(bytes.length, (i + 1) * maxSize);
                byte[] part = Arrays.copyOfRange(bytes, start, end);
                String partString = new String(part, StandardCharsets.UTF_8);

                NetworkHandler.INSTANCE.send(
                        PacketDistributor.TRACKING_CHUNK.with(() -> getPlayer().level().getChunkAt(getPlayer().blockPosition())),
                        new SendLongStringToClientPacket(partString)
                );
            }
        }
    }

    public void updateData(String program) {
        this.program = program;
        if (isClientSide()){
            NetworkHandler.INSTANCE.sendToServer(new SendLongStringToServerPacket(this.program));
        } else {
            this.host.setProgram(program);
        }
    }

    public void rename(String name) {
        this.name = name;
        if (isClientSide()){
            sendClientAction(RENAME, name);
        } else {
            host.getItemStack().setHoverName(Component.literal(name));
        }
    }
}