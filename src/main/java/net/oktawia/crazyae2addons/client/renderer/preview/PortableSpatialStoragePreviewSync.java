package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStackState;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.RequestPortableSpatialStoragePreviewPacket;
import net.oktawia.crazyae2addons.util.TemplateUtil;

public final class PortableSpatialStoragePreviewSync {

    private static final StringBuilder BUFFER = new StringBuilder();

    private static boolean receiving = false;
    private static String lastRequestedStructureId = "";
    private static int requestCooldownTicks = 0;

    private PortableSpatialStoragePreviewSync() {
    }

    public static void acceptChunk(String data) {
        if ("__RESET__".equals(data)) {
            BUFFER.setLength(0);
            receiving = true;
            return;
        }

        if ("__END__".equals(data)) {
            receiving = false;
            finish();
            return;
        }

        if (receiving && data != null) {
            BUFFER.append(data);
        }
    }

    public static void clientTick() {
        if (requestCooldownTicks > 0) {
            requestCooldownTicks--;
        }

        ItemStack stack = findActiveStack();
        if (stack.isEmpty()) {
            lastRequestedStructureId = "";
            return;
        }

        String structureId = CutPasteStackState.getStructureId(stack);
        if (structureId.isBlank()) {
            lastRequestedStructureId = "";
            return;
        }

        if (PortableSpatialStoragePreviewCache.get(structureId) != null) {
            return;
        }

        if (structureId.equals(lastRequestedStructureId) && requestCooldownTicks > 0) {
            return;
        }

        lastRequestedStructureId = structureId;
        requestCooldownTicks = 20;
        NetworkHandler.sendToServer(new RequestPortableSpatialStoragePreviewPacket());
    }

    public static void resetClientState() {
        BUFFER.setLength(0);
        receiving = false;
        lastRequestedStructureId = "";
        requestCooldownTicks = 0;
        PortableSpatialStoragePreviewCache.clearAll();
    }

    private static void finish() {
        ItemStack stack = findActiveStack();
        if (stack.isEmpty()) {
            BUFFER.setLength(0);
            return;
        }

        String structureId = CutPasteStackState.getStructureId(stack);
        if (structureId.isBlank()) {
            BUFFER.setLength(0);
            return;
        }

        if (BUFFER.length() == 0) {
            PortableSpatialStoragePreviewCache.clear(structureId);
            return;
        }

        try {
            byte[] bytes = TemplateUtil.fromBase64(BUFFER.toString());
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);
            PreviewStructure structure = PreviewStructure.fromTemplateTag(tag);
            PortableSpatialStoragePreviewCache.put(structureId, structure);
        } catch (Exception ignored) {
        } finally {
            BUFFER.setLength(0);
        }
    }

    public static ItemStack findActiveStack() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof PortableSpatialStorage && CutPasteStackState.hasStructure(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof PortableSpatialStorage && CutPasteStackState.hasStructure(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }
}