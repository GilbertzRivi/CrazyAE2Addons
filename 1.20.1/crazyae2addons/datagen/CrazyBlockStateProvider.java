package net.oktawia.crazyae2addons.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;

public class CrazyBlockStateProvider extends BlockStateProvider {
    public CrazyBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, CrazyAddons.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (var block : CrazyBlockRegistrar.getBlocks()){
            if (block != CrazyBlockRegistrar.AMPERE_METER_BLOCK.get()
                    && block != CrazyBlockRegistrar.SPAWNER_EXTRACTOR_CONTROLLER.get()
                    && block != CrazyBlockRegistrar.SPAWNER_EXTRACTOR_WALL.get()
                    && block != CrazyBlockRegistrar.MOB_FARM_CONTROLLER.get()
                    && block != CrazyBlockRegistrar.MOB_FARM_WALL.get()
                    && block != CrazyBlockRegistrar.PENROSE_CONTROLLER.get()
                    && block != CrazyBlockRegistrar.PENROSE_FRAME.get()
                    && block != CrazyBlockRegistrar.PENROSE_COIL.get()
                    && block != CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get()
                    && block != CrazyBlockRegistrar.BROKEN_PATTERN_PROVIDER_BLOCK.get()
                    && block != CrazyBlockRegistrar.ENTROPY_CRADLE.get()
                    && block != CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER.get()
                    && block != CrazyBlockRegistrar.ENTROPY_CRADLE_CAPACITOR.get()
                    && block != CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get()
                    && block != CrazyBlockRegistrar.RESEARCH_STATION.get()
                    && block != CrazyBlockRegistrar.RESEARCH_PEDESTAL_BOTTOM.get()
                    && block != CrazyBlockRegistrar.RESEARCH_CABLE_BLOCK.get()
                    && block != CrazyBlockRegistrar.RESEARCH_UNIT_FRAME.get()
                    && block != CrazyBlockRegistrar.RESEARCH_UNIT.get()
                    && block != CrazyBlockRegistrar.EJECTOR_BLOCK.get()){
                simpleBlockWithItem(block);
            }
        }
    }

    private void simpleBlockWithItem(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
