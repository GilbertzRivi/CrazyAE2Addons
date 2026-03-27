package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.interfaces.IProgressProvider;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.ResearchUnitBE;

public class ResearchUnitMenu extends AEBaseMenu {
    private final ResearchUnitBE host;

    @GuiSync(880) public boolean formed;
    @GuiSync(881) public int computation;
    @GuiSync(882) public int coolantPct;
    @GuiSync(883) public boolean preview;

    public coolantProgressProvider coolantBar = new coolantProgressProvider();

    private static final String ACT_TOGGLE_PREVIEW = "toggle_preview";

    public ResearchUnitMenu(int id, Inventory playerInventory, ResearchUnitBE host) {
        super(CrazyMenuRegistrar.RESEARCH_UNIT_MENU.get(), id, playerInventory, host);
        this.host = host;
        this.formed = host.formed;
        this.coolantPct = host.getCoolant();
        this.preview = host.preview;
        this.computation = host.getComputation();

        this.registerClientAction(ACT_TOGGLE_PREVIEW, Boolean.class, this::changePreview);
        this.createPlayerInventorySlots(playerInventory);
    }

    public void changePreview(Boolean preview) {
        host.preview = preview;
        this.preview = preview;
        if (isClientSide()){
            sendClientAction(ACT_TOGGLE_PREVIEW, preview);
        }
    }

    @Override
    public void broadcastChanges() {
        if (!isClientSide()) {
            this.formed = host.formed;
            this.coolantPct = host.getCoolant();
            this.preview = host.preview;
        }
        super.broadcastChanges();
    }

    public class coolantProgressProvider implements IProgressProvider {

        @Override
        public int getCurrentProgress() {
            return ResearchUnitMenu.this.coolantPct;
        }

        @Override
        public int getMaxProgress() {
            return 16_000;
        }
    }
}
