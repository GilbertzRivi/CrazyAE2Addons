package net.oktawia.crazyae2addons.defs.regs;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.blocks.*;
import net.oktawia.crazyae2addons.items.*;
import net.oktawia.crazyae2addons.menus.EnergyStorageControllerMenu;

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

    public static final RegistryObject<CraftingCancelerBlock> CRAFTING_CANCELER_BLOCK =
            BLOCKS.register("crafting_canceler", CraftingCancelerBlock::new);

    public static final RegistryObject<BlockItem> CRAFTING_CANCELER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crafting_canceler",
                    () -> new CraftingCancelerBlockItem(CRAFTING_CANCELER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<MEDataControllerBlock> ME_DATA_CONTROLLER_BLOCK =
            BLOCKS.register("me_data_controller", MEDataControllerBlock::new);

    public static final RegistryObject<BlockItem> ME_DATA_CONTROLLER_BLOCK_ITEM =
            BLOCK_ITEMS.register("me_data_controller",
                    () -> new MEDataControllerBlockItem(ME_DATA_CONTROLLER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<DataProcessorBlock> DATA_PROCESSOR_BLOCK =
            BLOCKS.register("data_processor", DataProcessorBlock::new);

    public static final RegistryObject<BlockItem> DATA_PROCESSOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("data_processor",
                    () -> new DataProcessorBlockItem(DATA_PROCESSOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<DataTrackerBlock> DATA_TRACKER_BLOCK =
            BLOCKS.register("data_tracker", DataTrackerBlock::new);

    public static final RegistryObject<BlockItem> DATA_TRACKER_BLOCK_ITEM =
            BLOCK_ITEMS.register("data_tracker",
                    () -> new DataTrackerBlockItem(DATA_TRACKER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<AmpereMeterBlock> AMPERE_METER_BLOCK =
            BLOCKS.register("ampere_meter", AmpereMeterBlock::new);

    public static final RegistryObject<BlockItem> AMPERE_METER_BLOCK_ITEM =
            BLOCK_ITEMS.register("ampere_meter",
                    () -> new AmpereMeterBlockItem(AMPERE_METER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<IsolatedDataProcessorBlock> ISOLATED_DATA_PROCESSOR_BLOCK =
            BLOCKS.register("isolated_data_processor", IsolatedDataProcessorBlock::new);

    public static final RegistryObject<BlockItem> ISOLATED_DATA_PROCESSOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("isolated_data_processor",
                    () -> new IsolatedDataProcessorBlockItem(ISOLATED_DATA_PROCESSOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<ImpulsedPatternProviderBlock> IMPULSED_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("impulsed_pattern_provider", ImpulsedPatternProviderBlock::new);

    public static final RegistryObject<BlockItem> IMPULSED_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("impulsed_pattern_provider",
                    () -> new ImpulsedPatternProviderBlockItem(IMPULSED_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<SignallingInterfaceBlock> SIGNALLING_INTERFACE_BLOCK =
            BLOCKS.register("signalling_interface", SignallingInterfaceBlock::new);

    public static final RegistryObject<BlockItem> SIGNALLING_INTERFACE_BLOCK_ITEM =
            BLOCK_ITEMS.register("signalling_interface",
                    () -> new SignallingInterfaceBlockItem(SIGNALLING_INTERFACE_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<AutoEnchanterBlock> AUTO_ENCHANTER_BLOCK =
            BLOCKS.register("auto_enchanter", AutoEnchanterBlock::new);

    public static final RegistryObject<BlockItem> AUTO_ENCHANTER_BLOCK_ITEM =
            BLOCK_ITEMS.register("auto_enchanter",
                    () -> new AutoEnchanterBlockItem(AUTO_ENCHANTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EjectorBlock> EJECTOR_BLOCK =
            BLOCKS.register("ejector", EjectorBlock::new);

    public static final RegistryObject<BlockItem> EJECTOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("ejector",
                    () -> new EjectorBlockItem(EJECTOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<SpawnerExtractorControllerBlock> SPAWNER_EXTRACTOR_CONTROLLER =
            BLOCKS.register("spawner_extractor_controller", SpawnerExtractorControllerBlock::new);

    public static final RegistryObject<BlockItem> SPAWNER_EXTRACTOR_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("spawner_extractor_controller",
                    () -> new EjectorBlockItem(SPAWNER_EXTRACTOR_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<SpawnerExtractorWallBlock> SPAWNER_EXTRACTOR_WALL =
            BLOCKS.register("spawner_extractor_wall", SpawnerExtractorWallBlock::new);

    public static final RegistryObject<BlockItem> SPAWNER_EXTRACTOR_WALL_ITEM =
            BLOCK_ITEMS.register("spawner_extractor_wall",
                    () -> new EjectorBlockItem(SPAWNER_EXTRACTOR_WALL.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmWallBlock> MOB_FARM_WALL =
            BLOCKS.register("mob_farm_wall", MobFarmWallBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_WALL_ITEM =
            BLOCK_ITEMS.register("mob_farm_wall",
                    () -> new EjectorBlockItem(MOB_FARM_WALL.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmControllerBlock> MOB_FARM_CONTROLLER =
            BLOCKS.register("mob_farm_controller", MobFarmControllerBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("mob_farm_controller",
                    () -> new EjectorBlockItem(MOB_FARM_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmCollectorBlock> MOB_FARM_COLLECTOR =
            BLOCKS.register("mob_farm_collector", MobFarmCollectorBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_COLLECTOR_ITEM =
            BLOCK_ITEMS.register("mob_farm_collector",
                    () -> new EjectorBlockItem(MOB_FARM_COLLECTOR.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmDamageBlock> MOB_FARM_DAMAGE =
            BLOCKS.register("mob_farm_damage", MobFarmDamageBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_DAMAGE_ITEM =
            BLOCK_ITEMS.register("mob_farm_damage",
                    () -> new EjectorBlockItem(MOB_FARM_DAMAGE.get(), new Item.Properties()));

    public static final RegistryObject<MobFarmInputBlock> MOB_FARM_INPUT =
            BLOCKS.register("mob_farm_input", MobFarmInputBlock::new);

    public static final RegistryObject<BlockItem> MOB_FARM_INPUT_ITEM =
            BLOCK_ITEMS.register("mob_farm_input",
                    () -> new EjectorBlockItem(MOB_FARM_INPUT.get(), new Item.Properties()));

    public static final RegistryObject<CraftingGuardBlock> CRAFTING_GUARD_BLOCK =
            BLOCKS.register("crafting_guard", CraftingGuardBlock::new);

    public static final RegistryObject<BlockItem> CRAFTING_GUARD_BLOCK_ITEM =
            BLOCK_ITEMS.register("crafting_guard",
                    () -> new CraftingGuardBlockItem(CRAFTING_GUARD_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<CraftingSchedulerBlock> CRAFTING_SCHEDULER_BLOCK =
            BLOCKS.register("crafting_scheduler", CraftingSchedulerBlock::new);

    public static final RegistryObject<BlockItem> CRAFTING_SCHEDULER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crafting_scheduler",
                    () -> new CraftingSchedulerBlockItem(CRAFTING_SCHEDULER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<ReinforcedMatterCondenserBlock> REINFORCED_MATTER_CONDENSER_BLOCK =
            BLOCKS.register("reinforced_matter_condenser", ReinforcedMatterCondenserBlock::new);

    public static final RegistryObject<BlockItem> REINFORCED_MATTER_CONDENSER_BLOCK_ITEM =
            BLOCK_ITEMS.register("reinforced_matter_condenser",
                    () -> new ReinforcedMatterCondenserBlockItem(REINFORCED_MATTER_CONDENSER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<PenroseFrameBlock> PENROSE_FRAME =
            BLOCKS.register("penrose_frame", PenroseFrameBlock::new);

    public static final RegistryObject<BlockItem> PENROSE_FRAME_ITEM =
            BLOCK_ITEMS.register("penrose_frame",
                    () -> new PenroseFrameItem(PENROSE_FRAME.get(), new Item.Properties()));

    public static final RegistryObject<PenroseCoilBlock> PENROSE_COIL =
            BLOCKS.register("penrose_coil", PenroseCoilBlock::new);

    public static final RegistryObject<BlockItem> PENROSE_COIL_ITEM =
            BLOCK_ITEMS.register("penrose_coil",
                    () -> new PenroseCoilItem(PENROSE_COIL.get(), new Item.Properties()));

    public static final RegistryObject<PenroseControllerBlock> PENROSE_CONTROLLER =
            BLOCKS.register("penrose_controller", PenroseControllerBlock::new);

    public static final RegistryObject<BlockItem> PENROSE_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("penrose_controller",
                    () -> new PenroseCoilItem(PENROSE_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<CrazyPatternProviderBlock> CRAZY_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("crazy_pattern_provider", CrazyPatternProviderBlock::new);

    public static final RegistryObject<BlockItem> CRAZY_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crazy_pattern_provider",
                    () -> new CrazyPatternProviderBlockItem(CRAZY_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EnergyStorageController> ENERGY_STORAGE_CONTROLLER_BLOCK =
            BLOCKS.register("energy_storage_controller", EnergyStorageController::new);

    public static final RegistryObject<BlockItem> ENERGY_STORAGE_CONTROLLER_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_controller",
                    () -> new EnergyStorageControllerBlockItem(ENERGY_STORAGE_CONTROLLER_BLOCK.get(), new Item.Properties()));

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

    public static final RegistryObject<EnergyStorageFrame> ENERGY_STORAGE_FRAME_BLOCK =
            BLOCKS.register("energy_storage_frame", EnergyStorageFrame::new);

    public static final RegistryObject<BlockItem> ENERGY_STORAGE_FRAME_BLOCK_ITEM =
            BLOCK_ITEMS.register("energy_storage_frame",
                    () -> new EnergyStorageFrameBlockItem(ENERGY_STORAGE_FRAME_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<DenseEnergyStorage1k> DENSE_ENERGY_STORAGE_1K_BLOCK =
            BLOCKS.register("dense_energy_storage_1k", DenseEnergyStorage1k::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_1K_BLOCK_ITEM =
            BLOCK_ITEMS.register("dense_energy_storage_1k",
                    () -> new DenseEnergyStorage1kBlockItem(DENSE_ENERGY_STORAGE_1K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<DenseEnergyStorage4k> DENSE_ENERGY_STORAGE_4K_BLOCK =
            BLOCKS.register("dense_energy_storage_4k", DenseEnergyStorage4k::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_4K_BLOCK_ITEM =
            BLOCK_ITEMS.register("dense_energy_storage_4k",
                    () -> new DenseEnergyStorage4kBlockItem(DENSE_ENERGY_STORAGE_4K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<DenseEnergyStorage16k> DENSE_ENERGY_STORAGE_16K_BLOCK =
            BLOCKS.register("dense_energy_storage_16k", DenseEnergyStorage16k::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_16K_BLOCK_ITEM =
            BLOCK_ITEMS.register("dense_energy_storage_16k",
                    () -> new DenseEnergyStorage16kBlockItem(DENSE_ENERGY_STORAGE_16K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<DenseEnergyStorage64k> DENSE_ENERGY_STORAGE_64K_BLOCK =
            BLOCKS.register("dense_energy_storage_64k", DenseEnergyStorage64k::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_64K_BLOCK_ITEM =
            BLOCK_ITEMS.register("dense_energy_storage_64k",
                    () -> new DenseEnergyStorage64kBlockItem(DENSE_ENERGY_STORAGE_64K_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<DenseEnergyStorage256k> DENSE_ENERGY_STORAGE_256K_BLOCK =
            BLOCKS.register("dense_energy_storage_256k", DenseEnergyStorage256k::new);

    public static final RegistryObject<BlockItem> DENSE_ENERGY_STORAGE_256K_BLOCK_ITEM =
            BLOCK_ITEMS.register("dense_energy_storage_256k",
                    () -> new DenseEnergyStorage256kBlockItem(DENSE_ENERGY_STORAGE_256K_BLOCK.get(), new Item.Properties()));

    private CrazyBlockRegistrar() {}
}