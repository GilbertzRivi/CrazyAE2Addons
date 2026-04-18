package net.oktawia.crazyae2addons.entities.penrose;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.Platform;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseMassEmitterMenu;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenroseMassEmitterBE extends AbstractMultiblockFrameBE<PenroseControllerBE>
        implements MenuProvider, IGridTickable, IMenuOpeningBlockEntity {

    @Getter
    @Persisted
    @DescSynced
    private double massOnPercent = 1.0;

    @Getter
    @Persisted
    @DescSynced
    private double massOffPercent = 0.0;

    @Getter
    @DescSynced
    private boolean emitting = false;

    public PenroseMassEmitterBE(BlockPos pos, BlockState blockState) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_MASS_EMITTER_BE.get(),
                pos,
                blockState,
                new ItemStack(CrazyBlockRegistrar.PENROSE_MASS_EMITTER.get().asItem()),
                2.0F
        );
        this.getMainNode().addService(IGridTickable.class, this);
    }

    @Override
    protected Class<PenroseControllerBE> controllerClass() {
        return PenroseControllerBE.class;
    }

    @Override
    protected void onControllerChanged(@Nullable PenroseControllerBE newController) {
        if (newController != null) {
            connectToControllerGrid();
        } else {
            disconnectFromControllerGrid();
        }

        refreshEmissionState();
    }

    @Override
    public void loadManagedPersistentData(HolderLookup.Provider provider, CompoundTag tag) {
        super.loadManagedPersistentData(provider, tag);
        sanitizeThresholds();
        this.emitting = false;
    }

    public void setMassOnPercent(double massOnPercent) {
        this.massOnPercent = massOnPercent;
        sanitizeThresholds();
        setChanged();
        refreshEmissionState();
    }

    public void setMassOffPercent(double massOffPercent) {
        this.massOffPercent = massOffPercent;
        sanitizeThresholds();
        setChanged();
        refreshEmissionState();
    }

    public boolean shouldEmit() {
        return emitting;
    }

    private void sanitizeThresholds() {
        this.massOnPercent = Math.clamp(this.massOnPercent, 0.0, 100.0);
        this.massOffPercent = Math.clamp(this.massOffPercent, 0.0, 100.0);

        if (this.massOffPercent > this.massOnPercent) {
            this.massOffPercent = this.massOnPercent;
        }
    }

    private long massFromPercent(double percent) {
        PenroseControllerBE controller = getResolvedController();
        if (controller == null) {
            return 0L;
        }

        long initial = controller.getInitialBhMass();
        long max = controller.getMaxBhMass();

        double p = Math.clamp(percent, 0.0, 100.0) / 100.0;
        long span = max - initial;
        if (span <= 0L) {
            return initial;
        }

        double mass = (double) initial + (double) span * p;
        long out = Math.round(mass);
        return Math.clamp(out, initial, max);
    }

    private boolean computeNextEmitState() {
        PenroseControllerBE controller = getResolvedController();
        if (controller == null || !controller.isFormed()) {
            return false;
        }

        long mass = controller.getBlackHoleMass();
        long onMass = massFromPercent(massOnPercent);
        long offMass = massFromPercent(massOffPercent);

        if (!emitting) {
            return mass >= onMass;
        }

        return mass > offMass;
    }

    private void refreshEmissionState() {
        Level level = getLevel();
        boolean next = computeNextEmitState();

        if (next == this.emitting) {
            return;
        }

        this.emitting = next;
        setChanged();

        if (level != null) {
            Platform.notifyBlocksOfNeighbors(level, getBlockPos());
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (getLevel() == null) {
            return TickRateModulation.IDLE;
        }

        refreshEmissionState();
        return TickRateModulation.SAME;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PenroseMassEmitterMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_MASS_EMITTER_MENU.get(), player, locator);
    }
}