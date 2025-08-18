package net.oktawia.crazyae2addons.menus;

import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.CpuPrioTunerItem;
import net.oktawia.crazyae2addons.interfaces.ICraftingClusterPrio;
import net.oktawia.crazyae2addons.logic.CpuPrioHost;
import net.oktawia.crazyae2addons.logic.ViewCellHost;

public class CpuPrioMenu extends AEBaseMenu {

    public static final String ACTION_SET_PRIO = "crazySetCpuPrio";

    @GuiSync(0)
    public int prio = 0;

    private CraftingCPUCluster targetCpu;

    public CpuPrioMenu(int id, Inventory playerInventory, CpuPrioHost host) {
        super(CrazyMenuRegistrar.CPU_PRIO_MENU.get(), id, playerInventory, host);

        if (!isClientSide()) {
            var stack = host.getItemStack();
            var tag = stack.getOrCreateTag();

            if (tag.contains(CpuPrioTunerItem.NBT_CPU_POS)) {
                var targetPos = BlockPos.of(tag.getLong(CpuPrioTunerItem.NBT_CPU_POS));
                var be = playerInventory.player.level().getBlockEntity(targetPos);
                if (be instanceof CraftingBlockEntity cbe) {
                    this.targetCpu = cbe.getCluster();
                    if (this.targetCpu != null) {
                        this.prio = ((ICraftingClusterPrio) (Object) this.targetCpu).getPrio();
                    }
                }
            }
        }

        registerClientAction(ACTION_SET_PRIO, Integer.class, this::setPriority);
    }

    public void setPriority(int newPrio) {
        if (isClientSide()){
            sendClientAction(ACTION_SET_PRIO, newPrio);
        } else {
            ((ICraftingClusterPrio) (Object) this.targetCpu).setPrio(newPrio);
            this.targetCpu.markDirty();
        }
    }
}
