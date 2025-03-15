package net.oktawia.crazyae2addons.defs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import appeng.core.definitions.BlockDefinition;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.oktawia.crazyae2addons.items.CraftingCancelerBlockItem;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.blocks.CraftingCancelerBlock;

public class Blocks {

    private static final List<BlockDefinition<?>> BLOCKS = new ArrayList<>();
    public static final BlockDefinition<CraftingCancelerBlock> CRAFTING_CANCELER_BLOCK = block(
            "Crafting Canceler",
            "crafting_canceler_block",
            CraftingCancelerBlock::new,
            CraftingCancelerBlockItem::new
    );

    public static List<BlockDefinition<?>> getBlocks() {
        return Collections.unmodifiableList(BLOCKS);
    }

    public static <T extends Block> BlockDefinition<T> block(
            String englishName,
            String id,
            Supplier<T> blockSupplier,
            BiFunction<Block, Item.Properties, BlockItem> itemFactory) {
        var block = blockSupplier.get();
        var item = itemFactory.apply(block, new Item.Properties());

        var definition = new BlockDefinition<>(englishName, CrazyAddons.makeId(id), block, item);
        BLOCKS.add(definition);
        return definition;
    }
}