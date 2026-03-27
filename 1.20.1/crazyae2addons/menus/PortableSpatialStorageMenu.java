package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.GadgetHost;
import net.oktawia.crazyae2addons.misc.TemplateUtil;
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

    /** Base64-encoded compressed-NBT template, or "" if none. */
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
            byte[] bytes = host.getProgramBytes();
            this.program = (bytes != null && bytes.length > 0) ? TemplateUtil.toBase64(bytes) : "";
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
            int maxSize = 1_000_000;
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
            net.minecraft.core.Direction srcFacing = PortableSpatialStorage.readSrcFacingFromNbt(host.getItemStack());
            applyTransformAndResend(tag -> TemplateUtil.applyFlipHToTag(tag, srcFacing));
        }
    }

    public void flipV() {
        if (isClientSide()) {
            sendClientAction(FLIP_V);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            applyTransformAndResend(TemplateUtil::applyFlipVToTag);
        }
    }

    public void rotateCW(Integer times) {
        int t = times == null ? 1 : times;
        if (isClientSide()) {
            sendClientAction(ROTATE, t);
        } else {
            if (!PortableSpatialStorage.hasStoredStructure(host.getItemStack())) return;
            applyTransformAndResend(tag -> TemplateUtil.applyRotateCWToTag(tag, t));
        }
    }

    @FunctionalInterface
    private interface TagTransform {
        CompoundTag apply(CompoundTag tag);
    }

    private void applyTransformAndResend(TagTransform transform) {
        ItemStack s = host.getItemStack();
        byte[] bytes = host.getProgramBytes();
        if (bytes == null || bytes.length == 0) return;
        try {
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);
            tag = transform.apply(tag);
            bytes = TemplateUtil.compressNbt(tag);
            host.setProgramBytes(bytes);
            this.program = TemplateUtil.toBase64(bytes);
            TemplateUtil.rebuildPreviewFromTag(s, tag);
        } catch (Exception ignored) {}
        requestData();
    }

    public void updateData(String data) {
        this.program = data;
        if (isClientSide()) {
            NetworkHandler.INSTANCE.sendToServer(new SendLongStringToServerPacket(this.program));
        } else {
            if (!data.isEmpty()) {
                try {
                    byte[] bytes = TemplateUtil.fromBase64(data);
                    CompoundTag tag = TemplateUtil.decompressNbt(bytes);
                    host.setProgramBytes(bytes);
                    this.program = data;
                    TemplateUtil.rebuildPreviewFromTag(host.getItemStack(), tag);
                } catch (Exception ignored) {
                    this.program = "";
                }
            } else {
                this.program = "";
            }
            requestData();
        }
    }
}
