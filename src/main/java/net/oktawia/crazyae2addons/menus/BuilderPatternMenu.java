package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.BuilderPatternItem;
import net.oktawia.crazyae2addons.logic.BuilderPatternHost;
import net.oktawia.crazyae2addons.network.packets.SendLongStringToClientPacket;
import net.oktawia.crazyae2addons.network.packets.SendLongStringToServerPacket;

public class BuilderPatternMenu extends AEBaseMenu {

    public static final String SEND_DELAY    = "SendDelay";
    public static final String REQUEST_DATA  = "requestData";
    public static final String RENAME        = "renameAction";
    public static final String FLIP_H        = "flipH";
    public static final String FLIP_V        = "flipV";
    public static final String ROTATE        = "rotateCW";

    public String program;
    public BuilderPatternHost host;

    @GuiSync(93)
    public Integer delay;
    @GuiSync(239)
    public String name;

    public BuilderPatternMenu(int id, Inventory playerInventory, BuilderPatternHost host) {
        super(CrazyMenuRegistrar.BUILDER_PATTERN_MENU.get(), id, playerInventory, host);
        registerClientAction(SEND_DELAY, Integer.class, this::updateDelay);
        registerClientAction(REQUEST_DATA, this::requestData);
        registerClientAction(RENAME, String.class, this::rename);
        registerClientAction(FLIP_H, this::flipH);
        registerClientAction(FLIP_V, this::flipV);
        registerClientAction(ROTATE, Integer.class, this::rotateCW);
        this.host = host;
        String displayName = host.getItemStack().getDisplayName().getString();
        // strip surrounding brackets added by some display name implementations
        if (displayName.startsWith("[") && displayName.endsWith("]")) {
            displayName = displayName.substring(1, displayName.length() - 1);
        }
        this.name = displayName;
        this.delay = host.getDelay();
        this.createPlayerInventorySlots(playerInventory);
        if (!isClientSide()) {
            this.program = host.getProgram();
        }
    }

    public void requestData() {
        if (isClientSide()) {
            sendClientAction(REQUEST_DATA);
        } else {
            String toSend = program != null ? program : "";
            PacketDistributor.sendToPlayer((ServerPlayer) getPlayer(),
                    new SendLongStringToClientPacket(toSend));
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
        if (isClientSide()) {
            PacketDistributor.sendToServer(new SendLongStringToServerPacket(program));
        } else {
            this.host.setProgram(program);
        }
    }

    public void receiveProgram(String program) {
        this.program = program;
        this.host.setProgram(program);
    }

    public void updateDelay(Integer delay) {
        if (delay == null || delay < 0) delay = 0;
        this.delay = delay;
        if (isClientSide()) {
            sendClientAction(SEND_DELAY, delay);
        } else {
            this.host.setDelay(delay);
        }
    }

    public void rename(String name) {
        this.name = name;
        if (isClientSide()) {
            sendClientAction(RENAME, name);
        } else {
            host.getItemStack().set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
    }
}
