package net.oktawia.crazyae2addons.ldlib;

import com.lowdragmc.lowdraglib.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import net.oktawia.crazyae2addons.ldlib.accessors.*;

@LDLibPlugin
public final class CrazyLDLibPlugin implements ILDLibPlugin {

    @Override
    public void onLoad() {
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new InventoryAccessor(), 1500);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new UpgradeInventoryAccessor(), 1500);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new ManagedBufferAccessor(), 1500);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new ConfigInventoryAccessor(), 1500);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new CraftingLinkAccessor(), 1000);
        TypedPayloadRegistries.register(NbtTagPayload.class, NbtTagPayload::new, new FluidTankAccessor(), 1000);
    }
}