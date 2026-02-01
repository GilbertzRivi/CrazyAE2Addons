package net.oktawia.crazyae2addons.entities;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.ticking.IGridTickable;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import org.jetbrains.annotations.NotNull;

public class ResearchUnitFrameBE extends AENetworkBlockEntity {

    public ResearchUnitBE controller;

    public ResearchUnitFrameBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.RESEARCH_UNIT_FRAME_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(1.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.RESEARCH_UNIT_FRAME.get().asItem())
                );
    }

    public void setController(ResearchUnitBE controller) {
        this.controller = controller;
        if (getMainNode().getNode() != null) {
            if (this.controller != null) {
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
