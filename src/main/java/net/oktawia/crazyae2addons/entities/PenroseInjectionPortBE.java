package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
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
import net.oktawia.crazyae2addons.menus.PenroseInjectionPortMenu;
import org.jetbrains.annotations.Nullable;

public class PenroseInjectionPortBE extends AENetworkBlockEntity implements MenuProvider {

    private PenroseControllerBE controller;

    public int desiredRate;
    public static final int MAX_RATE = 1024;

    public PenroseInjectionPortBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.PENROSE_INJECTION_PORT_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.PENROSE_INJECTION_PORT.get().asItem())
                );
    }

    public void setController(PenroseControllerBE controller) {
        if (this.controller == controller) return;

        // unregister old
        if (this.controller != null) this.controller.unregisterInjectionPort(this);

        this.controller = controller;

        // register new
        if (this.controller != null) this.controller.registerInjectionPort(this);

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
        if (controller != null) controller.unregisterInjectionPort(this);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("desiredRate")) {
            this.desiredRate = data.getInt("desiredRate");
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putInt("desiredRate", this.desiredRate);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PenroseInjectionPortMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.penrose_injection_port");
    }

    public void openMenu(Player player, MenuLocator locator) {
        if (controller != null) {
            MenuOpener.open(CrazyMenuRegistrar.PENROSE_INJECTION_PORT_MENU.get(), player, locator);
        }
    }

    // >>> tickowane przez kontroler
    public void tickFromController(int ticks) {
        var level = getLevel();
        if (level == null) return;

        if (controller == null || !controller.isBlackHoleActive()) return;
        if (controller.isVentingLocked()) return;
        if (!level.hasNeighborSignal(getBlockPos())) return;

        int perTick = Math.min(desiredRate, MAX_RATE);
        if (perTick <= 0) return;

        var grid = getMainNode().getGrid();
        if (grid == null) return;

        int t = Math.max(1, ticks);

        long want = (long) perTick * (long) t;
        if (want <= 0L) return;

        var inv = grid.getStorageService().getInventory();
        var key = AEItemKey.of(AEItems.SINGULARITY);

        long extracted = inv.extract(key, want, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (extracted <= 0L) return;

        // controller.addFeedMu ma int, a extracted jest long
        long left = extracted;
        while (left > 0L) {
            int chunk = (int) Math.min(Integer.MAX_VALUE, left);
            controller.addFeedMu(chunk);
            left -= chunk;
        }
    }
}
