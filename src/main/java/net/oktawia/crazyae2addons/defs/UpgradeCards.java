package net.oktawia.crazyae2addons.defs;

import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import com.glodblock.github.appflux.common.AFItemAndBlock;
import com.glodblock.github.appflux.common.items.ItemInductionCard;
import de.mari_023.ae2wtlib.AE2wtlib;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;

public class UpgradeCards {
    public UpgradeCards(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Upgrades.add(AEItems.SPEED_CARD, CrazyItemRegistrar.ENTITY_TICKER_PART_ITEM.get(), 8, "group.entity_ticker.name");
            Upgrades.add(AEItems.SPEED_CARD, CrazyItemRegistrar.ENERGY_EXPORTER_PART_ITEM.get(), 6, "group.energy_exporter.name");
            Upgrades.add(AEItems.REDSTONE_CARD, CrazyBlockRegistrar.SIGNALLING_INTERFACE_BLOCK_ITEM.get(), 1, "group.signalling_interface.name");
            Upgrades.add(AEItems.FUZZY_CARD, CrazyBlockRegistrar.SIGNALLING_INTERFACE_BLOCK_ITEM.get(), 1, "group.signalling_interface.name");
            Upgrades.add(AEItems.INVERTER_CARD, CrazyBlockRegistrar.SIGNALLING_INTERFACE_BLOCK_ITEM.get(), 1, "group.signalling_interface.name");
            Upgrades.add(CrazyItemRegistrar.CIRCUIT_UPGRADE_CARD_ITEM.get(), AEParts.STORAGE_BUS, 1, "group.storage_bus.name");
            Upgrades.add(AEItems.INVERTER_CARD, CrazyItemRegistrar.MOB_FORMATION_PLANE.get(), 1, "group.mob_formation_plane.name");
            Upgrades.add(AEItems.CAPACITY_CARD, CrazyItemRegistrar.MOB_FORMATION_PLANE.get(), 4, "group.mob_formation_plane.name");
            Upgrades.add(AEItems.SPEED_CARD, CrazyBlockRegistrar.SPAWNER_EXTRACTOR_CONTROLLER.get(), 4, "group.spawner_extractor_controller.name");
            Upgrades.add(AEItems.SPEED_CARD, CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get(), 4, "group.mob_farm_controller.name");
            Upgrades.add(CrazyItemRegistrar.LOOTING_UPGRADE_CARD.get(), CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get(), 4, "group.mob_farm_controller.name");
            Upgrades.add(CrazyItemRegistrar.EXPERIENCE_UPGRADE_CARD.get(), CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get(), 4, "group.mob_farm_controller.name");
            Upgrades.add(AEItems.VOID_CARD, CrazyItemRegistrar.NBT_STORAGE_BUS_PART_ITEM.get(), 1, "group.nbt_storage_bus.name");
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(), 1, "group.wireless_redstone_terminal.name");
            Upgrades.add(AEItems.SPEED_CARD, CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 6, "group.auto_builder.name");
            Upgrades.add(AEItems.CRAFTING_CARD, CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 1, "group.auto_builder.name");
            Upgrades.add(AE2wtlib.QUANTUM_BRIDGE_CARD, CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(), 1, "group.wireless_redstone_terminal.name");
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.STRUCTURE_GADGET.get(), 4, "group.structure_gadget.name");
            Upgrades.add(AEItems.ENERGY_CARD, CrazyItemRegistrar.SAMSUNG_GALAXY_S6.get(), 4, "group.samsung_galaxy_s6.name");
            Upgrades.add(AEItems.CRAFTING_CARD, CrazyItemRegistrar.SAMSUNG_GALAXY_S6.get(), 1, "group.samsung_galaxy_s6.name");

            if (IsModLoaded.isAppFluxLoaded()){
                Upgrades.add(AFItemAndBlock.INDUCTION_CARD, CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(), 1, "group.crazy_pattern_provider.name");
            }
        });
    }
}