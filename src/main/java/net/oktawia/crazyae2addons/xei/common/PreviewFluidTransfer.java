package net.oktawia.crazyae2addons.xei.common;

import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PreviewFluidTransfer implements IFluidTransfer {

    private FluidStack stack = FluidStack.empty();
    private final long capacity;

    public PreviewFluidTransfer(@Nullable FluidStack initial, long capacity) {
        this.capacity = Math.max(1L, capacity);
        this.stack = normalize(initial);
    }

    private FluidStack normalize(@Nullable FluidStack in) {
        if (in == null || in.isEmpty()) {
            return FluidStack.empty();
        }

        long amount = Math.min(Math.max(0L, in.getAmount()), this.capacity);
        if (amount <= 0L) {
            return FluidStack.empty();
        }

        return FluidStack.create(in.getFluid(), amount, in.getTag());
    }

    private boolean isValidTank(int tank) {
        return tank == 0;
    }

    private boolean sameFluid(FluidStack a, FluidStack b) {
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }

        return a.getFluid() == b.getFluid()
                && Objects.equals(a.getTag(), b.getTag());
    }

    private void setInternal(@Nullable FluidStack newStack, boolean notifyChanges) {
        FluidStack normalized = normalize(newStack);

        boolean changed =
                this.stack.isEmpty() != normalized.isEmpty()
                        || (!this.stack.isEmpty() && !normalized.isEmpty()
                        && (!sameFluid(this.stack, normalized)
                        || this.stack.getAmount() != normalized.getAmount()));

        this.stack = normalized;

        if (changed && notifyChanges) {
            onContentsChanged();
        }
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return isValidTank(tank) ? this.stack.copy() : FluidStack.empty();
    }

    @Override
    public void setFluidInTank(int tank, FluidStack fluidStack) {
        if (!isValidTank(tank)) {
            return;
        }

        setInternal(fluidStack, true);
    }

    @Override
    public long getTankCapacity(int tank) {
        return isValidTank(tank) ? this.capacity : 0L;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack candidate) {
        return isValidTank(tank);
    }

    @Override
    public long fill(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
        if (!isValidTank(tank) || resource == null || resource.isEmpty()) {
            return 0L;
        }

        FluidStack normalizedResource = normalize(resource);
        if (normalizedResource.isEmpty()) {
            return 0L;
        }

        if (this.stack.isEmpty()) {
            long inserted = Math.min(this.capacity, normalizedResource.getAmount());

            if (!simulate && inserted > 0L) {
                setInternal(
                        FluidStack.create(
                                normalizedResource.getFluid(),
                                inserted,
                                normalizedResource.getTag()
                        ),
                        notifyChanges
                );
            }

            return inserted;
        }

        if (!sameFluid(this.stack, normalizedResource)) {
            return 0L;
        }

        long freeSpace = this.capacity - this.stack.getAmount();
        if (freeSpace <= 0L) {
            return 0L;
        }

        long inserted = Math.min(freeSpace, normalizedResource.getAmount());
        if (!simulate && inserted > 0L) {
            setInternal(
                    FluidStack.create(
                            this.stack.getFluid(),
                            this.stack.getAmount() + inserted,
                            this.stack.getTag()
                    ),
                    notifyChanges
            );
        }

        return inserted;
    }

    @Override
    public long fill(FluidStack resource, boolean simulate, boolean notifyChanges) {
        return fill(0, resource, simulate, notifyChanges);
    }

    @Override
    public boolean supportsFill(int tank) {
        return isValidTank(tank);
    }

    @Override
    public FluidStack drain(int tank, FluidStack resource, boolean simulate, boolean notifyChanges) {
        if (!isValidTank(tank) || resource == null || resource.isEmpty() || this.stack.isEmpty()) {
            return FluidStack.empty();
        }

        if (!sameFluid(this.stack, resource)) {
            return FluidStack.empty();
        }

        return drain(resource.getAmount(), simulate, notifyChanges);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate, boolean notifyChanges) {
        return drain(0, resource, simulate, notifyChanges);
    }

    @Override
    public FluidStack drain(long maxDrain, boolean simulate, boolean notifyChanges) {
        if (this.stack.isEmpty() || maxDrain <= 0L) {
            return FluidStack.empty();
        }

        long drainedAmount = Math.min(maxDrain, this.stack.getAmount());
        if (drainedAmount <= 0L) {
            return FluidStack.empty();
        }

        FluidStack drained = FluidStack.create(
                this.stack.getFluid(),
                drainedAmount,
                this.stack.getTag()
        );

        if (!simulate) {
            long remaining = this.stack.getAmount() - drainedAmount;
            if (remaining <= 0L) {
                setInternal(FluidStack.empty(), notifyChanges);
            } else {
                setInternal(
                        FluidStack.create(this.stack.getFluid(), remaining, this.stack.getTag()),
                        notifyChanges
                );
            }
        }

        return drained;
    }

    @Override
    public boolean supportsDrain(int tank) {
        return isValidTank(tank);
    }

    @Override
    public Object createSnapshot() {
        return this.stack.copy();
    }

    @Override
    public void restoreFromSnapshot(Object snapshot) {
        if (snapshot instanceof FluidStack fluidStack) {
            setInternal(fluidStack.copy(), true);
        } else {
            setInternal(FluidStack.empty(), true);
        }
    }

    @Override
    public void onContentsChanged() {
    }
}