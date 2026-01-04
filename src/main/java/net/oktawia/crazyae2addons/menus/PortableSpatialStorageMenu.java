package net.oktawia.crazyae2addons.menus;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.BuilderPatternItem;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.BuilderPatternHost;
import net.oktawia.crazyae2addons.logic.GadgetHost;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.SendLongStringToClientPacket;
import net.oktawia.crazyae2addons.network.SendLongStringToServerPacket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PortableSpatialStorageMenu extends AEBaseMenu {
    public static final String SEND_DATA = "SendData";
    public static final String REQUEST_DATA = "requestData";
    public static final String FLIP_H = "flipH";
    public static final String FLIP_V = "flipV";
    public static final String ROTATE = "rotateCW";

    public String program = "";

    public GadgetHost host;

    @GuiSync(239)
    public String name = "";

    public PortableSpatialStorageMenu(int id, Inventory playerInventory, GadgetHost host) {
        super(CrazyMenuRegistrar.GADGET_MENU.get(), id, playerInventory, host);
        setupUpgrades(host.getUpgrades());
        registerClientAction(SEND_DATA, String.class, this::updateData);
        registerClientAction(REQUEST_DATA, this::requestData);
        registerClientAction(FLIP_H, this::flipH);
        registerClientAction(FLIP_V, this::flipV);
        registerClientAction(ROTATE, Integer.class, this::rotateCW);

        this.host = host;
        this.name = host.getItemStack().getDisplayName().getString()
                .substring(1, host.getItemStack().getDisplayName().getString().length() - 1);

        this.createPlayerInventorySlots(playerInventory);

        if (!isClientSide()) {
            this.program = host.getProgram();
        }
        requestData();
    }

    public void requestData() {
        if (isClientSide()) {
            sendClientAction(REQUEST_DATA);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ServerPlayer sp = (ServerPlayer) getPlayer();

            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sp),
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
                        PacketDistributor.PLAYER.with(() -> sp),
                        new SendLongStringToClientPacket(partString)
                );
            }
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SendLongStringToClientPacket("__END__")
            );
        }
    }

    public void flipH() {
        if (isClientSide()) {
            sendClientAction(FLIP_H);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ItemStack s = host.getItemStack();

            BuilderPatternItem.applyFlipHorizontalToItem(s, getPlayer().getServer(), getPlayer());

            String full = BuilderPatternHost.loadProgramFromFile(s, getPlayer().getServer());

            this.host.setProgram(full);
            this.program = full;

            PortableSpatialStorage.rebuildPreviewFromCode(s, getPlayer().getServer(), full);

            requestData();
        }
    }

    public void flipV() {
        if (isClientSide()) {
            sendClientAction(FLIP_V);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ItemStack s = host.getItemStack();

            BuilderPatternItem.applyFlipVerticalToItem(s, getPlayer().getServer(), getPlayer());

            String full = BuilderPatternHost.loadProgramFromFile(s, getPlayer().getServer());

            this.host.setProgram(full);
            this.program = full;

            PortableSpatialStorage.rebuildPreviewFromCode(s, getPlayer().getServer(), full);

            requestData();
        }
    }


    public void rotateCW(Integer times) {
        int t = times == null ? 1 : times;
        if (isClientSide()) {
            sendClientAction(ROTATE, t);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            ItemStack s = host.getItemStack();

            BuilderPatternItem.applyRotateCWToItem(s, getPlayer().getServer(), t, getPlayer());

            String full = BuilderPatternHost.loadProgramFromFile(s, getPlayer().getServer());

            this.host.setProgram(full);
            this.program = full;

            PortableSpatialStorage.rebuildPreviewFromCode(s, getPlayer().getServer(), full);

            requestData();
        }
    }


    public void updateData(String program) {
        this.program = program;
        if (isClientSide()) {
            NetworkHandler.INSTANCE.sendToServer(new SendLongStringToServerPacket(this.program));
        } else {
            this.host.setProgram(program);
            this.program = host.getProgram();
            requestData();
        }
    }
}
