package net.oktawia.crazyae2addons.menus;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.misc.UnifiedAutoBuilderSlot;

public class AutoBuilderMenu extends UpgradeableMenu<AutoBuilderBE> {

    @GuiSync(238)
    public int xax;
    @GuiSync(237)
    public int yax;
    @GuiSync(236)
    public int zax;
    @GuiSync(933)
    public boolean preview = false;
    @GuiSync(940)
    public String energyNeededText = "";

    @GuiSync(219)
    public String missingItem = "";
    private String MISSING = "actionUpdateMissing";
    private String OFFSET = "actionUpdateOffset";
    private String TOGGLE_PREVIEW = "actionTogglePreview";
    @GuiSync(932)
    public boolean skipEmpty = false;

    public AutoBuilderMenu(int id, Inventory playerInventory, AutoBuilderBE host) {
        super(CrazyMenuRegistrar.AUTO_BUILDER_MENU.get(), id, playerInventory, host);
        host.setMenu(this);
        pushEnergyDisplay();
        if (isServerSide() && host.missingItems != null){
            this.missingItem = String.format(
                    "%sx %s",
                    host.missingItems.what().formatAmount(host.missingItems.amount(), appeng.api.stacks.AmountFormat.SLOT),
                    host.missingItems.what().toString()
            );
        }
        this.addSlot(new UnifiedAutoBuilderSlot(host.inventory, 0), SlotSemantics.CONFIG);
        this.xax = host.offset.getX();
        this.yax = host.offset.getY();
        this.zax = host.offset.getZ();
        this.skipEmpty = host.skipEmpty;
        this.preview = host.isPreviewEnabled();
        this.registerClientAction(MISSING, Boolean.class, this::updateMissing);
        this.registerClientAction(OFFSET, String.class, this::syncOffset);
        this.registerClientAction(TOGGLE_PREVIEW, this::togglePreview);

        getHost().loadCode();
        getHost().recalculateRequiredEnergy();
        pushEnergyDisplay();
    }

    public void updateMissing(boolean selected) {
        if (isClientSide()){
            sendClientAction(MISSING, selected);
            return;
        }
        getHost().skipEmpty = selected;
        this.skipEmpty = selected;
    }

    public void pushEnergyDisplay() {
        if (isServerSide()) {
            double ae = Math.max(0, getHost().getRequiredEnergyAE());
            this.energyNeededText = String.format("Energy needed: %, .0f AE", ae).replace('\u00A0',' ');
        }
    }

    public void syncOffset() {
        syncOffset("%s|%s|%s".formatted(xax, yax, zax));
    }

    public void syncOffset(String offset) {
        if (isClientSide()){
            sendClientAction(OFFSET, offset);
            getHost().setPreviewDirty(true);
            getHost().setPreviewInfo(null);
            return;
        }
        xax = Integer.parseInt(offset.split("\\|")[0]);
        yax = Integer.parseInt(offset.split("\\|")[1]);
        zax = Integer.parseInt(offset.split("\\|")[2]);

        getHost().offset = new BlockPos(xax, yax, zax);
        getHost().recalculateRequiredEnergy();
        pushEnergyDisplay();
        getHost().resetGhostToHome();
    }

    public void togglePreview() {
        if (isClientSide()) {
            sendClientAction(TOGGLE_PREVIEW);
            return;
        }
        getHost().togglePreview();
        this.preview = getHost().isPreviewEnabled();
    }
}
