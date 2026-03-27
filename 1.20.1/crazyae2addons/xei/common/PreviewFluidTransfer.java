package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public class PreviewFluidTransfer implements IFluidTransfer {

    private FluidStack stack = FluidStack.empty();
    private final long capacity;

    public PreviewFluidTransfer(@Nullable FluidStack initial, long capacity) {
        this.capacity = Math.max(1, capacity);
        if (initial != null && !initial.isEmpty()) {
            this.stack = clamp(initial.copy());
        }
    }

    private FluidStack clamp(FluidStack in) {
        if (in == null || in.isEmpty()) return FluidStack.empty();
        long amt = Math.min(in.getAmount(), capacity);
        if (amt <= 0) return FluidStack.empty();
        return FluidStack.create(in.getFluid(), amt, in.getTag());
    }

    private void setInternal(@Nullable FluidStack s, boolean notifyChanges) {
        this.stack = clamp(s);
        if (notifyChanges) onContentsChanged();
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public void setFluidInTank(int tank, FluidStack fluidStack) {
        if (tank != 0) return;
        setInternal(fluidStack, true);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0; // podgląd recepty: wszystko valid
    }

    @Override
    public long fill(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
        if (tank != 0) return 0;
        if (resource == null || resource.isEmpty()) return 0;

        if (stack.isEmpty()) {
            long toFill = Math.min(capacity, resource.getAmount());
            if (!simulate && toFill > 0) {
                FluidStack next = FluidStack.create(resource.getFluid(), toFill, resource.getTag());
                setInternal(next, notifyChanges);
            }
            return toFill;
        }

        // nie mieszamy różnych fluidów
        if (stack.getFluid() != resource.getFluid()) return 0;

        long space = capacity - stack.getAmount();
        if (space <= 0) return 0;

        long toFill = Math.min(space, resource.getAmount());
        if (!simulate && toFill > 0) {
            long newAmt = stack.getAmount() + toFill;
            FluidStack next = FluidStack.create(stack.getFluid(), newAmt, stack.getTag());
            setInternal(next, notifyChanges);
        }
        return toFill;
    }

    @Override
    public boolean supportsFill(int tank) {
        return tank == 0;
    }

    @Override
    public FluidStack drain(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
        if (tank != 0) return FluidStack.empty();
        if (resource == null || resource.isEmpty()) return FluidStack.empty();
        if (stack.isEmpty()) return FluidStack.empty();
        if (stack.getFluid() != resource.getFluid()) return FluidStack.empty();

        return drain(resource.getAmount(), simulate, notifyChanges);
    }

    @Override
    public boolean supportsDrain(int tank) {
        return tank == 0;
    }

    @Override
    public Object createSnapshot() {
        return stack.copy();
    }

    @Override
    public void restoreFromSnapshot(Object snapshot) {
        if (snapshot instanceof FluidStack fs) {
            setInternal(fs.copy(), true);
        }
    }

    @Override
    public long fill(FluidStack resource, boolean simulate, boolean notifyChanges) {
        return fill(0, resource, simulate, notifyChanges);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate, boolean notifyChanges) {
        return drain(0, resource, simulate, notifyChanges);
    }

    @Override
    public FluidStack drain(long maxDrain, boolean simulate, boolean notifyChanges) {
        if (stack.isEmpty() || maxDrain <= 0) return FluidStack.empty();

        long drained = Math.min(maxDrain, stack.getAmount());
        FluidStack ret = FluidStack.create(stack.getFluid(), drained, stack.getTag());

        if (!simulate && drained > 0) {
            long remain = stack.getAmount() - drained;
            FluidStack next = (remain <= 0)
                    ? FluidStack.empty()
                    : FluidStack.create(stack.getFluid(), remain, stack.getTag());
            setInternal(next, notifyChanges);
        }

        return ret;
    }

    @Override
    public void onContentsChanged() {
    }

    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? stack.copy() : FluidStack.empty();
    }

    public long getTankCapacity(int tank) {
        return tank == 0 ? capacity : 0;
    }
}
