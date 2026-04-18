package net.oktawia.crazyae2addons.entities.penrose;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseInjectionPortMenu;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenroseInjectionPortBE extends AbstractMultiblockFrameBE<PenroseControllerBE>
        implements MenuProvider, IMenuOpeningBlockEntity {

    public static final int MAX_RATE = 1024;

    @Getter
    @Persisted
    @DescSynced
    private int desiredRate = 0;

    public PenroseInjectionPortBE(BlockPos pos, BlockState blockState) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_INJECTION_PORT_BE.get(),
                pos,
                blockState,
                new ItemStack(CrazyBlockRegistrar.PENROSE_INJECTION_PORT.get().asItem()),
                2.0F
        );
    }

    @Override
    protected Class<PenroseControllerBE> controllerClass() {
        return PenroseControllerBE.class;
    }

    @Override
    public void setController(@Nullable BlockEntity controller) {
        unregisterFromController();
        super.setController(controller);
    }

    @Override
    protected void onControllerChanged(@Nullable PenroseControllerBE newController) {
        if (newController != null) {
            newController.registerInjectionPort(this);
            connectToControllerGrid();
        } else {
            disconnectFromControllerGrid();
        }
    }

    @Override
    public void setRemoved() {
        unregisterFromController();
        super.setRemoved();
    }

    private void unregisterFromController() {
        if (activeController != null) {
            activeController.unregisterInjectionPort(this);
        }
    }

    public void setDesiredRate(int desiredRate) {
        this.desiredRate = Math.clamp(desiredRate, 0, MAX_RATE);
        setChanged();
    }

    public void tickFromController(int ticks) {
        var level = getLevel();
        if (level == null) {
            return;
        }

        PenroseControllerBE controller = getResolvedController();
        if (controller == null || !controller.isBlackHoleActive()) {
            return;
        }
        if (controller.isVentingLocked()) {
            return;
        }
        if (!level.hasNeighborSignal(getBlockPos())) {
            return;
        }

        int perTick = Math.clamp(desiredRate, 0, MAX_RATE);
        if (perTick <= 0) {
            return;
        }

        var grid = getMainNode().getGrid();
        if (grid == null) {
            return;
        }

        int t = Math.max(1, ticks);
        long want = (long) perTick * (long) t;
        if (want <= 0L) {
            return;
        }

        var inv = grid.getStorageService().getInventory();
        var key = AEItemKey.of(AEItems.SINGULARITY);

        long extracted = inv.extract(key, want, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (extracted <= 0L) {
            return;
        }

        long left = extracted;
        while (left > 0L) {
            int chunk = (int) Math.min(Integer.MAX_VALUE, left);
            controller.addFeedMu(chunk);
            left -= chunk;
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PenroseInjectionPortMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_INJECTION_PORT_MENU.get(), player, locator);
    }
}