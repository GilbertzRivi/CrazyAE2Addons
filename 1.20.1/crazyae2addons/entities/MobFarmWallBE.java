package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.grid.AENetworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;

public class MobFarmWallBE extends AENetworkBlockEntity {

    public MobFarmControllerBE controller;

    public MobFarmWallBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.MOB_FARM_WALL_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.MOB_FARM_WALL.get().asItem())
                );
    }

    public void setController(MobFarmControllerBE mobFarmControllerBE) {
        this.controller = mobFarmControllerBE;
    }
}
