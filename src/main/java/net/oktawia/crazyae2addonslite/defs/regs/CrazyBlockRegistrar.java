package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.block.AEBaseBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.blocks.*;
import net.oktawia.crazyae2addonslite.items.*;

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

    public static final RegistryObject<AmpereMeterBlock> AMPERE_METER_BLOCK =
            BLOCKS.register("ampere_meter", AmpereMeterBlock::new);

    public static final RegistryObject<BlockItem> AMPERE_METER_BLOCK_ITEM =
            BLOCK_ITEMS.register("ampere_meter",
                    () -> new AEBaseBlockItem(AMPERE_METER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<CraftingSchedulerBlock> CRAFTING_SCHEDULER_BLOCK =
            BLOCKS.register("crafting_scheduler", CraftingSchedulerBlock::new);

    public static final RegistryObject<BlockItem> CRAFTING_SCHEDULER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crafting_scheduler",
                    () -> new AEBaseBlockItem(CRAFTING_SCHEDULER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<CrazyPatternProviderBlock> CRAZY_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("crazy_pattern_provider", CrazyPatternProviderBlock::new);

    public static final RegistryObject<BlockItem> CRAZY_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crazy_pattern_provider",
                    () -> new CrazyPatternProviderBlockItem(CRAZY_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BrokenPatternProviderBlock> BROKEN_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("broken_pattern_provider", BrokenPatternProviderBlock::new);

    public static final RegistryObject<BlockItem> BROKEN_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("broken_pattern_provider",
                    () -> new AEBaseBlockItem(BROKEN_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EjectorBlock> EJECTOR_BLOCK =
            BLOCKS.register("ejector", EjectorBlock::new);

    public static final RegistryObject<BlockItem> EJECTOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("ejector",
                    () -> new AEBaseBlockItem(EJECTOR_BLOCK.get(), new Item.Properties()));

    private CrazyBlockRegistrar() {}
}
