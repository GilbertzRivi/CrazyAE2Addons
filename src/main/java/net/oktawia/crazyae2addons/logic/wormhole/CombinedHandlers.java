package net.oktawia.crazyae2addons.logic.wormhole;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public final class CombinedHandlers {

    private CombinedHandlers() {
    }

    public static final class Energy implements IEnergyStorage {

        private final List<IEnergyStorage> storages;

        public Energy(List<IEnergyStorage> storages) {
            this.storages = List.copyOf(Objects.requireNonNull(storages));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = 0;

            for (IEnergyStorage storage : storages) {
                if (received >= maxReceive) {
                    break;
                }

                received += storage.receiveEnergy(maxReceive - received, simulate);
            }

            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = 0;

            for (IEnergyStorage storage : storages) {
                if (extracted >= maxExtract) {
                    break;
                }

                extracted += storage.extractEnergy(maxExtract - extracted, simulate);
            }

            return extracted;
        }

        @Override
        public int getEnergyStored() {
            int total = 0;
            for (IEnergyStorage storage : storages) {
                total += storage.getEnergyStored();
            }
            return total;
        }

        @Override
        public int getMaxEnergyStored() {
            int total = 0;
            for (IEnergyStorage storage : storages) {
                total += storage.getMaxEnergyStored();
            }
            return total;
        }

        @Override
        public boolean canExtract() {
            for (IEnergyStorage storage : storages) {
                if (storage.canExtract()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canReceive() {
            for (IEnergyStorage storage : storages) {
                if (storage.canReceive()) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final class Fluid implements IFluidHandler {

        private final List<IFluidHandler> handlers;

        public Fluid(List<IFluidHandler> handlers) {
            this.handlers = List.copyOf(Objects.requireNonNull(handlers));
        }

        @Override
        public int getTanks() {
            int total = 0;
            for (IFluidHandler handler : handlers) {
                total += handler.getTanks();
            }
            return total;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            for (IFluidHandler handler : handlers) {
                int tanks = handler.getTanks();
                if (tank < tanks) {
                    return handler.getFluidInTank(tank);
                }
                tank -= tanks;
            }

            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            for (IFluidHandler handler : handlers) {
                int tanks = handler.getTanks();
                if (tank < tanks) {
                    return handler.getTankCapacity(tank);
                }
                tank -= tanks;
            }

            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            for (IFluidHandler handler : handlers) {
                int tanks = handler.getTanks();
                if (tank < tanks) {
                    return handler.isFluidValid(tank, stack);
                }
                tank -= tanks;
            }

            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }

            int filled = 0;
            int remaining = resource.getAmount();

            for (IFluidHandler handler : handlers) {
                if (remaining <= 0) {
                    break;
                }

                FluidStack request = resource.copy();
                request.setAmount(remaining);

                int accepted = handler.fill(request, action);
                filled += accepted;
                remaining -= accepted;
            }

            return filled;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }

            FluidStack totalDrained = FluidStack.EMPTY;
            int remaining = resource.getAmount();

            for (IFluidHandler handler : handlers) {
                if (remaining <= 0) {
                    break;
                }

                FluidStack request = resource.copy();
                request.setAmount(remaining);

                FluidStack drained = handler.drain(request, action);
                if (drained.isEmpty()) {
                    continue;
                }

                if (totalDrained.isEmpty()) {
                    totalDrained = drained.copy();
                } else if (totalDrained.isFluidEqual(drained)) {
                    totalDrained.grow(drained.getAmount());
                } else {
                    break;
                }

                remaining -= drained.getAmount();
            }

            return totalDrained.isEmpty() ? FluidStack.EMPTY : totalDrained;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }

            FluidStack totalDrained = FluidStack.EMPTY;
            int remaining = maxDrain;

            for (IFluidHandler handler : handlers) {
                if (remaining <= 0) {
                    break;
                }

                FluidStack drained = handler.drain(remaining, action);
                if (drained.isEmpty()) {
                    continue;
                }

                if (totalDrained.isEmpty()) {
                    totalDrained = drained.copy();
                } else if (totalDrained.isFluidEqual(drained)) {
                    totalDrained.grow(drained.getAmount());
                } else {
                    break;
                }

                remaining -= drained.getAmount();
            }

            return totalDrained.isEmpty() ? FluidStack.EMPTY : totalDrained;
        }
    }

    public static final class FluidItem implements IFluidHandlerItem {

        private final List<IFluidHandlerItem> handlers;

        public FluidItem(List<IFluidHandlerItem> handlers) {
            this.handlers = List.copyOf(Objects.requireNonNull(handlers));
        }

        @Override
        public int getTanks() {
            int total = 0;
            for (IFluidHandlerItem handler : handlers) {
                total += handler.getTanks();
            }
            return total;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            for (IFluidHandlerItem handler : handlers) {
                int tanks = handler.getTanks();
                if (tank < tanks) {
                    return handler.getFluidInTank(tank);
                }
                tank -= tanks;
            }

            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            for (IFluidHandlerItem handler : handlers) {
                int tanks = handler.getTanks();
                if (tank < tanks) {
                    return handler.getTankCapacity(tank);
                }
                tank -= tanks;
            }

            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            for (IFluidHandlerItem handler : handlers) {
                int tanks = handler.getTanks();
                if (tank < tanks) {
                    return handler.isFluidValid(tank, stack);
                }
                tank -= tanks;
            }

            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }

            int filled = 0;
            int remaining = resource.getAmount();

            for (IFluidHandlerItem handler : handlers) {
                if (remaining <= 0) {
                    break;
                }

                FluidStack request = resource.copy();
                request.setAmount(remaining);

                int accepted = handler.fill(request, action);
                filled += accepted;
                remaining -= accepted;
            }

            return filled;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }

            FluidStack totalDrained = FluidStack.EMPTY;
            int remaining = maxDrain;

            for (IFluidHandlerItem handler : handlers) {
                if (remaining <= 0) {
                    break;
                }

                FluidStack drained = handler.drain(remaining, action);
                if (drained.isEmpty()) {
                    continue;
                }

                if (totalDrained.isEmpty()) {
                    totalDrained = drained.copy();
                } else if (totalDrained.isFluidEqual(drained)) {
                    totalDrained.grow(drained.getAmount());
                } else {
                    break;
                }

                remaining -= drained.getAmount();
            }

            return totalDrained.isEmpty() ? FluidStack.EMPTY : totalDrained;
        }

        @Override
        public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }

            FluidStack totalDrained = FluidStack.EMPTY;
            int remaining = resource.getAmount();

            for (IFluidHandlerItem handler : handlers) {
                if (remaining <= 0) {
                    break;
                }

                FluidStack request = resource.copy();
                request.setAmount(remaining);

                FluidStack drained = handler.drain(request, action);
                if (drained.isEmpty()) {
                    continue;
                }

                if (totalDrained.isEmpty()) {
                    totalDrained = drained.copy();
                } else if (totalDrained.isFluidEqual(drained)) {
                    totalDrained.grow(drained.getAmount());
                } else {
                    break;
                }

                remaining -= drained.getAmount();
            }

            return totalDrained.isEmpty() ? FluidStack.EMPTY : totalDrained;
        }

        @Override
        public @NotNull ItemStack getContainer() {
            for (IFluidHandlerItem handler : handlers) {
                ItemStack container = handler.getContainer();
                if (!container.isEmpty()) {
                    return container;
                }
            }

            return ItemStack.EMPTY;
        }
    }
}