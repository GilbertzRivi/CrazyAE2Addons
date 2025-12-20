package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.AEBaseBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.blocks.*;
import net.oktawia.crazyae2addons.items.*;

import java.util.List;

public class CrazyBlockRegistrar {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CrazyAddons.MODID);

    public static List<Block> getBlocks() {
        return BLOCKS.getEntries()
                .stream()
                .map(RegistryObject::get)
                .toList();
    }

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CrazyAddons.MODID);

    // =========================================================
    // Utility / pomiary
    // =========================================================

    public static final RegistryObject<AmpereMeterBlock> AMPERE_METER_BLOCK =
            BLOCKS.register("ampere_meter", AmpereMeterBlock::new);

    public static final RegistryObject<BlockItem> AMPERE_METER_BLOCK_ITEM =
            BLOCK_ITEMS.register("ampere_meter",
                    () -> new AEBaseBlockItem(AMPERE_METER_BLOCK.get(), new Item.Properties()));

    // =========================================================
    // AE2 / sieć / crafting / patterny
    // =========================================================

    public static final RegistryObject<SignallingInterfaceBlock> SIGNALLING_INTERFACE_BLOCK =
            BLOCKS.register("signalling_interface", SignallingInterfaceBlock::new);

    public static final RegistryObject<BlockItem> SIGNALLING_INTERFACE_BLOCK_ITEM =
            BLOCK_ITEMS.register("signalling_interface",
                    () -> new AEBaseBlockItem(SIGNALLING_INTERFACE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<CraftingSchedulerBlock> CRAFTING_SCHEDULER_BLOCK =
            BLOCKS.register("crafting_scheduler", CraftingSchedulerBlock::new);

    public static final RegistryObject<BlockItem> CRAFTING_SCHEDULER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crafting_scheduler",
                    () -> new AEBaseBlockItem(CRAFTING_SCHEDULER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<RecipeFabricatorBlock> RECIPE_FABRICATOR_BLOCK =
            BLOCKS.register("recipe_fabricator", RecipeFabricatorBlock::new);

    public static final RegistryObject<BlockItem> RECIPE_FABRICATOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("recipe_fabricator",
                    () -> new AEBaseBlockItem(RECIPE_FABRICATOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<CrazyPatternProviderBlock> CRAZY_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("crazy_pattern_provider", CrazyPatternProviderBlock::new);

    public static final RegistryObject<BlockItem> CRAZY_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crazy_pattern_provider",
                    () -> new CrazyPatternProviderBlockItem(CRAZY_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<ImpulsedPatternProviderBlock> IMPULSED_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("impulsed_pattern_provider", ImpulsedPatternProviderBlock::new);

    public static final RegistryObject<BlockItem> IMPULSED_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("impulsed_pattern_provider",
                    () -> new AEBaseBlockItem(IMPULSED_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BrokenPatternProviderBlock> BROKEN_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("broken_pattern_provider", BrokenPatternProviderBlock::new);

    public static final RegistryObject<BlockItem> BROKEN_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("broken_pattern_provider",
                    () -> new AEBaseBlockItem(BROKEN_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    // =========================================================
    // Maszyny / automatyzacja
    // =========================================================

    public static final RegistryObject<AutoEnchanterBlock> AUTO_ENCHANTER_BLOCK =
            BLOCKS.register("auto_enchanter", AutoEnchanterBlock::new);

    public static final RegistryObject<BlockItem> AUTO_ENCHANTER_BLOCK_ITEM =
            BLOCK_ITEMS.register("auto_enchanter",
                    () -> new AEBaseBlockItem(AUTO_ENCHANTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EjectorBlock> EJECTOR_BLOCK =
            BLOCKS.register("ejector", EjectorBlock::new);

    public static final RegistryObject<BlockItem> EJECTOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("ejector",
                    () -> new AEBaseBlockItem(EJECTOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<AutoBuilderBlock> AUTO_BUILDER_BLOCK =
            BLOCKS.register("auto_builder", AutoBuilderBlock::new);

    public static final RegistryObject<BlockItem> AUTO_BUILDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("auto_builder",
                    () -> new AEBaseBlockItem(AUTO_BUILDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<AutoBuilderCreativeSupplyBlock> AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK =
            BLOCKS.register("auto_builder_creative_supply", AutoBuilderCreativeSupplyBlock::new);

    public static final RegistryObject<BlockItem> AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK_ITEM =
            BLOCK_ITEMS.register("auto_builder_creative_supply",
                    () -> new AEBaseBlockItem(AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK.get(), new Item.Properties()));

    // =========================================================
    // Materia / kondensacja
    // =========================================================

    public static final RegistryObject<ReinforcedMatterCondenserBlock> REINFORCED_MATTER_CONDENSER_BLOCK =
            BLOCKS.register("reinforced_matter_condenser", ReinforcedMatterCondenserBlock::new);

    public static final RegistryObject<BlockItem> REINFORCED_MATTER_CONDENSER_BLOCK_ITEM =
            BLOCK_ITEMS.register("reinforced_matter_condenser",
                    () -> new AEBaseBlockItem(REINFORCED_MATTER_CONDENSER_BLOCK.get(), new Item.Properties()));

    // =========================================================
    // Energy Storage (tiered) + Dense
    // =========================================================

    public static final RegistryObject<EnergyStorage1k> ENERGY_STORAGE_1K_BLOCK =
            BLOCKS.register("energy_storage_1k", EnergyStorage1k::new);

    public static final RegistryObject<BlockItem> ENERGY_STORAGE_1K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_1k",
                    () -> new EnergyStorage1kBlockItem(ENERGY_STORAGE_1K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage4k> ENERGY_STORAGE_4K_BLOCK =
            BLOCKS.register("energy_storage_4k", EnergyStorage4k::new);

    public static final RegistryObject<BlockItem> ENERGY_STORAGE_4K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_4k",
                    () -> new EnergyStorage4kBlockItem(ENERGY_STORAGE_4K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage16k> ENERGY_STORAGE_16K_BLOCK =
            BLOCKS.register("energy_storage_16k", EnergyStorage16k::new);

    public static final RegistryObject<BlockItem> ENERGY_STORAGE_16K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_16k",
                    () -> new EnergyStorage16kBlockItem(ENERGY_STORAGE_16K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage64k> ENERGY_STORAGE_64K_BLOCK =
            BLOCKS.register("energy_storage_64k", EnergyStorage64k::new);

    public static final RegistryObject<BlockItem> ENERGY_STORAGE_64K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_64k",
                    () -> new EnergyStorage64kBlockItem(ENERGY_STORAGE_64K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage256k> ENERGY_STORAGE_256K_BLOCK =
            BLOCKS.register("energy_storage_256k", EnergyStorage256k::new);

    public static final RegistryObject<BlockItem> ENERGY_STORAGE_256K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_256k",
                    () -> new EnergyStorage256kBlockItem(ENERGY_STORAGE_256K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage1m> DENSE_ENERGY_STORAGE_1K_BLOCK =
            BLOCKS.register("energy_storage_1m", EnergyStorage1m::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_1K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_1m",
                    () -> new EnergyStorage1mBlockItem(DENSE_ENERGY_STORAGE_1K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage4m> DENSE_ENERGY_STORAGE_4K_BLOCK =
            BLOCKS.register("energy_storage_4m", EnergyStorage4m::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_4K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_4m",
                    () -> new EnergyStorage4mBlockItem(DENSE_ENERGY_STORAGE_4K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage16m> DENSE_ENERGY_STORAGE_16K_BLOCK =
            BLOCKS.register("energy_storage_16m", EnergyStorage16m::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_16K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_16m",
                    () -> new EnergyStorage16mBlockItem(DENSE_ENERGY_STORAGE_16K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage64m> DENSE_ENERGY_STORAGE_64K_BLOCK =
            BLOCKS.register("energy_storage_64m", EnergyStorage64m::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_64K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_64m",
                    () -> new EnergyStorage64mBlockItem(DENSE_ENERGY_STORAGE_64K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorage256m> DENSE_ENERGY_STORAGE_256K_BLOCK =
            BLOCKS.register("energy_storage_256m", EnergyStorage256m::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_256K_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_256m",
                    () -> new EnergyStorage256mBlockItem(DENSE_ENERGY_STORAGE_256K_BLOCK.get(), new Item.Properties()));

    // =========================================================
    // Multiblok: Mob Farm
    // =========================================================

    public static final RegistryObject<MobFarmControllerBlock> MOB_FARM_CONTROLLER =
            BLOCKS.register("mob_farm_controller", MobFarmControllerBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("mob_farm_controller",
                    () -> new AEBaseBlockItem(MOB_FARM_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmWallBlock> MOB_FARM_WALL =
            BLOCKS.register("mob_farm_wall", MobFarmWallBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_WALL_ITEM =
            BLOCK_ITEMS.register("mob_farm_wall",
                    () -> new AEBaseBlockItem(MOB_FARM_WALL.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmCollectorBlock> MOB_FARM_COLLECTOR =
            BLOCKS.register("mob_farm_collector", MobFarmCollectorBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_COLLECTOR_ITEM =
            BLOCK_ITEMS.register("mob_farm_collector",
                    () -> new AEBaseBlockItem(MOB_FARM_COLLECTOR.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmDamageBlock> MOB_FARM_DAMAGE =
            BLOCKS.register("mob_farm_damage", MobFarmDamageBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_DAMAGE_ITEM =
            BLOCK_ITEMS.register("mob_farm_damage",
                    () -> new AEBaseBlockItem(MOB_FARM_DAMAGE.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmInputBlock> MOB_FARM_INPUT =
            BLOCKS.register("mob_farm_input", MobFarmInputBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_INPUT_ITEM =
            BLOCK_ITEMS.register("mob_farm_input",
                    () -> new AEBaseBlockItem(MOB_FARM_INPUT.get(), new Item.Properties()));

    // =========================================================
    // Multiblok: Spawner Extractor
    // =========================================================

    public static final RegistryObject<SpawnerExtractorControllerBlock> SPAWNER_EXTRACTOR_CONTROLLER =
            BLOCKS.register("spawner_extractor_controller", SpawnerExtractorControllerBlock::new);

    public static final RegistryObject<BlockItem> SPAWNER_EXTRACTOR_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("spawner_extractor_controller",
                    () -> new AEBaseBlockItem(SPAWNER_EXTRACTOR_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<SpawnerExtractorWallBlock> SPAWNER_EXTRACTOR_WALL =
            BLOCKS.register("spawner_extractor_wall", SpawnerExtractorWallBlock::new);

    public static final RegistryObject<BlockItem> SPAWNER_EXTRACTOR_WALL_ITEM =
            BLOCK_ITEMS.register("spawner_extractor_wall",
                    () -> new AEBaseBlockItem(SPAWNER_EXTRACTOR_WALL.get(), new Item.Properties()));

    // =========================================================
    // Entropy / singularity (multiblok)
    // =========================================================

    public static final RegistryObject<EntropyCradle> ENTROPY_CRADLE =
            BLOCKS.register("entropy_cradle", EntropyCradle::new);

    public static final RegistryObject<BlockItem> ENTROPY_CRADLE_ITEM =
            BLOCK_ITEMS.register("entropy_cradle",
                    () -> new AEBaseBlockItem(ENTROPY_CRADLE.get(), new Item.Properties()));

    public static final RegistryObject<EntropyCradleController> ENTROPY_CRADLE_CONTROLLER =
            BLOCKS.register("entropy_cradle_controller", EntropyCradleController::new);

    public static final RegistryObject<BlockItem> ENTROPY_CRADLE_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("entropy_cradle_controller",
                    () -> new AEBaseBlockItem(ENTROPY_CRADLE_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<EntropyCradleCapacitor> ENTROPY_CRADLE_CAPACITOR =
            BLOCKS.register("entropy_cradle_capacitor", EntropyCradleCapacitor::new);

    public static final RegistryObject<BlockItem> ENTROPY_CRADLE_CAPACITOR_ITEM =
            BLOCK_ITEMS.register("entropy_cradle_capacitor",
                    () -> new AEBaseBlockItem(ENTROPY_CRADLE_CAPACITOR.get(), new Item.Properties()));

    public static final RegistryObject<SuperSingularityBlock> SUPER_SINGULARITY_BLOCK =
            BLOCKS.register("super_singularity_block", SuperSingularityBlock::new);

    public static final RegistryObject<BlockItem> SUPER_SINGULARITY_BLOCK_ITEM =
            BLOCK_ITEMS.register("super_singularity_block",
                    () -> new AEBaseBlockItem(SUPER_SINGULARITY_BLOCK.get(), new Item.Properties()));

    // =========================================================
    // Research (stacja / pedestal / unit / kabel / stabilizacja)
    // =========================================================

    public static final RegistryObject<ResearchStation> RESEARCH_STATION =
            BLOCKS.register("research_station", ResearchStation::new);

    public static final RegistryObject<BlockItem> RESEARCH_STATION_BLOCK_ITEM =
            BLOCK_ITEMS.register("research_station",
                    () -> new AEBaseBlockItem(RESEARCH_STATION.get(), new Item.Properties()));

    public static final RegistryObject<ResearchPedestalBottomBlock> RESEARCH_PEDESTAL_BOTTOM =
            BLOCKS.register("research_pedestal_bottom", ResearchPedestalBottomBlock::new);

    public static final RegistryObject<BlockItem> RESEARCH_PEDESTAL_BOTTOM_BLOCK_ITEM =
            BLOCK_ITEMS.register("research_pedestal_bottom",
                    () -> new AEBaseBlockItem(RESEARCH_PEDESTAL_BOTTOM.get(), new Item.Properties()));

    public static final RegistryObject<ResearchPedestalTopBlock> RESEARCH_PEDESTAL_TOP =
            BLOCKS.register("research_pedestal_top", ResearchPedestalTopBlock::new);

    public static final RegistryObject<BlockItem> RESEARCH_PEDESTAL_TOP_BLOCK_ITEM =
            BLOCK_ITEMS.register("research_pedestal_top",
                    () -> new AEBaseBlockItem(RESEARCH_PEDESTAL_TOP.get(), new Item.Properties()));

    public static final RegistryObject<ResearchCableBlock> RESEARCH_CABLE_BLOCK =
            BLOCKS.register("research_cable", ResearchCableBlock::new);

    public static final RegistryObject<BlockItem> RESEARCH_CABLE_BLOCK_ITEM =
            BLOCK_ITEMS.register("research_cable",
                    () -> new BlockItem(CrazyBlockRegistrar.RESEARCH_CABLE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<ResearchUnitFrameBlock> RESEARCH_UNIT_FRAME =
            BLOCKS.register("research_unit_frame", ResearchUnitFrameBlock::new);

    public static final RegistryObject<BlockItem> RESEARCH_UNIT_FRAME_BLOCK_ITEM =
            BLOCK_ITEMS.register("research_unit_frame",
                    () -> new AEBaseBlockItem(CrazyBlockRegistrar.RESEARCH_UNIT_FRAME.get(), new Item.Properties()));

    public static final RegistryObject<ResearchUnit> RESEARCH_UNIT =
            BLOCKS.register("research_unit", ResearchUnit::new);

    public static final RegistryObject<BlockItem> RESEARCH_UNIT_BLOCK_ITEM =
            BLOCK_ITEMS.register("research_unit",
                    () -> new AEBaseBlockItem(CrazyBlockRegistrar.RESEARCH_UNIT.get(), new Item.Properties()));

    // =========================================================
    // Penrose (multiblok + elementy)
    // =========================================================

    public static final RegistryObject<PenroseControllerBlock> PENROSE_CONTROLLER =
            BLOCKS.register("penrose_controller", PenroseControllerBlock::new);

    public static final RegistryObject<BlockItem> PENROSE_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("penrose_controller",
                    () -> new AEBaseBlockItem(PENROSE_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<PenroseFrameBlock> PENROSE_FRAME =
            BLOCKS.register("penrose_frame", PenroseFrameBlock::new);

    public static final RegistryObject<BlockItem> PENROSE_FRAME_ITEM =
            BLOCK_ITEMS.register("penrose_frame",
                    () -> new AEBaseBlockItem(PENROSE_FRAME.get(), new Item.Properties()));

    public static final RegistryObject<PenroseCoilBlock> PENROSE_COIL =
            BLOCKS.register("penrose_coil", PenroseCoilBlock::new);

    public static final RegistryObject<BlockItem> PENROSE_COIL_ITEM =
            BLOCK_ITEMS.register("penrose_coil",
                    () -> new AEBaseBlockItem(PENROSE_COIL.get(), new Item.Properties()));

    public static final RegistryObject<PenrosePortBlock> PENROSE_PORT =
            BLOCKS.register("penrose_port", PenrosePortBlock::new);

    public static final RegistryObject<BlockItem> PENROSE_PORT_ITEM =
            BLOCK_ITEMS.register("penrose_port",
                    () -> new AEBaseBlockItem(PENROSE_PORT.get(), new Item.Properties()));

    public static final RegistryObject<PenroseMassEmitter> PENROSE_MASS_EMITTER =
            BLOCKS.register("penrose_mass_emitter", PenroseMassEmitter::new);

    public static final RegistryObject<BlockItem> PENROSE_MASS_EMITTER_BLOCK_ITEM =
            BLOCK_ITEMS.register("penrose_mass_emitter",
                    () -> new AEBaseBlockItem(CrazyBlockRegistrar.PENROSE_MASS_EMITTER.get(), new Item.Properties()));

    public static final RegistryObject<PenroseHeatEmitter> PENROSE_HEAT_EMITTER =
            BLOCKS.register("penrose_heat_emitter", PenroseHeatEmitter::new);

    public static final RegistryObject<BlockItem> PENROSE_HEAT_EMITTER_BLOCK_ITEM =
            BLOCK_ITEMS.register("penrose_heat_emitter",
                    () -> new AEBaseBlockItem(CrazyBlockRegistrar.PENROSE_HEAT_EMITTER.get(), new Item.Properties()));

    public static final RegistryObject<PenroseInjectionPort> PENROSE_INJECTION_PORT =
            BLOCKS.register("penrose_injection_port", PenroseInjectionPort::new);

    public static final RegistryObject<BlockItem> PENROSE_INJECTION_PORT_BLOCK_ITEM =
            BLOCK_ITEMS.register("penrose_injection_port",
                    () -> new AEBaseBlockItem(CrazyBlockRegistrar.PENROSE_INJECTION_PORT.get(), new Item.Properties()));

    public static final RegistryObject<PenroseHeatVent> PENROSE_HEAT_VENT =
            BLOCKS.register("penrose_heat_vent", PenroseHeatVent::new);

    public static final RegistryObject<BlockItem> PENROSE_HEAT_VENT_BLOCK_ITEM =
            BLOCK_ITEMS.register("penrose_heat_vent",
                    () -> new AEBaseBlockItem(CrazyBlockRegistrar.PENROSE_HEAT_VENT.get(), new Item.Properties()));

    public static final RegistryObject<PenroseHawkingVent> PENROSE_HAWKING_VENT =
            BLOCKS.register("penrose_hawking_vent", PenroseHawkingVent::new);

    public static final RegistryObject<BlockItem> PENROSE_HAWKING_VENT_BLOCK_ITEM =
            BLOCK_ITEMS.register("penrose_hawking_vent",
                    () -> new AEBaseBlockItem(CrazyBlockRegistrar.PENROSE_HAWKING_VENT.get(), new Item.Properties()));

    private CrazyBlockRegistrar() {}
}
