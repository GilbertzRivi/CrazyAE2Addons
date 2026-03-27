package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.blocks.EnergyStorage1k;
import net.oktawia.crazyae2addons.compat.Apotheosis.ApothAutoEnchanterBE;
import net.oktawia.crazyae2addons.compat.GregTech.*;
import net.oktawia.crazyae2addons.entities.*;

import java.util.ArrayList;
import java.util.List;

public class CrazyBlockEntityRegistrar {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CrazyAddons.MODID);

    private static final List<Runnable> BLOCK_ENTITY_SETUP = new ArrayList<>();

    private static <T extends AEBaseBlockEntity> RegistryObject<BlockEntityType<T>> reg(
            String id,
            RegistryObject<? extends AEBaseEntityBlock<?>> block,
            BlockEntityType.BlockEntitySupplier<T> factory,
            Class<T> blockEntityClass
    ) {
        return BLOCK_ENTITIES.register(id, () -> {
            var blk = block.get();
            var type = BlockEntityType.Builder.of(factory, blk).build(null);

            BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity(
                    (Class) blockEntityClass, (BlockEntityType) type, null, null
            ));

            return type;
        });
    }

    public static final RegistryObject<BlockEntityType<EjectorBE>> EJECTOR_BE =
            reg("ejector_be", CrazyBlockRegistrar.EJECTOR_BLOCK, EjectorBE::new, EjectorBE.class);

    public static final RegistryObject<BlockEntityType<SpawnerExtractorControllerBE>> SPAWNER_EXTRACTOR_CONTROLLER_BE =
            reg("spawner_extractor_controller_be", CrazyBlockRegistrar.SPAWNER_EXTRACTOR_CONTROLLER, SpawnerExtractorControllerBE::new, SpawnerExtractorControllerBE.class);

    public static final RegistryObject<BlockEntityType<SpawnerExtractorWallBE>> SPAWNER_EXTRACTOR_WALL_BE =
            reg("spawner_extractor_wall_be", CrazyBlockRegistrar.SPAWNER_EXTRACTOR_WALL, SpawnerExtractorWallBE::new, SpawnerExtractorWallBE.class);

    public static final RegistryObject<BlockEntityType<MobFarmControllerBE>> MOB_FARM_CONTROLLER_BE =
            reg("mob_farm_controller_be", CrazyBlockRegistrar.MOB_FARM_CONTROLLER, MobFarmControllerBE::new, MobFarmControllerBE.class);

    public static final RegistryObject<BlockEntityType<MobFarmWallBE>> MOB_FARM_WALL_BE =
            reg("mob_farm_wall_be", CrazyBlockRegistrar.MOB_FARM_WALL, MobFarmWallBE::new, MobFarmWallBE.class);

    public static final RegistryObject<BlockEntityType<CraftingSchedulerBE>> CRAFTING_SHEDULER_BE =
            reg("crafting_scheduler_be", CrazyBlockRegistrar.CRAFTING_SCHEDULER_BLOCK, CraftingSchedulerBE::new, CraftingSchedulerBE.class);

    public static final RegistryObject<BlockEntityType<ReinforcedMatterCondenserBE>> REINFORCED_MATTER_CONDENSER_BE =
            reg("reinforced_matter_condenser_be", CrazyBlockRegistrar.REINFORCED_MATTER_CONDENSER_BLOCK, ReinforcedMatterCondenserBE::new, ReinforcedMatterCondenserBE.class);

    public static final RegistryObject<BlockEntityType<PenroseFrameBE>> PENROSE_FRAME_BE =
            reg("penrose_frame_be", CrazyBlockRegistrar.PENROSE_FRAME, PenroseFrameBE::new, PenroseFrameBE.class);

    public static final RegistryObject<BlockEntityType<PenroseCoilBE>> PENROSE_COIL_BE =
            reg("penrose_coil_be", CrazyBlockRegistrar.PENROSE_COIL, PenroseCoilBE::new, PenroseCoilBE.class);

    public static final RegistryObject<BlockEntityType<PenrosePortBE>> PENROSE_PORT_BE =
            reg("penrose_port_be", CrazyBlockRegistrar.PENROSE_PORT, PenrosePortBE::new, PenrosePortBE.class);

    public static final RegistryObject<BlockEntityType<PenroseControllerBE>> PENROSE_CONTROLLER_BE =
            reg("penrose_controller_be", CrazyBlockRegistrar.PENROSE_CONTROLLER, PenroseControllerBE::new, PenroseControllerBE.class);

    public static final RegistryObject<BlockEntityType<CrazyPatternProviderBE>> CRAZY_PATTERN_PROVIDER_BE =
            reg("crazy_pattern_provider_be", CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK, CrazyPatternProviderBE::new, CrazyPatternProviderBE.class);

    public static final RegistryObject<BlockEntityType<AutoBuilderBE>> AUTO_BUILDER_BE =
            reg("auto_builder_be", CrazyBlockRegistrar.AUTO_BUILDER_BLOCK, AutoBuilderBE::new, AutoBuilderBE.class);

    public static final RegistryObject<BlockEntityType<AutoBuilderCreativeSupplyBE>> AUTO_BUILDER_CREATIVE_SUPPLY_BE =
            reg("auto_builder_creative_supply_be", CrazyBlockRegistrar.AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK, AutoBuilderCreativeSupplyBE::new, AutoBuilderCreativeSupplyBE.class);

    public static final RegistryObject<BlockEntityType<BrokenPatternProviderBE>> BROKEN_PATTERN_PROVIDER_BE =
            reg("broken_pattern_provider_be", CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK, BrokenPatternProviderBE::new, BrokenPatternProviderBE.class);

    public static final RegistryObject<BlockEntityType<EntropyCradleBE>> ENTROPY_CRADLE_BE =
            reg("entropy_cradle_be", CrazyBlockRegistrar.ENTROPY_CRADLE, EntropyCradleBE::new, EntropyCradleBE.class);

    public static final RegistryObject<BlockEntityType<EntropyCradleCapacitorBE>> ENTROPY_CRADLE_CAPACITOR_BE =
            reg("entropy_cradle_capacitor_be", CrazyBlockRegistrar.ENTROPY_CRADLE_CAPACITOR, EntropyCradleCapacitorBE::new, EntropyCradleCapacitorBE.class);

    public static final RegistryObject<BlockEntityType<EntropyCradleControllerBE>> ENTROPY_CRADLE_CONTROLLER_BE =
            reg("entropy_cradle_controller_be", CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER, EntropyCradleControllerBE::new, EntropyCradleControllerBE.class);

    public static final RegistryObject<BlockEntityType<SuperSingularityBlockBE>> SUPER_SINGULARITY_BLOCK_BE =
            reg("super_singularity_block_be", CrazyBlockRegistrar.SUPER_SINGULARITY_BLOCK, SuperSingularityBlockBE::new, SuperSingularityBlockBE.class);

    public static final RegistryObject<BlockEntityType<ResearchStationBE>> RESEARCH_STATION_BE =
            reg("research_station_be", CrazyBlockRegistrar.RESEARCH_STATION, ResearchStationBE::new, ResearchStationBE.class);

    public static final RegistryObject<BlockEntityType<RecipeFabricatorBE>> RECIPE_FABRICATOR_BE =
            reg("recipe_fabricator", CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK, RecipeFabricatorBE::new, RecipeFabricatorBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage1kBE>> ENERGY_STORAGE_1K =
            reg("energy_storage_1k", CrazyBlockRegistrar.ENERGY_STORAGE_1K_BLOCK, EnergyStorage1kBE::new, EnergyStorage1kBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage1mBE>> ENERGY_STORAGE_1M =
            reg("energy_storage_1m", CrazyBlockRegistrar.DENSE_ENERGY_STORAGE_1K_BLOCK, EnergyStorage1mBE::new, EnergyStorage1mBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage4kBE>> ENERGY_STORAGE_4K =
            reg("energy_storage_4k", CrazyBlockRegistrar.ENERGY_STORAGE_4K_BLOCK, EnergyStorage4kBE::new, EnergyStorage4kBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage4mBE>> ENERGY_STORAGE_4M =
            reg("energy_storage_4m", CrazyBlockRegistrar.DENSE_ENERGY_STORAGE_4K_BLOCK, EnergyStorage4mBE::new, EnergyStorage4mBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage16kBE>> ENERGY_STORAGE_16K =
            reg("energy_storage_16k", CrazyBlockRegistrar.ENERGY_STORAGE_16K_BLOCK, EnergyStorage16kBE::new, EnergyStorage16kBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage16mBE>> ENERGY_STORAGE_16M =
            reg("energy_storage_16m", CrazyBlockRegistrar.DENSE_ENERGY_STORAGE_16K_BLOCK, EnergyStorage16mBE::new, EnergyStorage16mBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage64kBE>> ENERGY_STORAGE_64K =
            reg("energy_storage_64k", CrazyBlockRegistrar.ENERGY_STORAGE_64K_BLOCK, EnergyStorage64kBE::new, EnergyStorage64kBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage64mBE>> ENERGY_STORAGE_64M =
            reg("energy_storage_64m", CrazyBlockRegistrar.DENSE_ENERGY_STORAGE_64K_BLOCK, EnergyStorage64mBE::new, EnergyStorage64mBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage256kBE>> ENERGY_STORAGE_256K =
            reg("energy_storage_256k", CrazyBlockRegistrar.ENERGY_STORAGE_256K_BLOCK, EnergyStorage256kBE::new, EnergyStorage256kBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorage256mBE>> ENERGY_STORAGE_256M =
            reg("energy_storage_256m", CrazyBlockRegistrar.DENSE_ENERGY_STORAGE_256K_BLOCK, EnergyStorage256mBE::new, EnergyStorage256mBE.class);

    public static final RegistryObject<BlockEntityType<ResearchPedestalBottomBE>> RESEARCH_PEDESTAL_BOTTOM_BE =
            reg("research_pedestal_bottom", CrazyBlockRegistrar.RESEARCH_PEDESTAL_BOTTOM, ResearchPedestalBottomBE::new, ResearchPedestalBottomBE.class);

    public static final RegistryObject<BlockEntityType<ResearchPedestalTopBE>> RESEARCH_PEDESTAL_TOP_BE =
            reg("research_pedestal_top", CrazyBlockRegistrar.RESEARCH_PEDESTAL_TOP, ResearchPedestalTopBE::new, ResearchPedestalTopBE.class);

    public static final RegistryObject<BlockEntityType<ResearchUnitFrameBE>> RESEARCH_UNIT_FRAME_BE =
            reg("research_unit_frame", CrazyBlockRegistrar.RESEARCH_UNIT_FRAME, ResearchUnitFrameBE::new, ResearchUnitFrameBE.class);

    public static final RegistryObject<BlockEntityType<ResearchUnitBE>> RESEARCH_UNIT_BE =
            reg("research_unit", CrazyBlockRegistrar.RESEARCH_UNIT, ResearchUnitBE::new, ResearchUnitBE.class);

    public static final RegistryObject<BlockEntityType<PenroseMassEmitterBE>> PENROSE_MASS_EMITTER_BE =
            reg("penrose_mass_emitter", CrazyBlockRegistrar.PENROSE_MASS_EMITTER, PenroseMassEmitterBE::new, PenroseMassEmitterBE.class);

    public static final RegistryObject<BlockEntityType<PenroseHeatEmitterBE>> PENROSE_HEAT_EMITTER_BE =
            reg("penrose_heat_emitter", CrazyBlockRegistrar.PENROSE_HEAT_EMITTER, PenroseHeatEmitterBE::new, PenroseHeatEmitterBE.class);

    public static final RegistryObject<BlockEntityType<PenroseInjectionPortBE>> PENROSE_INJECTION_PORT_BE =
            reg("penrose_injection_port", CrazyBlockRegistrar.PENROSE_INJECTION_PORT, PenroseInjectionPortBE::new, PenroseInjectionPortBE.class);

    public static final RegistryObject<BlockEntityType<PenroseHeatVentBE>> PENROSE_HEAT_VENT_BE =
            reg("penrose_heat_vent", CrazyBlockRegistrar.PENROSE_HEAT_VENT, PenroseHeatVentBE::new, PenroseHeatVentBE.class);

    public static final RegistryObject<BlockEntityType<PenroseHawkingVentBE>> PENROSE_HAWKING_VENT_BE =
            reg("penrose_hawking_vent", CrazyBlockRegistrar.PENROSE_HAWKING_VENT, PenroseHawkingVentBE::new, PenroseHawkingVentBE.class);

    public static final RegistryObject<BlockEntityType<? extends AmpereMeterBE>> AMPERE_METER_BE =
            BLOCK_ENTITIES.register("ampere_meter_be", () -> {
                var blk = CrazyBlockRegistrar.AMPERE_METER_BLOCK.get();
                if (IsModLoaded.isGTCEuLoaded()) {
                    var type = BlockEntityType.Builder.of(GTAmpereMeterBE::new, blk).build(null);
                    BLOCK_ENTITY_SETUP.add(() -> ((AEBaseEntityBlock) blk).setBlockEntity(GTAmpereMeterBE.class, type, null, null));
                    return type;
                } else {
                    var type = BlockEntityType.Builder.of(AmpereMeterBE::new, blk).build(null);
                    BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity(AmpereMeterBE.class, type, null, null));
                    return type;
                }
            });

    public static final RegistryObject<BlockEntityType<? extends AutoEnchanterBE>> AUTO_ENCHANTER_BE =
            BLOCK_ENTITIES.register("auto_enchanter", () -> {
                var blk = CrazyBlockRegistrar.AUTO_ENCHANTER_BLOCK.get();
                if (IsModLoaded.isApothLoaded()) {
                    var type = BlockEntityType.Builder.of(ApothAutoEnchanterBE::new, blk).build(null);
                    BLOCK_ENTITY_SETUP.add(() -> ((AEBaseEntityBlock) blk).setBlockEntity(ApothAutoEnchanterBE.class, type, null, null));
                    return type;
                } else {
                    var type = BlockEntityType.Builder.of(AutoEnchanterBE::new, blk).build(null);
                    BLOCK_ENTITY_SETUP.add(() -> blk.setBlockEntity(AutoEnchanterBE.class, type, null, null));
                    return type;
                }
            });

    public static void setupBlockEntityTypes() {
        for (var runnable : BLOCK_ENTITY_SETUP) {
            runnable.run();
        }
    }

    public static List<? extends BlockEntityType<?>> getEntities() {
        return BLOCK_ENTITIES.getEntries().stream().map(RegistryObject::get).toList();
    }
}