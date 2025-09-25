package net.oktawia.crazyae2addons.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;

public class ResearchFluidType extends WaterBasedFluidType {
    public ResearchFluidType(FluidType.Properties props) {
        super(props);
        this.tintColor = 0xFF47C7FF;
    }

    @Override
    public boolean canConvertToSource(@NotNull FluidState state, @NotNull LevelReader reader, @NotNull BlockPos pos) {
        return false;
    }
}
