package net.oktawia.crazyae2addons.defs;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import de.mari_023.ae2wtlib.AE2wtlibItems;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import com.glodblock.github.appflux.common.AFSingletons;

public class UpgradeCards {
    public UpgradeCards(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(AEItems.SPEED_CARD, CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 6);
            Upgrades.add(AEItems.CRAFTING_CARD, CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 1);
            Upgrades.add(AE2wtlibItems.QUANTUM_BRIDGE_CARD, CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get(), 1);
            if (IsModLoaded.isAppFluxLoaded()){
                Upgrades.add(AFSingletons.INDUCTION_CARD, CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(), 1);
                Upgrades.add(AFSingletons.INDUCTION_CARD, CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get(), 1);
                Upgrades.add(AFSingletons.INDUCTION_CARD, CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK.get(), 1);
            }
        });
    }
}