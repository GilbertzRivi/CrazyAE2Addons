package net.oktawia.crazyae2addonslite.defs;

import java.util.*;
import java.util.function.Supplier;

import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;

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
                CrazyBlockRegistrar.CRAFTING_SCHEDULER_BLOCK.get(),
                "PRE",
                () -> Map.of(
                        "P", AEBlocks.PATTERN_PROVIDER.asItem(),
                        "R", Items.REDSTONE,
                        "E", AEParts.LEVEL_EMITTER.asItem()
                )
        );
    }
}