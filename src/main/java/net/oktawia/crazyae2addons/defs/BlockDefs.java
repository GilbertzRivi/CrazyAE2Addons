package net.oktawia.crazyae2addons.defs;

import java.util.*;
import java.util.function.Supplier;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.core.definitions.AEParts;
import appeng.core.definitions.BlockDefinition;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.blocks.*;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;

public class BlockDefs {

    private static final Map<Block, Map.Entry<String, Map<String, Item>>> BLOCK_RECIPES = new HashMap<>();

    public static Map<Block, Map.Entry<String, Map<String, Item>>> getBlockRecipes() {
        return BLOCK_RECIPES;
    }

    public static void block(
            Block block,
            String recipe,
            Supplier<Map<String, Item>> recipe_map) {
        BLOCK_RECIPES.put(block, Map.entry(recipe, recipe_map.get()));
    }

    public static void registerRecipes(){
        block(
                CrazyBlockRegistrar.AMPERE_METER_BLOCK.get(),
                "ICE",
                () -> Map.of(
                        "I", AEParts.IMPORT_BUS.asItem(),
                        "C", AEBlocks.INTERFACE.asItem(),
                        "E", AEParts.EXPORT_BUS.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.IMPULSED_PATTERN_PROVIDER_BLOCK.get(),
                "PDR",
                () -> Map.of(
                        "P", AEBlocks.PATTERN_PROVIDER.asItem(),
                        "D", Items.DIAMOND,
                        "R", Items.REDSTONE_TORCH
                )
        );

        block(
                CrazyBlockRegistrar.SIGNALLING_INTERFACE_BLOCK.get(),
                "IT",
                () -> Map.of(
                        "I", AEBlocks.INTERFACE.asItem(),
                        "T", Items.REDSTONE.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.MOB_FARM_WALL.get(),
                "BIB/IRI/BIB",
                () -> Map.of(
                        "I", Blocks.IRON_BLOCK.asItem(),
                        "B", Blocks.IRON_BARS.asItem(),
                        "R", Items.ROTTEN_FLESH
                )
        );

        block(
                CrazyBlockRegistrar.MOB_FARM_INPUT.get(),
                "WWW/WEW/WWW",
                () -> Map.of(
                        "W", CrazyBlockRegistrar.MOB_FARM_WALL.get().asItem(),
                        "E", CrazyItemRegistrar.MOB_EXPORT_BUS.get()
                )
        );

        block(
                CrazyBlockRegistrar.MOB_FARM_COLLECTOR.get(),
                "WHW/HEH/WHW",
                () -> Map.of(
                        "W", CrazyBlockRegistrar.MOB_FARM_WALL.get().asItem(),
                        "H", AEParts.IMPORT_BUS.asItem(),
                        "E", AEItems.FLUIX_PEARL.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.MOB_FARM_DAMAGE.get(),
                "DND/NEN/DND",
                () -> Map.of(
                        "D", AEBlocks.DENSE_ENERGY_CELL.asItem(),
                        "N", Items.NETHERITE_INGOT,
                        "E", Items.ECHO_SHARD
                )
        );

        block(
                CrazyBlockRegistrar.SPAWNER_EXTRACTOR_WALL.get(),
                "WEW/ESE/WEW",
                () -> Map.of(
                        "W", CrazyBlockRegistrar.MOB_FARM_WALL.get().asItem(),
                        "E", Items.BLAZE_ROD,
                        "S", AEItems.FLUIX_PEARL.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.SPAWNER_EXTRACTOR_CONTROLLER.get(),
                "WE",
                () -> Map.of(
                        "W", CrazyBlockRegistrar.SPAWNER_EXTRACTOR_WALL.get().asItem(),
                        "E", Items.NETHER_STAR
                )
        );

        block(
                CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get(),
                "WE",
                () -> Map.of(
                        "W", CrazyBlockRegistrar.MOB_FARM_WALL.get().asItem(),
                        "E", Items.NETHER_STAR
                )
        );

        block(
                CrazyBlockRegistrar.CRAFTING_SCHEDULER_BLOCK.get(),
                "PRE",
                () -> Map.of(
                        "P", AEBlocks.PATTERN_PROVIDER.asItem(),
                        "R", Items.REDSTONE,
                        "E", AEParts.LEVEL_EMITTER.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.REINFORCED_MATTER_CONDENSER_BLOCK.get(),
                "IPI/GMG/ICI",
                () -> Map.of(
                        "I", Items.IRON_INGOT,
                        "P", Blocks.IRON_BLOCK.asItem(),
                        "G", AEBlocks.QUARTZ_GLASS.asItem(),
                        "M", AEBlocks.CONDENSER.asItem(),
                        "C", AEItems.CELL_COMPONENT_256K.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.PENROSE_COIL.get(),
                "AAA/ADA/AAA",
                () -> Map.of(
                        "A", Blocks.COPPER_BLOCK.asItem(),
                        "D", CrazyBlockRegistrar.PENROSE_FRAME.get().asItem()
                )
        );

        block(
                CrazyBlockRegistrar.PENROSE_CONTROLLER.get(),
                "AAA/ANA/AAA",
                () -> Map.of(
                        "A", CrazyBlockRegistrar.PENROSE_FRAME.get().asItem(),
                        "N", Items.NETHER_STAR
                )
        );

        block(
                CrazyBlockRegistrar.ENERGY_STORAGE_FRAME_BLOCK.get(),
                "FMF/MEM/FMF",
                () -> Map.of(
                        "F", AEItems.FLUIX_CRYSTAL.asItem(),
                        "M", AEItems.CERTUS_QUARTZ_CRYSTAL_CHARGED.asItem(),
                        "E", AEBlocks.ENERGY_CELL.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.ENERGY_STORAGE_CONTROLLER_BLOCK.get(),
                "FFF/FNF/FFF",
                () -> Map.of(
                        "F", CrazyBlockRegistrar.ENERGY_STORAGE_FRAME_BLOCK.get().asItem(),
                        "N", Items.NETHER_STAR
                )
        );

        block(
                CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(),
                "EPE/BRN/EPE",
                () -> Map.of(
                        "E", Items.EMERALD.asItem(),
                        "P", CrazyItemRegistrar.BUILDER_PATTERN.get().asItem(),
                        "B", AEParts.IMPORT_BUS.asItem(),
                        "R", AEBlocks.PATTERN_PROVIDER.asItem(),
                        "N", AEParts.EXPORT_BUS.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.PENROSE_PORT.get(),
                " E /IPI/ E ",
                () -> Map.of(
                        "E", CrazyItemRegistrar.ENERGY_EXPORTER_PART_ITEM.get().asItem(),
                        "I", AEBlocks.INTERFACE.asItem(),
                        "P", CrazyBlockRegistrar.PENROSE_FRAME.get().asItem()
                )
        );

        block(
                CrazyBlockRegistrar.ENERGY_STORAGE_PORT_BLOCK.get(),
                " E /IPI/ E ",
                () -> Map.of(
                        "E", CrazyItemRegistrar.ENERGY_EXPORTER_PART_ITEM.get().asItem(),
                        "I", AEBlocks.INTERFACE.asItem(),
                        "P", CrazyBlockRegistrar.ENERGY_STORAGE_FRAME_BLOCK.get().asItem()
                )
        );

        block(
                CrazyBlockRegistrar.ENTROPY_CRADLE.get(),
                "BQB/QBQ/BQB",
                () -> Map.of(
                        "B", Blocks.OBSIDIAN.asItem(),
                        "Q", Blocks.QUARTZ_BLOCK.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER.get(),
                " F /FDF/ F ",
                () -> Map.of(
                        "F", CrazyBlockRegistrar.ENTROPY_CRADLE.get().asItem(),
                        "D", Items.DIAMOND.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.ENTROPY_CRADLE_CAPACITOR.get(),
                " F /FRF/ F ",
                () -> Map.of(
                        "F", CrazyBlockRegistrar.ENTROPY_CRADLE.get().asItem(),
                        "R", Blocks.REDSTONE_BLOCK.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.RESEARCH_STATION.get(),
                " C /IKO/ E ",
                () -> Map.of(
                        "C", Items.CLOCK.asItem(),
                        "I", AEParts.IMPORT_BUS.asItem(),
                        "K", AEBlocks.CONTROLLER.asItem(),
                        "O", AEParts.EXPORT_BUS.asItem(),
                        "E", AEBlocks.ENERGY_ACCEPTOR.asItem()
                )
        );

        block(
                CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get(),
                "CR",
                () -> Map.of(
                        "C", Blocks.CRAFTING_TABLE.asItem(),
                        "R", CrazyBlockRegistrar.RESEARCH_STATION.get().asItem()
                )
        );
    }

}