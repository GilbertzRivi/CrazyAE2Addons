package net.oktawia.crazyae2addons.menus.item;

import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStackState;
import net.oktawia.crazyae2addons.logic.cutpaste.PortableSpatialStorageHost;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.SendLongStringToClientPacket;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PortableSpatialStorageMenu extends UpgradeableMenu<PortableSpatialStorageHost> {

    private static final String ACTION_REQUEST_PREVIEW = "portable_spatial_storage.request_preview";
    private static final String ACTION_FLIP_HORIZONTAL = "portable_spatial_storage.flip_horizontal";
    private static final String ACTION_FLIP_VERTICAL = "portable_spatial_storage.flip_vertical";
    private static final String ACTION_ROTATE_CLOCKWISE = "portable_spatial_storage.rotate_clockwise";

    private static final int CHUNK_SIZE = 1_000_000;

    protected final PortableSpatialStorageHost host;

    public PortableSpatialStorageMenu(int id, Inventory playerInventory, PortableSpatialStorageHost host) {
        super(CrazyMenuRegistrar.PORTABLE_SPATIAL_STORAGE_MENU.get(), id, playerInventory, host);
        this.host = host;

        setupUpgrades(host.getUpgrades());

        registerClientAction(ACTION_REQUEST_PREVIEW, this::requestPreview);
        registerClientAction(ACTION_FLIP_HORIZONTAL, this::flipHorizontal);
        registerClientAction(ACTION_FLIP_VERTICAL, this::flipVertical);
        registerClientAction(ACTION_ROTATE_CLOCKWISE, Integer.class, this::rotateClockwise);

        if (!isClientSide()) {
            requestPreview();
        }
    }

    public void requestPreview() {
        if (isClientSide()) {
            sendClientAction(ACTION_REQUEST_PREVIEW);
            return;
        }

        if (!host.hasStoredStructure()) {
            sendPreviewString("");
            return;
        }

        byte[] bytes = host.getStructureBytes();
        if (bytes == null || bytes.length == 0) {
            sendPreviewString("");
            return;
        }

        sendPreviewString(TemplateUtil.toBase64(bytes));
    }

    public void flipHorizontal() {
        if (isClientSide()) {
            sendClientAction(ACTION_FLIP_HORIZONTAL);
            return;
        }

        if (!host.hasStoredStructure()) {
            return;
        }

        applyTransformAndResend(tag ->
                TemplateUtil.applyFlipHToTag(tag, CutPasteStackState.getSourceFacing(host.getItemStack()))
        );
    }

    public void flipVertical() {
        if (isClientSide()) {
            sendClientAction(ACTION_FLIP_VERTICAL);
            return;
        }

        if (!host.hasStoredStructure()) {
            return;
        }

        applyTransformAndResend(TemplateUtil::applyFlipVToTag);
    }

    public void rotateClockwise(Integer times) {
        int turns = times == null ? 1 : times;

        if (isClientSide()) {
            sendClientAction(ACTION_ROTATE_CLOCKWISE, turns);
            return;
        }

        if (!host.hasStoredStructure()) {
            return;
        }

        applyTransformAndResend(tag -> TemplateUtil.applyRotateCWToTag(tag, turns));
    }

    @FunctionalInterface
    private interface TagTransform {
        CompoundTag apply(CompoundTag tag);
    }

    private void applyTransformAndResend(TagTransform transform) {
        byte[] bytes = host.getStructureBytes();
        if (bytes == null || bytes.length == 0) {
            return;
        }

        try {
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);
            CompoundTag transformed = transform.apply(tag);
            host.setStructureBytes(TemplateUtil.compressNbt(transformed));
        } catch (Exception ignored) {
            return;
        }

        requestPreview();
    }

    private void sendPreviewString(String base64) {
        if (!(getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NetworkHandler.sendToPlayer(serverPlayer, new SendLongStringToClientPacket("__RESET__"));

        if (base64 != null && !base64.isEmpty()) {
            byte[] bytes = base64.getBytes(StandardCharsets.UTF_8);
            int total = (int) Math.ceil((double) bytes.length / CHUNK_SIZE);

            for (int i = 0; i < total; i++) {
                int start = i * CHUNK_SIZE;
                int end = Math.min(bytes.length, (i + 1) * CHUNK_SIZE);
                byte[] part = Arrays.copyOfRange(bytes, start, end);

                NetworkHandler.sendToPlayer(serverPlayer, new SendLongStringToClientPacket(new String(part, StandardCharsets.UTF_8)));
            }
        }

        NetworkHandler.sendToPlayer(serverPlayer, new SendLongStringToClientPacket("__END__"));
    }
}