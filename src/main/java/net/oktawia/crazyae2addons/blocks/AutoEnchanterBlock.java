package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.Apotheosis.ApothAutoEnchanterBE;
import net.oktawia.crazyae2addons.entities.AutoEnchanterBE;
import net.oktawia.crazyae2addons.util.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.Nullable;

public class AutoEnchanterBlock extends AbstractMenuOpeningBlock<AutoEnchanterBE> {

    public AutoEnchanterBlock() {
        super(AEBaseBlock.metalProps());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return IsModLoaded.APOTH_ENCHANTING ? new ApothAutoEnchanterBE(pos, state) : new AutoEnchanterBE(pos, state);
    }
}