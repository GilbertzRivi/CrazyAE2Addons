package net.oktawia.crazyae2addons.ldlib;

import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import net.oktawia.crazyae2addons.ldlib.accessors.*;

@LDLibPlugin
public final class CrazyDLibPlugin implements ILDLibPlugin {
    @Override
    public void onLoad() {
        AccessorRegistries.registerAccessor(new AE2InventoryAccessor(), 1500);
        AccessorRegistries.registerAccessor(new AE2UpgradeInventoryAccessor(), 1500);
        AccessorRegistries.registerAccessor(new ManagedBufferAccessor(), 1500);
    }
}