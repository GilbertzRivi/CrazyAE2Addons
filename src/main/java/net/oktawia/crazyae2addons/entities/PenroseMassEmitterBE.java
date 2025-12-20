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
import net.oktawia.crazyae2addons.menus.PenroseMassEmitterMenu;
import org.jetbrains.annotations.Nullable;

public class PenroseMassEmitterBE extends AENetworkBlockEntity implements MenuProvider, IGridTickable {

    private PenroseControllerBE controller;

    public double massOnPercent  = 1;
    public double massOffPercent = 0;

    private boolean emitting = false;

    private static final long INITIAL_BH_MASS = PenroseControllerBE.cfgInitialMassMu();
    private static final long MAX_BH_MASS     = PenroseControllerBE.cfgMaxMassMu();

    public PenroseMassEmitterBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.PENROSE_MASS_EMITTER_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.PENROSE_MASS_EMITTER.get().asItem())
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
        massOnPercent  = clamp(massOnPercent, 0.0, 100.0);
        massOffPercent = clamp(massOffPercent, 0.0, 100.0);

        // OFF <= ON (żeby histereza miała sens)
        if (massOffPercent > massOnPercent) {
            double tmp = massOffPercent;
            massOffPercent = massOnPercent;
            massOnPercent = tmp;
        }
    }

    private static long massFromPercent(double pct) {
        double p = clamp(pct, 0.0, 100.0) / 100.0;
        long span = MAX_BH_MASS - INITIAL_BH_MASS;
        if (span <= 0) return INITIAL_BH_MASS;
        double m = (double) INITIAL_BH_MASS + ((double) span) * p;
        long out = (long) Math.round(m);
        if (out < INITIAL_BH_MASS) return INITIAL_BH_MASS;
        if (out > MAX_BH_MASS) return MAX_BH_MASS;
        return out;
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);

        // nowe klucze
        if (data.contains("massOnPercent")) this.massOnPercent = data.getDouble("massOnPercent");
        if (data.contains("massOffPercent")) this.massOffPercent = data.getDouble("massOffPercent");
        if (data.contains("emitting")) this.emitting = data.getBoolean("emitting");

        // kompatybilność wstecz: stare ratio (z poprzedniej wersji)
        if (!data.contains("massOnPercent") && data.contains("massOnRatio")) {
            double onRatio = data.getDouble("massOnRatio");
            double offRatio = data.contains("massOffRatio") ? data.getDouble("massOffRatio") : onRatio;

            long onMass = (long) Math.round(onRatio * (double) INITIAL_BH_MASS);
            long offMass = (long) Math.round(offRatio * (double) INITIAL_BH_MASS);

            long span = MAX_BH_MASS - INITIAL_BH_MASS;
            if (span > 0) {
                this.massOnPercent = clamp(((double) (onMass - INITIAL_BH_MASS) / (double) span) * 100.0, 0.0, 100.0);
                this.massOffPercent = clamp(((double) (offMass - INITIAL_BH_MASS) / (double) span) * 100.0, 0.0, 100.0);
            } else {
                this.massOnPercent = 0.0;
                this.massOffPercent = 0.0;
            }
        }

        // kompatybilność wstecz: stary zapis "desired" jako absolute MU
        if (!data.contains("massOnPercent") && data.contains("desired")) {
            long oldDesiredMass = data.getLong("desired");
            long span = MAX_BH_MASS - INITIAL_BH_MASS;
            if (oldDesiredMass > 0 && span > 0) {
                this.massOnPercent = clamp(((double) (oldDesiredMass - INITIAL_BH_MASS) / (double) span) * 100.0, 0.0, 100.0);
                this.massOffPercent = clamp(this.massOnPercent - 5.0, 0.0, 100.0);
            }
        }

        sanitizeThresholds();
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        sanitizeThresholds();

        data.putDouble("massOnPercent", this.massOnPercent);
        data.putDouble("massOffPercent", this.massOffPercent);
        data.putBoolean("emitting", this.emitting);
    }

    public boolean shouldEmit() {
        return emitting;
    }

    private boolean computeNextEmitState() {
        if (controller == null) return false;

        long mass = controller.getBlackHoleMass();
        long onMass  = massFromPercent(massOnPercent);
        long offMass = massFromPercent(massOffPercent);

        if (!emitting) {
            return mass >= onMass;
        } else {
            return mass > offMass; // gaśnie dopiero gdy <= offMass
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PenroseMassEmitterMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.penrose_mass_emitter");
    }

    public void openMenu(Player player, MenuLocator locator) {
        if (controller != null) {
            MenuOpener.open(CrazyMenuRegistrar.PENROSE_MASS_EMITTER_MENU.get(), player, locator);
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
            Platform.notifyBlocksOfNeighbors(lvl, getBlockPos());
        }

        return TickRateModulation.SAME;
    }
}
