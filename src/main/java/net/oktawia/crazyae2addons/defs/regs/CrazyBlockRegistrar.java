package net.oktawia.crazyae2addons.defs.regs;

import appeng.block.networking.EnergyCellBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.blocks.*;
import net.oktawia.crazyae2addons.blocks.PenroseControllerBlock;
import net.oktawia.crazyae2addons.items.CrazyPatternProviderBlockItem;

import java.util.ArrayList;
import java.util.List;

public class CrazyBlockRegistrar {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CrazyAddons.MODID);

    public static final DeferredRegister.Items BLOCK_ITEMS =
            DeferredRegister.createItems(CrazyAddons.MODID);

    public static List<Block> getBlocks() {
        return BLOCKS.getEntries()
                .stream()
                .map(DeferredHolder::get)
                .map(x -> (Block) x)
                .toList();
    }

    public static final DeferredBlock<CrazyPatternProviderBlock> CRAZY_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("crazy_pattern_provider", CrazyPatternProviderBlock::new);

    public static final DeferredItem<CrazyPatternProviderBlockItem> CRAZY_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crazy_pattern_provider",
                    () -> new CrazyPatternProviderBlockItem(CRAZY_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<BrokenPatternProviderBlock> BROKEN_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("broken_pattern_provider", BrokenPatternProviderBlock::new);

    public static final DeferredItem<BlockItem> BROKEN_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("broken_pattern_provider",
                    () -> new BlockItem(BROKEN_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<CraftingSchedulerBlock> CRAFTING_SCHEDULER_BLOCK =
            BLOCKS.register("crafting_scheduler", CraftingSchedulerBlock::new);

    public static final DeferredItem<BlockItem> CRAFTING_SCHEDULER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crafting_scheduler",
                    () -> new BlockItem(CRAFTING_SCHEDULER_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_1K =
            BLOCKS.register("energy_storage_1k", () -> new EnergyStorageBlock(1024L * 1024 * 8));

    public static final DeferredItem<Item> ENERGY_STORAGE_1K_ITEM =
            BLOCK_ITEMS.register("energy_storage_1k",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_1K.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_4K =
            BLOCKS.register("energy_storage_4k", () -> new EnergyStorageBlock(1024L * 1024 * 32));

    public static final DeferredItem<Item> ENERGY_STORAGE_4K_ITEM =
            BLOCK_ITEMS.register("energy_storage_4k",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_4K.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_16K =
            BLOCKS.register("energy_storage_16k", () -> new EnergyStorageBlock(1024L * 1024 * 128));

    public static final DeferredItem<Item> ENERGY_STORAGE_16K_ITEM =
            BLOCK_ITEMS.register("energy_storage_16k",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_16K.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_64K =
            BLOCKS.register("energy_storage_64k", () -> new EnergyStorageBlock(1024L * 1024 * 512));

    public static final DeferredItem<Item> ENERGY_STORAGE_64K_ITEM =
            BLOCK_ITEMS.register("energy_storage_64k",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_64K.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_256K =
            BLOCKS.register("energy_storage_256k", () -> new EnergyStorageBlock(1024L * 1024 * 8 * 256));

    public static final DeferredItem<Item> ENERGY_STORAGE_256K_ITEM =
            BLOCK_ITEMS.register("energy_storage_256k",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_256K.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_1M =
            BLOCKS.register("energy_storage_1m", () -> new EnergyStorageBlock(1024L * 1024 * 1024 * 8));

    public static final DeferredItem<Item> ENERGY_STORAGE_1M_ITEM =
            BLOCK_ITEMS.register("energy_storage_1m",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_1M.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_4M =
            BLOCKS.register("energy_storage_4m", () -> new EnergyStorageBlock(1024L * 1024 * 1024 * 32));

    public static final DeferredItem<Item> ENERGY_STORAGE_4M_ITEM =
            BLOCK_ITEMS.register("energy_storage_4m",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_4M.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_16M =
            BLOCKS.register("energy_storage_16m", () -> new EnergyStorageBlock(1024L * 1024 * 1024 * 128));

    public static final DeferredItem<Item> ENERGY_STORAGE_16M_ITEM =
            BLOCK_ITEMS.register("energy_storage_16m",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_16M.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_64M =
            BLOCKS.register("energy_storage_64m", () -> new EnergyStorageBlock(1024L * 1024 * 1024 * 512));

    public static final DeferredItem<Item> ENERGY_STORAGE_64M_ITEM =
            BLOCK_ITEMS.register("energy_storage_64m",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_64M.get(), new Item.Properties()));

    public static final DeferredBlock<EnergyStorageBlock> ENERGY_STORAGE_256M =
            BLOCKS.register("energy_storage_256m", () -> new EnergyStorageBlock(1024L * 1024 * 1024 * 8 * 256));

    public static final DeferredItem<Item> ENERGY_STORAGE_256M_ITEM =
            BLOCK_ITEMS.register("energy_storage_256m",
                    () -> new EnergyCellBlockItem(ENERGY_STORAGE_256M.get(), new Item.Properties()));

    public static final DeferredBlock<AutoBuilderCreativeSupplyBlock> AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK =
            BLOCKS.register("auto_builder_creative_supply", AutoBuilderCreativeSupplyBlock::new);

    public static final DeferredItem<BlockItem> AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK_ITEM =
            BLOCK_ITEMS.register("auto_builder_creative_supply",
                    () -> new BlockItem(AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<AutoBuilderBlock> AUTO_BUILDER_BLOCK =
            BLOCKS.register("auto_builder", AutoBuilderBlock::new);

    public static final DeferredItem<BlockItem> AUTO_BUILDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("auto_builder",
                    () -> new BlockItem(AUTO_BUILDER_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<AmpereMeterBlock> AMPERE_METER_BLOCK =
            BLOCKS.register("ampere_meter", AmpereMeterBlock::new);

    public static final DeferredItem<BlockItem> AMPERE_METER_BLOCK_ITEM =
            BLOCK_ITEMS.register("ampere_meter",
                    () -> new BlockItem(AMPERE_METER_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<EjectorBlock> EJECTOR_BLOCK =
            BLOCKS.register("ejector", EjectorBlock::new);

    public static final DeferredItem<BlockItem> EJECTOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("ejector",
                    () -> new appeng.block.AEBaseBlockItem(EJECTOR_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<AutoEnchanterBlock> AUTO_ENCHANTER_BLOCK =
            BLOCKS.register("auto_enchanter", AutoEnchanterBlock::new);

    public static final DeferredItem<BlockItem> AUTO_ENCHANTER_BLOCK_ITEM =
            BLOCK_ITEMS.register("auto_enchanter",
                    () -> new BlockItem(AUTO_ENCHANTER_BLOCK.get(), new Item.Properties()));

    public static final DeferredBlock<PenroseControllerBlock> PENROSE_CONTROLLER =
            BLOCKS.register("penrose_controller", PenroseControllerBlock::new);

    public static final DeferredItem<BlockItem> PENROSE_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("penrose_controller",
                    () -> new BlockItem(PENROSE_CONTROLLER.get(), new Item.Properties()));

    public static final DeferredBlock<TestMultiblockControllerBlock> TEST_MULTIBLOCK_CONTROLLER =
            BLOCKS.register("test_multiblock_controller", TestMultiblockControllerBlock::new);

    public static final DeferredItem<BlockItem> TEST_MULTIBLOCK_CONTROLLER_ITEM =
            BLOCK_ITEMS.register("test_multiblock_controller",
                    () -> new BlockItem(TEST_MULTIBLOCK_CONTROLLER.get(), new Item.Properties()));

    public static final DeferredBlock<TestMultiblockFrameBlock> TEST_MULTIBLOCK_FRAME =
            BLOCKS.register("test_multiblock_frame", TestMultiblockFrameBlock::new);

    public static final DeferredItem<BlockItem> TEST_MULTIBLOCK_FRAME_ITEM =
            BLOCK_ITEMS.register("test_multiblock_frame",
                    () -> new BlockItem(TEST_MULTIBLOCK_FRAME.get(), new Item.Properties()));

    private CrazyBlockRegistrar() {}
}
