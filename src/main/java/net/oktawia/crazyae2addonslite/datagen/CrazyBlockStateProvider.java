package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;

public class CrazyBlockStateProvider extends BlockStateProvider {

    public CrazyBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, CrazyAddonslite.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (var block : CrazyBlockRegistrar.getBlocks()) {
            if (
//                    block != CrazyBlockRegistrar.AMPERE_METER_BLOCK.get() &&
                      block != CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get()
//                    && block != CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK.get()
//                    && block != CrazyBlockRegistrar.EJECTOR_BLOCK.get()
            ) {
                simpleBlockWithItem(block);
            }
        }
    }

    private void simpleBlockWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
