package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.oktawia.crazyae2addons.entities.PenroseCoilBE;
import net.oktawia.crazyae2addons.entities.PenroseFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenroseCoilBlock extends AEBaseEntityBlock<PenroseCoilBE> {

    public PenroseCoilBlock() {
        super(Properties.of().strength(2f).mapColor(MapColor.METAL).sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PenroseCoilBE(pos, state);
    }
}
