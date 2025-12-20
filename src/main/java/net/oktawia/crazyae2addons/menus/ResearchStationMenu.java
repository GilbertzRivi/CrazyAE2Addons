package net.oktawia.crazyae2addons.menus;

import appeng.client.gui.Icon;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.OutputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.ResearchStationBE;
import net.oktawia.crazyae2addons.misc.AppEngFilteredSlot;

public class ResearchStationMenu extends AEBaseMenu {
    private final ResearchStationBE host;

    @GuiSync(880) public boolean formed;
    @GuiSync(881) public int energyPct;
    @GuiSync(882) public int waterPct;
    @GuiSync(883) public boolean preview;
    @GuiSync(884) public int progressPct;

    private static final String ACT_UNLOCK_ALL = "unlock_all";
    private final Inventory playerInv;

    public energyProgressProvider energyBar = new energyProgressProvider();
    public recipeProgressProvider recipeBar = new recipeProgressProvider();

    private static final String ACT_TOGGLE_PREVIEW = "toggle_preview";

    public ResearchStationMenu(int id, Inventory playerInventory, ResearchStationBE host) {
        super(CrazyMenuRegistrar.RESEARCH_STATION_MENU.get(), id, playerInventory, host);
        this.host = host;
        this.playerInv = playerInventory;
        this.energyPct = host.getEnergyPct();
        this.progressPct = host.getProgressPct();
        this.preview = host.preview;

        if (host.disk != null) {
            this.addSlot(new AppEngFilteredSlot(host.disk, 0, CrazyItemRegistrar.DATA_DRIVE.get()), SlotSemantics.MACHINE_OUTPUT);
        }

        this.registerClientAction(ACT_TOGGLE_PREVIEW, Boolean.class, this::changePreview);
        this.registerClientAction(ACT_UNLOCK_ALL, this::unlockAllClick);
        this.createPlayerInventorySlots(playerInventory);
    }

    public void changePreview(Boolean preview) {
        host.preview = preview;
        this.preview = preview;
        if (isClientSide()){
            sendClientAction(ACT_TOGGLE_PREVIEW, preview);
        }
    }

    public void unlockAllClick() {
        if (isClientSide()){
            sendClientAction(ACT_UNLOCK_ALL);
        }
        else if (playerInv != null && playerInv.player.isCreative()) {
            host.unlockAllToDisk();
        }
    }

    @Override
    public void broadcastChanges() {
        if (!isClientSide()) {
            this.energyPct = host.getEnergyPct();
            this.progressPct = host.getProgressPct();
            this.preview = host.preview;
        }
        super.broadcastChanges();
    }

    public class recipeProgressProvider implements IProgressProvider {
        @Override public int getCurrentProgress() { return ResearchStationMenu.this.progressPct; }
        @Override public int getMaxProgress() { return 1000; }
    }

    public class energyProgressProvider implements IProgressProvider {

        @Override
        public int getCurrentProgress() {
            return ResearchStationMenu.this.energyPct;
        }

        @Override
        public int getMaxProgress() {
            return ResearchStationBE.MAX_ENERGY;
        }
    }
}
