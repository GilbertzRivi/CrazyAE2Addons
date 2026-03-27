package net.oktawia.crazyae2addons.entities;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;

import java.util.List;

public class SuperSingularityBlockBE extends AENetworkBlockEntity implements IGridTickable {

    public SuperSingularityBlockBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.SUPER_SINGULARITY_BLOCK_BE.get(), pos, blockState);
        getMainNode()
                .addService(IGridTickable.class, this)
                .setIdlePowerUsage(8192)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.SUPER_SINGULARITY_BLOCK.get().asItem())
                );
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        Level world = getLevel();
        BlockPos pos = getBlockPos();

        List<Player> players = world.getEntitiesOfClass(Player.class,
                new AABB(pos).inflate(2),
                player -> player.isAlive() && !player.isCreative());

        DamageSource source = world.damageSources().generic();
        for (Player player : players) {
            player.hurt(source, 5);
        }
        return TickRateModulation.IDLE;
    }
}
