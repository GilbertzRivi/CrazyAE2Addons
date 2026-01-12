package net.oktawia.crazyae2addonslite.misc;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CombinedFluidHandlerItem implements IFluidHandlerItem {

    private final List<IFluidHandlerItem> handlers;

    public CombinedFluidHandlerItem(List<IFluidHandlerItem> handlers) {
        this.handlers = handlers;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        for (IFluidHandlerItem handler : handlers) {
            FluidStack drained = handler.drain(maxDrain, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(@NotNull FluidStack resource, FluidAction action) {
        for (IFluidHandlerItem handler : handlers) {
            FluidStack drained = handler.drain(resource, action);
            if (!drained.isEmpty()) {
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        for (IFluidHandlerItem handler : handlers) {
            int filled = handler.fill(resource, action);
            if (filled > 0) {
                return filled;
            }
        }
        return 0;
    }

    @Override
    public int getTanks() {
        return handlers.stream().mapToInt(IFluidHandlerItem::getTanks).sum();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        int base = 0;
        for (IFluidHandlerItem handler : handlers) {
            int tanks = handler.getTanks();
            if (tank < base + tanks) {
                return handler.getFluidInTank(tank - base);
            }
            base += tanks;
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        int base = 0;
        for (IFluidHandlerItem handler : handlers) {
            int tanks = handler.getTanks();
            if (tank < base + tanks) {
                return handler.getTankCapacity(tank - base);
            }
            base += tanks;
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        int base = 0;
        for (IFluidHandlerItem handler : handlers) {
            int tanks = handler.getTanks();
            if (tank < base + tanks) {
                return handler.isFluidValid(tank - base, stack);
            }
            base += tanks;
        }
        return false;
    }

    @Override
    public @NotNull net.minecraft.world.item.ItemStack getContainer() {
        for (IFluidHandlerItem handler : handlers) {
            if (!handler.getContainer().isEmpty()) {
                return handler.getContainer();
            }
        }
        return handlers.isEmpty() ? net.minecraft.world.item.ItemStack.EMPTY : handlers.get(0).getContainer();
    }
}
