package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.Apotheosis.ApothAutoEnchanterBE;
import net.oktawia.crazyae2addons.entities.AutoEnchanterBE;
import net.oktawia.crazyae2addons.logic.AbstractMenuOpeningBlock;
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
