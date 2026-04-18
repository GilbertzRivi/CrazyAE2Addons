package net.oktawia.crazyae2addons.entities.penrose;

import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
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
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseHawkingVentMenu;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenroseHawkingVentBE extends AbstractMultiblockFrameBE<PenroseControllerBE>
        implements MenuProvider, IMenuOpeningBlockEntity {

    private static final double REF_FEED = 16.0;

    @Getter
    @Setter
    @Persisted
    @DescSynced
    private double desiredEvap = 0.0;

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
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public PenroseHawkingVentBE(BlockPos pos, BlockState state) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_HAWKING_VENT_BE.get(),
                pos,
                state,
                new ItemStack(CrazyBlockRegistrar.PENROSE_HAWKING_VENT.get().asItem()),
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
            newController.registerHawkingVent(this);
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
            activeController.unregisterHawkingVent(this);
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PenroseHawkingVentMenu(id, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_HAWKING_VENT_MENU.get(), player, locator);
    }

    public static long computeCostForEvap(double evapRate) {
        if (evapRate <= 0.0) {
            return 0L;
        }

        evapRate /= 3.0;

        double exponent = 2.0 * ((evapRate / REF_FEED) - 1.0);
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