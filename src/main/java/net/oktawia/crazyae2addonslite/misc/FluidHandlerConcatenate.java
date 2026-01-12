package net.oktawia.crazyae2addonslite.misc;

import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public class FluidHandlerConcatenate implements IFluidHandler {

    private final List<IFluidHandler> handlers;

    public FluidHandlerConcatenate(List<IFluidHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public int getTanks() {
        return handlers.stream().mapToInt(IFluidHandler::getTanks).sum();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        for (IFluidHandler h : handlers) {
            if (tank < h.getTanks()) return h.getFluidInTank(tank);
            tank -= h.getTanks();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        for (IFluidHandler h : handlers) {
            if (tank < h.getTanks()) return h.getTankCapacity(tank);
            tank -= h.getTanks();
        }
        return 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        for (IFluidHandler h : handlers) {
            if (tank < h.getTanks()) return h.isFluidValid(tank, stack);
            tank -= h.getTanks();
        }
        return false;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int filled = 0;
        for (IFluidHandler h : handlers) {
            filled += h.fill(resource, action);
            if (filled >= resource.getAmount()) break;
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        for (IFluidHandler h : handlers) {
            FluidStack drained = h.drain(resource, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (IFluidHandler h : handlers) {
            FluidStack drained = h.drain(maxDrain, action);
            if (!drained.isEmpty()) return drained;
        }
        return FluidStack.EMPTY;
    }
}
