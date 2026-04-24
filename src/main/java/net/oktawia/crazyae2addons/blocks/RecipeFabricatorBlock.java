package net.oktawia.crazyae2addons.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.oktawia.crazyae2addons.entities.RecipeFabricatorBE;
import net.oktawia.crazyae2addons.util.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.Nullable;

public class RecipeFabricatorBlock extends AbstractMenuOpeningBlock<RecipeFabricatorBE> {

    public RecipeFabricatorBlock() {
        super(Properties.of().strength(2f).mapColor(MapColor.METAL).sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecipeFabricatorBE(pos, state);
    }
}