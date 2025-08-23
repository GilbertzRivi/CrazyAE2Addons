package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.blocks.EnergyStorageFrame;
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

    public static final RegistryObject<BlockEntityType<MEDataControllerBE>> ME_DATA_CONTROLLER_BE =
            reg("me_data_controller_be", CrazyBlockRegistrar.ME_DATA_CONTROLLER_BLOCK, MEDataControllerBE::new, MEDataControllerBE.class);

    public static final RegistryObject<BlockEntityType<DataProcessorBE>> DATA_PROCESSOR_BE =
            reg("data_processor_be", CrazyBlockRegistrar.DATA_PROCESSOR_BLOCK, DataProcessorBE::new, DataProcessorBE.class);

    public static final RegistryObject<BlockEntityType<ImpulsedPatternProviderBE>> IMPULSED_PATTERN_PROVIDER_BE =
            reg("impulsed_pp_be", CrazyBlockRegistrar.IMPULSED_PATTERN_PROVIDER_BLOCK, ImpulsedPatternProviderBE::new, ImpulsedPatternProviderBE.class);

    public static final RegistryObject<BlockEntityType<SignallingInterfaceBE>> SIGNALLING_INTERFACE_BE =
            reg("signalling_interface_be", CrazyBlockRegistrar.SIGNALLING_INTERFACE_BLOCK, SignallingInterfaceBE::new, SignallingInterfaceBE.class);

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

    public static final RegistryObject<BlockEntityType<EnergyStorageControllerBE>> ENERGY_STORAGE_CONTROLLER_BE =
            reg("energy_storage_controller_be", CrazyBlockRegistrar.ENERGY_STORAGE_CONTROLLER_BLOCK, EnergyStorageControllerBE::new, EnergyStorageControllerBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStorageFrameBE>> ENERGY_STORAGE_FRAME_BE =
            reg("energy_storage_frame_be", CrazyBlockRegistrar.ENERGY_STORAGE_FRAME_BLOCK, EnergyStorageFrameBE::new, EnergyStorageFrameBE.class);

    public static final RegistryObject<BlockEntityType<EnergyStoragePortBE>> ENERGY_STORAGE_PORT_BE =
            reg("energy_storage_port_be", CrazyBlockRegistrar.ENERGY_STORAGE_PORT_BLOCK, EnergyStoragePortBE::new, EnergyStoragePortBE.class);

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

    public static final RegistryObject<BlockEntityType<PatternManagementUnitBE>> PATTERN_MANAGEMENT_UNIT_BE =
            reg("pattern_management_unit_be", CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_BLOCK, PatternManagementUnitBE::new, PatternManagementUnitBE.class);

    public static final RegistryObject<BlockEntityType<PatternManagementUnitFrameBE>> PATTERN_MANAGEMENT_UNIT_FRAME_BE =
            reg("pattern_management_unit_frame_be", CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_FRAME_BLOCK, PatternManagementUnitFrameBE::new, PatternManagementUnitFrameBE.class);

    public static final RegistryObject<BlockEntityType<PatternManagementUnitWallBE>> PATTERN_MANAGEMENT_UNIT_WALL_BE =
            reg("pattern_management_unit_wall_be", CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_WALL_BLOCK, PatternManagementUnitWallBE::new, PatternManagementUnitWallBE.class);

    public static final RegistryObject<BlockEntityType<PatternManagementUnitControllerBE>> PATTERN_MANAGEMENT_UNIT_CONTROLLER_BE =
            reg("pattern_management_unit_controller_be", CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_CONTROLLER_BLOCK, PatternManagementUnitControllerBE::new, PatternManagementUnitControllerBE.class);

    public static final RegistryObject<BlockEntityType<ResearchStationBE>> RESEARCH_STATION_BE =
            reg("research_station_be", CrazyBlockRegistrar.RESEARCH_STATION, ResearchStationBE::new, ResearchStationBE.class);

    public static final RegistryObject<BlockEntityType<RecipeFabricatorBE>> RECIPE_FABRICATOR_BE =
            reg("recipe_fabricator", CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK, RecipeFabricatorBE::new, RecipeFabricatorBE.class);

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