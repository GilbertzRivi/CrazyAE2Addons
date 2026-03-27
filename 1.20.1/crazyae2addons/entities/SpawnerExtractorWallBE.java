package net.oktawia.crazyae2addons.entities;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.blockentity.grid.AENetworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;

public class SpawnerExtractorWallBE extends AENetworkBlockEntity {

    public SpawnerExtractorControllerBE controller;

    public SpawnerExtractorWallBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.SPAWNER_EXTRACTOR_WALL_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.SPAWNER_EXTRACTOR_WALL.get().asItem())
                );
    }

    public void setController(SpawnerExtractorControllerBE spawnerExtractorControllerBE) {
        this.controller = spawnerExtractorControllerBE;
        if (getMainNode().getNode() != null){
            if (this.controller != null && this.controller.getMainNode().getNode() != null){
                if (getMainNode().getNode().getConnections().stream()
                        .noneMatch(x -> (x.a() == this.controller.getMainNode().getNode() || x.b() == this.controller.getMainNode().getNode()))){
                    GridHelper.createConnection(getMainNode().getNode(), this.controller.getMainNode().getNode());
                }
            } else {
                getMainNode().getNode().getConnections().stream()
                        .filter(x -> (!x.isInWorld()))
                        .forEach(IGridConnection::destroy);
            }
        }
    }
}
