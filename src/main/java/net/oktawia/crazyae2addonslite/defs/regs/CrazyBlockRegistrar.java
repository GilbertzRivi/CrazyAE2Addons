package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.block.AEBaseBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.blocks.CrazyPatternProviderBlock;

import java.util.List;

public class CrazyBlockRegistrar {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CrazyAddonslite.MODID);

    public static final DeferredRegister.Items BLOCK_ITEMS =
            DeferredRegister.createItems(CrazyAddonslite.MODID);

    public static List<Block> getBlocks() {
        return BLOCKS.getEntries()
                .stream()
                .map(DeferredHolder::get)
                .map(x -> (Block) x)
                .toList();
    }

    public static final DeferredBlock<CrazyPatternProviderBlock> CRAZY_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("crazy_pattern_provider", CrazyPatternProviderBlock::new);

    public static final DeferredItem<BlockItem> CRAZY_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crazy_pattern_provider",
                    () -> new AEBaseBlockItem(CRAZY_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

//    public static final DeferredBlock<AmpereMeterBlock> AMPERE_METER_BLOCK =
//            BLOCKS.register("ampere_meter", AmpereMeterBlock::new);
//
//    public static final DeferredItem<BlockItem> AMPERE_METER_BLOCK_ITEM =
//            BLOCK_ITEMS.register("ampere_meter",
//                    () -> new AEBaseBlockItem(AMPERE_METER_BLOCK.get(), new Item.Properties()));
//
//    public static final DeferredBlock<CraftingSchedulerBlock> CRAFTING_SCHEDULER_BLOCK =
//            BLOCKS.register("crafting_scheduler", CraftingSchedulerBlock::new);
//
//    public static final DeferredItem<BlockItem> CRAFTING_SCHEDULER_BLOCK_ITEM =
//            BLOCK_ITEMS.register("crafting_scheduler",
//                    () -> new AEBaseBlockItem(CRAFTING_SCHEDULER_BLOCK.get(), new Item.Properties()));
//
//    public static final DeferredBlock<BrokenPatternProviderBlock> BROKEN_PATTERN_PROVIDER_BLOCK =
//            BLOCKS.register("broken_pattern_provider", BrokenPatternProviderBlock::new);
//
//    public static final DeferredItem<BlockItem> BROKEN_PATTERN_PROVIDER_BLOCK_ITEM =
//            BLOCK_ITEMS.register("broken_pattern_provider",
//                    () -> new AEBaseBlockItem(BROKEN_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));
//
//    public static final DeferredBlock<EjectorBlock> EJECTOR_BLOCK =
//            BLOCKS.register("ejector", EjectorBlock::new);
//
//    public static final DeferredItem<BlockItem> EJECTOR_BLOCK_ITEM =
//            BLOCK_ITEMS.register("ejector",
//                    () -> new AEBaseBlockItem(EJECTOR_BLOCK.get(), new Item.Properties()));

    private CrazyBlockRegistrar() {}
}
