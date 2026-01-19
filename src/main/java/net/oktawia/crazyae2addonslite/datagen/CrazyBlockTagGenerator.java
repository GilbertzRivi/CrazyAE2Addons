package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class CrazyBlockTagGenerator extends BlockTagsProvider {

    public CrazyBlockTagGenerator(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, CrazyAddonslite.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        for (var block : CrazyBlockRegistrar.getBlocks()) {
            this.tag(BlockTags.NEEDS_IRON_TOOL).add(block);
            this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
        }
    }
}
