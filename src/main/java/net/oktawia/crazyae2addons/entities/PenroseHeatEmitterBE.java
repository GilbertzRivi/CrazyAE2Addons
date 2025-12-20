package net.oktawia.crazyae2addons.entities;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.PenroseHeatEmitterMenu;
import org.jetbrains.annotations.Nullable;

public class PenroseHeatEmitterBE extends AENetworkBlockEntity implements MenuProvider, IGridTickable {

    private PenroseControllerBE controller;

    public double heatOnGK  = 1;
    public double heatOffGK = 0;

    private boolean emitting = false;

    private static final double MAX_HEAT_GK = 1_000_000.0;

    public PenroseHeatEmitterBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.PENROSE_HEAT_EMITTER_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.PENROSE_HEAT_EMITTER.get().asItem())
                );
    }

    public void setController(PenroseControllerBE controller) {
        this.controller = controller;
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

    private static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private void sanitizeThresholds() {
        heatOnGK = clamp(heatOnGK, 0.0, MAX_HEAT_GK);
        heatOffGK = clamp(heatOffGK, 0.0, MAX_HEAT_GK);

        // wymuszamy sensowną histerezę: OFF <= ON
        if (heatOffGK > heatOnGK) {
            double tmp = heatOffGK;
            heatOffGK = heatOnGK;
            heatOnGK = tmp;
        }
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);

        // nowe klucze
        if (data.contains("heatOnGK")) this.heatOnGK = data.getDouble("heatOnGK");
        if (data.contains("heatOffGK")) this.heatOffGK = data.getDouble("heatOffGK");
        if (data.contains("emitting")) this.emitting = data.getBoolean("emitting");

        // kompatybilność wstecz: stary desired_heat
        if (!data.contains("heatOnGK") && data.contains("desired_heat")) {
            this.heatOnGK = data.getDouble("desired_heat");
            this.heatOffGK = this.heatOnGK * 0.9; // sensowny default
        }

        sanitizeThresholds();
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        sanitizeThresholds();

        data.putDouble("heatOnGK", this.heatOnGK);
        data.putDouble("heatOffGK", this.heatOffGK);
        data.putBoolean("emitting", this.emitting);
    }

    /** Stan wyjścia redstone (bez side-effectów). */
    public boolean shouldEmit() {
        return emitting;
    }

    private boolean computeNextEmitState() {
        if (controller == null) return false;

        double h = controller.getHeat();

        // histereza:
        if (!emitting) {
            return h >= heatOnGK;
        } else {
            return h > heatOffGK; // gaśnie dopiero gdy <= heatOffGK
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PenroseHeatEmitterMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.penrose_heat_emitter");
    }

    public void openMenu(Player player, MenuLocator locator) {
        if (controller != null) {
            MenuOpener.open(CrazyMenuRegistrar.PENROSE_HEAT_EMITTER_MENU.get(), player, locator);
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        var lvl = getLevel();
        if (lvl == null) return TickRateModulation.IDLE;

        boolean next = computeNextEmitState();
        if (next != emitting) {
            emitting = next;
            setChanged();
            // notify TYLKO gdy stan się zmienia
            Platform.notifyBlocksOfNeighbors(lvl, getBlockPos());
        }

        return TickRateModulation.SAME;
    }
}
