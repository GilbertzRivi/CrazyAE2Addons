package net.oktawia.crazyae2addons.entities;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.blockentity.grid.AENetworkBlockEntity;
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
import net.oktawia.crazyae2addons.menus.PenroseHeatVentMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PenroseHeatVentBE extends AENetworkBlockEntity implements MenuProvider {

    private PenroseControllerBE controller;

    public double desiredCooling;

    private static final double HEAT_PER_MU = 1.0;
    private static final double REF_FE    = 16.0;

    final EnergyStorage energyStorage =
            new EnergyStorage(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    // package-private getter dla kontrolera
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
                @Override public boolean canExtract() { return true; }
                @Override public boolean canReceive() { return true; }
            });

    public PenroseHeatVentBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.PENROSE_HEAT_VENT_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(1.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.PENROSE_HEAT_VENT.get().asItem())
                );
    }

    public void setController(PenroseControllerBE controller) {
        if (this.controller == controller) return;

        if (this.controller != null) this.controller.unregisterHeatVent(this);
        this.controller = controller;
        if (this.controller != null) this.controller.registerHeatVent(this);

        if (this.controller != null) {
            if (getMainNode().getNode().getConnections().stream()
                    .noneMatch(x -> (x.a() == this.controller.getMainNode().getNode()
                            || x.b() == this.controller.getMainNode().getNode()))) {
                GridHelper.createConnection(getMainNode().getNode(), this.controller.getMainNode().getNode());
            }
        } else {
            getMainNode().getNode().getConnections().stream()
                    .filter(x -> (!x.isInWorld()))
                    .forEach(IGridConnection::destroy);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (controller != null) controller.unregisterHeatVent(this);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("desiredCooling")) {
            this.desiredCooling = data.getDouble("desiredCooling");
        }
        if (data.contains("energy")) {
            int e = data.getInt("energy");
            energyStorage.receiveEnergy(e, false);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putDouble("desiredCooling", this.desiredCooling);
        data.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PenroseHeatVentMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.penrose_heat_vent");
    }

    public void openMenu(Player player, MenuLocator locator) {
        if (controller != null){
            MenuOpener.open(CrazyMenuRegistrar.PENROSE_HEAT_VENT_MENU.get(), player, locator);
        }
    }

    public static long computeCostForCooling(double cooling) {
        if (cooling <= 0.0) return 0L;

        double fEq = cooling / HEAT_PER_MU;
        double exponent = 2.0 * ((fEq / REF_FE) - 1.0);
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
