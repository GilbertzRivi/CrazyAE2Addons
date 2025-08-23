package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.items.BuilderPatternItem;
import net.oktawia.crazyae2addons.logic.BuilderPatternHost;
import net.oktawia.crazyae2addons.network.DataValuesPacket;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.SendLongStringToClientPacket;
import net.oktawia.crazyae2addons.network.SendLongStringToServerPacket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BuilderPatternMenu extends AEBaseMenu {
    public static final String SEND_DATA = "SendData";
    public static final String SEND_DELAY = "SendDelay";
    public static final String REQUEST_DATA = "requestData";
    public static final String RENAME = "renameAction";
    public static final String FLIP_H = "flipH";
    public static final String FLIP_V = "flipV";
    public static final String ROTATE = "rotateCW";

    public String program;
    public BuilderPatternHost host;
    @GuiSync(93)
    public Integer delay;
    @GuiSync(239)
    public String name;

    public BuilderPatternMenu(int id, Inventory playerInventory, BuilderPatternHost host) {
        super(CrazyMenuRegistrar.BUILDER_PATTERN_MENU.get(), id, playerInventory, host);
        registerClientAction(SEND_DATA, String.class, this::updateData);
        registerClientAction(SEND_DELAY, Integer.class, this::updateDelay);
        registerClientAction(REQUEST_DATA, this::requestData);
        registerClientAction(RENAME, String.class, this::rename);
        registerClientAction(FLIP_H, this::flipH);
        registerClientAction(FLIP_V, this::flipV);
        registerClientAction(ROTATE, Integer.class, this::rotateCW);
        this.host = host;
        this.name = host.getItemStack().getDisplayName().getString().substring(1, host.getItemStack().getDisplayName().getString().length()-1);
        this.delay = host.getDelay();
        this.createPlayerInventorySlots(playerInventory);
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


    public void flipH() {
        if (isClientSide()) {
            sendClientAction(FLIP_H);
        } else {
            ItemStack s = host.getItemStack();
            BuilderPatternItem.applyFlipHorizontalToItem(s, getPlayer().getServer(), getPlayer());
            this.program = host.getProgram();
            requestData();
        }
    }

    public void flipV() {
        if (isClientSide()) {
            sendClientAction(FLIP_V);
        } else {
            ItemStack s = host.getItemStack();
            BuilderPatternItem.applyFlipVerticalToItem(s, getPlayer().getServer(), getPlayer());
            this.program = host.getProgram();
            requestData();
        }
    }

    public void rotateCW(Integer times) {
        int t = times == null ? 1 : times;
        if (isClientSide()) {
            sendClientAction(ROTATE, t);
        } else {
            ItemStack s = host.getItemStack();
            BuilderPatternItem.applyRotateCWToItem(s, getPlayer().getServer(), t, getPlayer());
            this.program = host.getProgram();
            requestData();
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

    public void updateDelay(Integer delay) {
        if (delay < 0) delay = 0;
        this.delay = delay;
        if (isClientSide()){
            sendClientAction(SEND_DELAY, delay);
        } else {
            this.host.setDelay(delay);
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
