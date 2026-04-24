package net.oktawia.crazyae2addons.defs;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.glodblock.github.appflux.common.AFItemAndBlock;
import de.mari_023.ae2wtlib.AE2wtlib;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;

public class UpgradeCards {
    public UpgradeCards(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get(), 1);
            Upgrades.add(AE2wtlib.QUANTUM_BRIDGE_CARD, CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get(), 1);
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get(), 1);
            Upgrades.add(AE2wtlib.QUANTUM_BRIDGE_CARD, CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get(), 1);
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(), 1);
            Upgrades.add(AE2wtlib.QUANTUM_BRIDGE_CARD, CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(), 1);
            Upgrades.add(AEItems.CRAFTING_CARD, CrazyItemRegistrar.MULTI_LEVEL_EMITTER.get(), 1);
            Upgrades.add(AEItems.FUZZY_CARD, CrazyItemRegistrar.MULTI_LEVEL_EMITTER.get(), 1);
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.PORTABLE_SPATIAL_STORAGE.get(), 4);
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.PORTABLE_SPATIAL_CLONER.get(), 4);
            Upgrades.add(AEItems.CRAFTING_CARD, CrazyItemRegistrar.PORTABLE_SPATIAL_CLONER.get(), 1);
            if (IsModLoaded.APP_FLUX){
                Upgrades.add(AFItemAndBlock.INDUCTION_CARD, CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(), 1);
                Upgrades.add(AFItemAndBlock.INDUCTION_CARD, CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get(), 1);
            }
        });
    }
}