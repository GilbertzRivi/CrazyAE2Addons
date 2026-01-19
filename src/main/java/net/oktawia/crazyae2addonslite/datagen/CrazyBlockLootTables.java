package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CrazyBlockLootTables extends BlockLootSubProvider {

    public CrazyBlockLootTables(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (var block : CrazyBlockRegistrar.getBlocks()) {
            this.dropSelf(block);
        }
    }

    @Override
    protected @NotNull Iterable<Block> getKnownBlocks() {
        return CrazyBlockRegistrar.getBlocks().stream()::iterator;
    }
}
