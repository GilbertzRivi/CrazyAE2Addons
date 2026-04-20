package net.oktawia.crazyae2addons.defs;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.glodblock.github.appflux.common.AFItemAndBlock;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;

public class UpgradeCards {
    public UpgradeCards(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(AEItems.SPEED_CARD, CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 6);
            Upgrades.add(AEItems.CRAFTING_CARD, CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 1);

            if (IsModLoaded.APP_FLUX){
                Upgrades.add(AFItemAndBlock.INDUCTION_CARD, CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(), 1);
                Upgrades.add(AFItemAndBlock.INDUCTION_CARD, CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get(), 1);
                Upgrades.add(AFItemAndBlock.INDUCTION_CARD, CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK.get(), 1);
            }
        });
    }
}