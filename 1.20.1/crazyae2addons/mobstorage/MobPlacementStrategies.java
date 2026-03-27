package net.oktawia.crazyae2addons.mobstorage;

import appeng.api.behaviors.PlacementStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;

public class MobPlacementStrategies implements PlacementStrategy {

    private final ServerLevel level;
    private final BlockPos pos;
    private final Direction side;

    public MobPlacementStrategies(ServerLevel level, BlockPos pos, Direction side) {
        this.level = level;
        this.pos = pos;
        this.side = side;
    }

    @Override
    public void clearBlocked() {}

    public boolean canSpawn() {
        BlockState here  = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        return here.isAir() && above.isAir();
    }

    @Override
    public long placeInWorld(AEKey what, long amount, Actionable type, boolean placeAsEntity) {
        if (!(what instanceof MobKey mk)) return 0;
        if (!canSpawn()) return 0;
        if (amount > 24) amount = 24;
        if (type == Actionable.MODULATE){
            for (int i = 0; i < amount; i++){
                mk.getEntityType().spawn(level, pos, MobSpawnType.COMMAND);
            }
            level.sendParticles(
                    ParticleTypes.FIREWORK,
                    pos.getX(),
                    pos.getY() + 1,
                    pos.getZ(),
                    20,
                    0.5,
                    0.5,
                    0.5,
                    0.01
            );
        }
        return amount;
    }
}
