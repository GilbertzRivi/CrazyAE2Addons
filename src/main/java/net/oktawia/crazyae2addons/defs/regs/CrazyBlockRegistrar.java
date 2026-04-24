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
import net.oktawia.crazyae2addons.entities.RecipeFabricatorBE;
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

    public static final RegistryObject<CrazyPatternProviderBlock> CRAZY_PATTERN_PROVIDER_BLOCK =
            BLOCKS.register("crazy_pattern_provider", CrazyPatternProviderBlock::new);

    public static final RegistryObject<BlockItem> CRAZY_PATTERN_PROVIDER_BLOCK_ITEM =
            BLOCK_ITEMS.register("crazy_pattern_provider",
                    () -> new CrazyPatternProviderBlockItem(CRAZY_PATTERN_PROVIDER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<EjectorBlock> EJECTOR_BLOCK =
            BLOCKS.register("ejector", EjectorBlock::new);

    public static final RegistryObject<BlockItem> EJECTOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("ejector",
                    () -> new AEBaseBlockItem(EJECTOR_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<RecipeFabricatorBlock> RECIPE_FABRICATOR_BLOCK =
            BLOCKS.register("recipe_fabricator", RecipeFabricatorBlock::new);

    public static final RegistryObject<BlockItem> RECIPE_FABRICATOR_BLOCK_ITEM =
            BLOCK_ITEMS.register("recipe_fabricator",
                    () -> new AEBaseBlockItem(RECIPE_FABRICATOR_BLOCK.get(), new Item.Properties()));

    private CrazyBlockRegistrar() {}
}
