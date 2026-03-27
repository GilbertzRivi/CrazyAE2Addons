package net.oktawia.crazyae2addons.entities;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.mojang.datafixers.Products;
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

public class EntropyCradleBE extends AENetworkBlockEntity {

    private EntropyCradleControllerBE controller;

    public EntropyCradleBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.ENTROPY_CRADLE_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE.get().asItem())
                );
    }

    public void setController(EntropyCradleControllerBE controller) {
        this.controller = controller;
    }
}
