package net.oktawia.crazyae2addonslite.defs;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import com.glodblock.github.appflux.common.AFItemAndBlock;
import de.mari_023.ae2wtlib.AE2wtlib;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.oktawia.crazyae2addonslite.IsModLoaded;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;

public class UpgradeCards {
    public UpgradeCards(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(AEItems.VOID_CARD, CrazyItemRegistrar.NBT_STORAGE_BUS_PART_ITEM.get(), 1, "group.nbt_storage_bus.name");
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(), 1, "group.wireless_redstone_terminal.name");
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get(), 1, "group.wireless_redstone_terminal.name");
            Upgrades.add(AE2wtlib.QUANTUM_BRIDGE_CARD, CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(), 1, "group.wireless_redstone_terminal.name");
            Upgrades.add(AE2wtlib.QUANTUM_BRIDGE_CARD, CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get(), 1, "group.wireless_redstone_terminal.name");
            Upgrades.add(CrazyItemRegistrar.AUTOMATION_UPGRADE_CARD.get(), CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(), 1, "group.crazy_pattern_provider.name");
            Upgrades.add(CrazyItemRegistrar.PLAYER_UPGRADE_CARD.get(), CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(), 1, "group.crazy_pattern_provider.name");
            Upgrades.add(AEItems.FUZZY_CARD, CrazyItemRegistrar.MULTI_LEVEL_EMITTER_ITEM.get(), 1, "group.multi_level_emitter.name");
            Upgrades.add(AEItems.CRAFTING_CARD, CrazyItemRegistrar.MULTI_LEVEL_EMITTER_ITEM.get(), 1, "group.multi_level_emitter.name");

            if (IsModLoaded.isAppFluxLoaded()){
                Upgrades.add(AFItemAndBlock.INDUCTION_CARD, CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(), 1, "group.crazy_pattern_provider.name");
            }
        });
    }
}