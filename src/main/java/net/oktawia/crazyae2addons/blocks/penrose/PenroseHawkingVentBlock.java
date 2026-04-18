package net.oktawia.crazyae2addons.blocks.penrose;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.oktawia.crazyae2addons.entities.penrose.PenroseHawkingVentBE;
import net.oktawia.crazyae2addons.logic.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PenroseHawkingVentBlock extends AbstractMenuOpeningBlock<PenroseHawkingVentBE> {

    public PenroseHawkingVentBlock() {
        super(AEBaseBlock.metalProps());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PenroseHawkingVentBE(pos, state);
    }

    @Override
    public boolean canConnectRedstone(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @Nullable Direction side) {
        return true;
    }
}