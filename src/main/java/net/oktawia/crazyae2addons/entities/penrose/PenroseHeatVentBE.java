package net.oktawia.crazyae2addons.entities.penrose;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseHeatVentMenu;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenroseHeatVentBE extends AbstractMultiblockFrameBE<PenroseControllerBE>
        implements MenuProvider, IMenuOpeningBlockEntity {

    private static final double HEAT_PER_MU = 1.0;
    private static final double REF_FE = 16.0;

    @Getter
    @Persisted
    @DescSynced
    private double desiredCooling = 0.0;

    @Persisted
    @DescSynced
    private long bufferEnergy = 0L;

    @Getter
    private final IEnergyStorage energyStorage = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            long space = (long) Integer.MAX_VALUE - bufferEnergy;
            int received = (int) Math.min(maxReceive, space);
            if (!simulate) {
                bufferEnergy += received;
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = (int) Math.min(maxExtract, bufferEnergy);
            if (!simulate) {
                bufferEnergy -= extracted;
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(bufferEnergy, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public PenroseHeatVentBE(BlockPos pos, BlockState blockState) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_HEAT_VENT_BE.get(),
                pos,
                blockState,
                new ItemStack(CrazyBlockRegistrar.PENROSE_HEAT_VENT.get().asItem()),
                1.0F
        );
    }

    @Override
    protected Class<PenroseControllerBE> controllerClass() {
        return PenroseControllerBE.class;
    }

    @Override
    public void setController(@Nullable BlockEntity controller) {
        unregisterFromController();
        super.setController(controller);
    }

    @Override
    protected void onControllerChanged(@Nullable PenroseControllerBE newController) {
        if (newController != null) {
            newController.registerHeatVent(this);
            connectToControllerGrid();
        } else {
            disconnectFromControllerGrid();
        }
    }

    @Override
    public void setRemoved() {
        unregisterFromController();
        super.setRemoved();
    }

    private void unregisterFromController() {
        if (activeController != null) {
            activeController.unregisterHeatVent(this);
        }
    }

    public void setDesiredCooling(double desiredCooling) {
        this.desiredCooling = Math.max(0.0, desiredCooling);
        setChanged();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PenroseHeatVentMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_HEAT_VENT_MENU.get(), player, locator);
    }

    public static long computeCostForCooling(double cooling) {
        if (cooling <= 0.0) {
            return 0L;
        }

        double fEq = cooling / HEAT_PER_MU;
        double exponent = 2.0 * ((fEq / REF_FE) - 1.0);
        double pCost = (double) (1L << 30) * Math.exp(exponent);

        if (Double.isNaN(pCost) || pCost <= 0.0) {
            return 0L;
        }
        if (Double.isInfinite(pCost) || pCost > Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return (long) Math.ceil(pCost);
    }
}