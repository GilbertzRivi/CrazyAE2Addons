package net.oktawia.crazyae2addons.ldlib;

import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import net.oktawia.crazyae2addons.ldlib.accessors.ConfigInventoryAccessor;
import net.oktawia.crazyae2addons.ldlib.accessors.CraftingLinkAccessor;
import net.oktawia.crazyae2addons.ldlib.accessors.InventoryAccessor;
import net.oktawia.crazyae2addons.ldlib.accessors.ManagedBufferAccessor;
import net.oktawia.crazyae2addons.ldlib.accessors.UpgradeInventoryAccessor;

public final class CrazyLDLibPlugin {
    private static boolean initialized = false;

    private CrazyLDLibPlugin() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new InventoryAccessor(), 1500);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new UpgradeInventoryAccessor(), 1500);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new ManagedBufferAccessor(), 1500);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new ConfigInventoryAccessor(), 1500);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new CraftingLinkAccessor(), 1000);
    }
}