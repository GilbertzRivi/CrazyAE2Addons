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
import net.oktawia.crazyae2addons.client.screens.item.PortableSpatialClonerScreen;
import net.oktawia.crazyae2addons.client.screens.item.PortableSpatialStorageScreen;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryClientCache;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.structures.RequestStructureToolPreviewPacket;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PortableSpatialStoragePreviewSync {

    private static final StringBuilder BUFFER = new StringBuilder();
    private static final Map<String, PreviewStructure> STRUCTURE_CACHE = new HashMap<>();
    private static final Map<String, CompoundTag> RAW_TAG_CACHE = new HashMap<>();

    private static boolean receiving = false;
    private static String receivingStructureId = "";
    private static String lastRequestedStructureId = "";
    private static int requestCooldownTicks = 0;

    private PortableSpatialStoragePreviewSync() {
    }

    static void cachePut(String structureId, PreviewStructure structure) {
        if (structureId == null || structureId.isBlank() || structure == null) {
            return;
        }

        PreviewStructure old = STRUCTURE_CACHE.put(structureId, structure);

        if (old != null && old != structure) {
            old.close();
        }
    }

    public static PreviewStructure cacheGet(String structureId) {
        if (structureId == null || structureId.isBlank()) {
            return null;
        }

        return STRUCTURE_CACHE.get(structureId);
    }

    static void cacheClear(String structureId) {
        if (structureId == null || structureId.isBlank()) {
            return;
        }

        PreviewStructure old = STRUCTURE_CACHE.remove(structureId);

        if (old != null) {
            old.close();
        }

        RAW_TAG_CACHE.remove(structureId);
    }

    static void cacheClearAll() {
        STRUCTURE_CACHE.values().forEach(PreviewStructure::close);
        STRUCTURE_CACHE.clear();
        RAW_TAG_CACHE.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            clientTick();
        }
    }

    public static @Nullable CompoundTag cacheGetRawTag(String structureId) {
        CompoundTag tag = RAW_TAG_CACHE.get(structureId);
        return tag == null ? null : tag.copy();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetClientState();
    }

    public static void acceptChunk(String data) {
        if ("__RESET__".equals(data)) {
            BUFFER.setLength(0);
            receiving = true;
            receivingStructureId = getActivePreviewStructureId();
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

        String structureId = getActivePreviewStructureId();

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
        NetworkHandler.sendToServer(new RequestStructureToolPreviewPacket());
    }

    public static void resetClientState() {
        BUFFER.setLength(0);
        receiving = false;
        receivingStructureId = "";
        lastRequestedStructureId = "";
        requestCooldownTicks = 0;
        cacheClearAll();
    }

    private static void finish() {
        String structureId = receivingStructureId;

        if (structureId.isBlank()) {
            structureId = getActivePreviewStructureId();
        }

        if (structureId.isBlank()) {
            BUFFER.setLength(0);
            receivingStructureId = "";
            return;
        }

        if (BUFFER.length() == 0) {
            cacheClear(structureId);
            markOpenPreviewDirty();
            BUFFER.setLength(0);
            receivingStructureId = "";
            return;
        }

        try {
            byte[] bytes = TemplateUtil.fromBase64(BUFFER.toString());
            CompoundTag tag = TemplateUtil.decompressNbt(bytes);

            syncActiveStackTransformFromTag(tag);

            RAW_TAG_CACHE.put(structureId, tag.copy());

            PreviewStructure structure = PreviewStructure.fromTemplateTag(tag);
            cachePut(structureId, structure);

            markOpenPreviewDirty();
        } catch (Exception t) {
            CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
        } finally {
            BUFFER.setLength(0);
            receivingStructureId = "";
        }
    }

    private static String getActivePreviewStructureId() {
        Minecraft minecraft = Minecraft.getInstance();

        ItemStack stack = StructureToolUtil.findActive(
                minecraft.player,
                PortableSpatialStorage.class,
                PortableSpatialCloner.class
        );

        if (stack.isEmpty()) {
            stack = StructureToolUtil.findHeld(
                    minecraft.player,
                    PortableSpatialStorage.class,
                    PortableSpatialCloner.class
            );
        }

        if (stack.isEmpty()) {
            return "";
        }

        if (stack.getItem() instanceof PortableSpatialCloner) {
            String selectedId = ClonerStructureLibraryClientCache.selectedId();

            if (selectedId != null && !selectedId.isBlank()) {
                return selectedId;
            }
        }

        return StructureToolStackState.getStructureId(stack);
    }

    private static void syncActiveStackTransformFromTag(CompoundTag tag) {
        if (tag == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        ItemStack stack = StructureToolUtil.findActive(
                minecraft.player,
                PortableSpatialStorage.class,
                PortableSpatialCloner.class
        );

        if (stack.isEmpty()) {
            stack = StructureToolUtil.findHeld(
                    minecraft.player,
                    PortableSpatialStorage.class,
                    PortableSpatialCloner.class
            );
        }

        if (stack.isEmpty()) {
            return;
        }

        TemplateUtil.copyPreviewTransformState(tag, stack.getOrCreateTag());
    }

    private static void markOpenPreviewDirty() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof PortableSpatialStorageScreen<?> screen) {
            screen.markPreviewDirty();
        }

        if (minecraft.screen instanceof PortableSpatialClonerScreen<?> screen) {
            screen.markPreviewDirty();
        }
    }
}