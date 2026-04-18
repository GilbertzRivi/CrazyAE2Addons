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
import net.oktawia.crazyae2addons.logic.penrose.PenroseLogic;
import net.oktawia.crazyae2addons.logic.interfaces.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseHeatEmitterMenu;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenroseHeatEmitterBE extends AbstractMultiblockFrameBE<PenroseControllerBE>
        implements MenuProvider, IGridTickable, IMenuOpeningBlockEntity {

    @Getter
    @Persisted
    @DescSynced
    private double heatOnGK = 1.0;

    @Getter
    @Persisted
    @DescSynced
    private double heatOffGK = 0.0;

    @Getter
    @DescSynced
    private boolean emitting = false;

    public PenroseHeatEmitterBE(BlockPos pos, BlockState blockState) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_HEAT_EMITTER_BE.get(),
                pos,
                blockState,
                new ItemStack(CrazyBlockRegistrar.PENROSE_HEAT_EMITTER.get().asItem()),
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

    public boolean shouldEmit() {
        return emitting;
    }

    public void setHeatOnGK(double heatOnGK) {
        this.heatOnGK = heatOnGK;
        sanitizeThresholds();
        setChanged();
        refreshEmissionState();
    }

    public void setHeatOffGK(double heatOffGK) {
        this.heatOffGK = heatOffGK;
        sanitizeThresholds();
        setChanged();
        refreshEmissionState();
    }

    private double getMaxAllowedHeatGk() {
        PenroseControllerBE controller = getResolvedController();
        if (controller != null) {
            return Math.max(0.0, controller.maxHeatGK);
        }
        return Math.max(0.0, PenroseLogic.MAX_HEAT_GK);
    }

    private void sanitizeThresholds() {
        double maxHeat = getMaxAllowedHeatGk();

        this.heatOnGK = Math.clamp(this.heatOnGK, 0.0, maxHeat);
        this.heatOffGK = Math.clamp(this.heatOffGK, 0.0, maxHeat);

        if (this.heatOffGK > this.heatOnGK) {
            this.heatOffGK = this.heatOnGK;
        }
    }

    private boolean computeNextEmitState() {
        PenroseControllerBE controller = getResolvedController();
        if (controller == null || !controller.isFormed()) {
            return false;
        }

        double heat = controller.getHeat();

        if (!emitting) {
            return heat >= heatOnGK;
        }

        return heat > heatOffGK;
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
        return new PenroseHeatEmitterMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_HEAT_EMITTER_MENU.get(), player, locator);
    }
}