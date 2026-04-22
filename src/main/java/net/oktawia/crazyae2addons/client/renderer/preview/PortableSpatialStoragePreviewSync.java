package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.client.screens.item.PortableSpatialStorageScreen;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStackState;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.RequestPortableSpatialStoragePreviewPacket;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PortableSpatialStoragePreviewSync {

    private static final StringBuilder BUFFER = new StringBuilder();
    private static final Map<String, PreviewStructure> STRUCTURE_CACHE = new HashMap<>();

    private static boolean receiving = false;
    private static String lastRequestedStructureId = "";
    private static int requestCooldownTicks = 0;

    private PortableSpatialStoragePreviewSync() {
    }

    static void cachePut(String structureId, PreviewStructure structure) {
        if (structureId == null || structureId.isBlank() || structure == null) return;
        STRUCTURE_CACHE.put(structureId, structure);
    }

    public static PreviewStructure cacheGet(String structureId) {
        if (structureId == null || structureId.isBlank()) return null;
        return STRUCTURE_CACHE.get(structureId);
    }

    static void cacheClear(String structureId) {
        if (structureId == null || structureId.isBlank()) return;
        PreviewStructure old = STRUCTURE_CACHE.remove(structureId);
        if (old != null) old.close();
    }

    static void cacheClearAll() {
        STRUCTURE_CACHE.values().forEach(PreviewStructure::close);
        STRUCTURE_CACHE.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            clientTick();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetClientState();
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

        ItemStack stack = PortableSpatialStorage.findActive(Minecraft.getInstance().player);
        if (stack.isEmpty()) {
            lastRequestedStructureId = "";
            return;
        }

        String structureId = CutPasteStackState.getStructureId(stack);
        if (structureId.isBlank()) {
            lastRequestedStructureId = "";
            return;
        }

        if (cacheGet(structureId) != null) {
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
        cacheClearAll();
    }

    private static void finish() {
        ItemStack stack = PortableSpatialStorage.findActive(Minecraft.getInstance().player);
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
            cacheClear(structureId);
            return;
        }

        try {
            byte[] bytes = TemplateUtil.fromBase64(BUFFER.toString());
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);
            PreviewStructure structure = PreviewStructure.fromTemplateTag(tag);
            cachePut(structureId, structure);
            if (Minecraft.getInstance().screen instanceof PortableSpatialStorageScreen<?> pss) {
                pss.markPreviewDirty();
            }
        } catch (Exception t) {
            CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
        } finally {
            BUFFER.setLength(0);
        }
    }

}
