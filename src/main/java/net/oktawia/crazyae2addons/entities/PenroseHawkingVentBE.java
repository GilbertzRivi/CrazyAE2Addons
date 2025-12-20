package net.oktawia.crazyae2addons.entities;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.PenroseHawkingVentMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PenroseHawkingVentBE extends AENetworkBlockEntity implements MenuProvider {

    private PenroseControllerBE controller;

    public double desiredEvap;

    private static final double REF_FEED = 16.0;

    final EnergyStorage energyStorage =
            new EnergyStorage(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    EnergyStorage getInternalEnergyStorage() { return energyStorage; }

    private final LazyOptional<IEnergyStorage> energyCap =
            LazyOptional.of(() -> new IEnergyStorage() {
                @Override public int receiveEnergy(int maxReceive, boolean simulate) {
                    return energyStorage.receiveEnergy(maxReceive, simulate);
                }
                @Override public int extractEnergy(int maxExtract, boolean simulate) {
                    return energyStorage.extractEnergy(maxExtract, simulate);
                }
                @Override public int getEnergyStored() { return energyStorage.getEnergyStored(); }
                @Override public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
                @Override public boolean canExtract() { return false; }
                @Override public boolean canReceive() { return true; }
            });

    public PenroseHawkingVentBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.PENROSE_HAWKING_VENT_BE.get(), pos, state);
        this.getMainNode()
                .setIdlePowerUsage(1.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.PENROSE_HAWKING_VENT.get().asItem())
                );
    }

    public void setController(PenroseControllerBE controller) {
        if (this.controller == controller) return;

        if (this.controller != null) this.controller.unregisterHawkingVent(this);
        this.controller = controller;
        if (this.controller != null) this.controller.registerHawkingVent(this);

        if (this.controller != null) {
            if (getMainNode().getNode().getConnections().stream()
                    .noneMatch(x -> (x.a() == this.controller.getMainNode().getNode()
                            || x.b() == this.controller.getMainNode().getNode()))) {
                GridHelper.createConnection(getMainNode().getNode(), this.controller.getMainNode().getNode());
            }
        } else {
            getMainNode().getNode().getConnections().stream()
                    .filter(x -> !x.isInWorld())
                    .forEach(IGridConnection::destroy);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (controller != null) controller.unregisterHawkingVent(this);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("desiredEvap")) {
            this.desiredEvap = data.getDouble("desiredEvap");
        }
        if (data.contains("energy")) {
            int e = data.getInt("energy");
            energyStorage.receiveEnergy(e, false);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putDouble("desiredEvap", this.desiredEvap);
        data.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inv, Player player) {
        return new PenroseHawkingVentMenu(i, inv, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.penrose_hawking_vent");
    }

    public void openMenu(Player player, MenuLocator locator) {
        if (controller != null) {
            MenuOpener.open(CrazyMenuRegistrar.PENROSE_HAWKING_VENT_MENU.get(), player, locator);
        }
    }

    public static long computeCostForEvap(double evapRate) {
        if (evapRate <= 0.0) return 0L;
        evapRate /= 3;

        double exponent = 2.0 * ((evapRate / REF_FEED) - 1.0);
        double pCost = (double) (1L << 30) * Math.exp(exponent);

        if (Double.isNaN(pCost) || pCost <= 0.0) return 0L;
        if (Double.isInfinite(pCost) || pCost > Long.MAX_VALUE) return Long.MAX_VALUE;

        return (long) Math.ceil(pCost);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        return super.getCapability(cap);
    }
}
