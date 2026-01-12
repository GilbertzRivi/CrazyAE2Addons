package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class CrazyItemTagGeneratorr extends ItemTagsProvider {
    public CrazyItemTagGeneratorr(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, CompletableFuture<TagLookup<Block>> pBlockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(pOutput, pLookupProvider, pBlockTags, CrazyAddons.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {

    }
}