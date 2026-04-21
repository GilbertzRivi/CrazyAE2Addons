package net.oktawia.crazyae2addons.logic.cutpaste;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.util.UUID;

public class PortableSpatialStorageHost extends ItemMenuHost {

    public PortableSpatialStorageHost(Player player, int inventorySlot, ItemStack stack) {
        super(player, inventorySlot, stack);
    }

    public boolean hasStoredStructure() {
        return CutPasteStackState.hasStructure(getItemStack());
    }

    public byte[] getStructureBytes() {
        String id = CutPasteStackState.getStructureId(getItemStack());
        if (id.isBlank()) {
            return null;
        }

        MinecraftServer server = getPlayer().getServer();
        if (server == null) {
            return null;
        }

        try {
            var tag = CutPasteStructureStore.load(server, id);
            return tag == null ? null : TemplateUtil.compressNbt(tag);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void setStructureBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        MinecraftServer server = getPlayer().getServer();
        if (server == null) {
            return;
        }

        try {
            String id = CutPasteStackState.getStructureId(getItemStack());
            if (id.isBlank()) {
                id = UUID.randomUUID().toString();
                CutPasteStackState.setStructureId(getItemStack(), id);
            }

            var tag = TemplateUtil.decompressNbt(bytes);
            CutPasteStructureStore.save(server, id, tag);
        } catch (Exception ignored) {
        }
    }

}