package net.oktawia.crazyae2addons.menus.item;

import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPrioHost;
import net.oktawia.crazyae2addons.logic.interfaces.ICpuPrio;
import org.jetbrains.annotations.Nullable;

public class CpuPrioMenu extends AEBaseMenu {

    public static final String ACTION_SET_PRIO = "crazySetCpuPrio";

    @GuiSync(0)
    public int prio = 0;

    private final CpuPrioHost host;
    private @Nullable appeng.me.cluster.implementations.CraftingCPUCluster targetCpu;
    @Getter
    private ItemStack targetIcon = ItemStack.EMPTY;

    public CpuPrioMenu(int id, Inventory playerInventory, CpuPrioHost host) {
        super(CrazyMenuRegistrar.CPU_PRIO_MENU.get(), id, playerInventory, host);
        this.host = host;

        registerClientAction(ACTION_SET_PRIO, Integer.class, this::setPriority);

        resolveMenuState(playerInventory);
    }

    private void resolveMenuState(Inventory playerInventory) {
        BlockPos targetPos = this.host.getTargetPos();
        if (targetPos == null) {
            this.targetCpu = null;
            this.targetIcon = ItemStack.EMPTY;
            this.prio = 0;
            return;
        }

        var level = playerInventory.player.level();
        var blockEntity = level.getBlockEntity(targetPos);

        if (!(blockEntity instanceof CraftingBlockEntity craftingBlockEntity)) {
            this.targetCpu = null;
            this.targetIcon = ItemStack.EMPTY;
            this.prio = 0;
            return;
        }

        this.targetIcon = new ItemStack(craftingBlockEntity.getBlockState().getBlock());
        this.targetCpu = craftingBlockEntity.getCluster();

        if ((Object) this.targetCpu instanceof ICpuPrio prioHolder) {
            this.prio = prioHolder.getPrio();
        } else {
            this.prio = 0;
        }
    }

    public void setPriority(int newPrio) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_PRIO, newPrio);
            return;
        }

        if (this.targetCpu == null) {
            resolveMenuState(getPlayerInventory());
        }

        if ((Object) this.targetCpu instanceof ICpuPrio prioHolder) {
            prioHolder.setPrio(newPrio);
            this.targetCpu.markDirty();
            this.prio = newPrio;
        }
    }
}