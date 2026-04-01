package net.oktawia.crazyae2addons.menus;

import appeng.api.stacks.AmountFormat;
import appeng.api.util.IConfigManager;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;

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
    @GuiSync(932)
    public boolean skipEmpty = false;

    private static final String MISSING = "actionUpdateMissing";
    private static final String OFFSET = "actionUpdateOffset";
    private static final String TOGGLE_PREVIEW = "actionTogglePreview";

    public AutoBuilderMenu(int id, Inventory playerInventory, AutoBuilderBE host) {
        super(CrazyMenuRegistrar.AUTO_BUILDER_MENU.get(), id, playerInventory, host);

        getHost().setMenu(this);

        this.xax = host.offset.getX();
        this.yax = host.offset.getY();
        this.zax = host.offset.getZ();
        this.skipEmpty = host.skipEmpty;
        this.preview = host.isPreviewEnabled();

        if (isServerSide() && host.missingItems != null) {
            this.missingItem = String.format(
                    "%sx %s",
                    host.missingItems.what().formatAmount(host.missingItems.amount(), AmountFormat.SLOT),
                    host.missingItems.what().toString()
            );
        }

        this.registerClientAction(MISSING, Boolean.class, this::updateMissing);
        this.registerClientAction(OFFSET, String.class, this::syncOffset);
        this.registerClientAction(TOGGLE_PREVIEW, this::togglePreview);

        getHost().loadCode();
        getHost().recalculateRequiredEnergy();
        pushEnergyDisplay();
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            var missing = getHost().missingItems;
            if (missing != null) {
                this.missingItem = String.format("%sx %s",
                        missing.what().formatAmount(missing.amount(), AmountFormat.SLOT),
                        missing.what().toString());
            } else {
                this.missingItem = "";
            }
            pushEnergyDisplay();
        }
        super.broadcastChanges();
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        // PatternProviderLogic only registers BLOCKING_MODE, PATTERN_ACCESS_TERMINAL,
        // and LOCK_CRAFTING_MODE — not FUZZY_MODE or REDSTONE_CONTROLLED
    }

    @Override
    protected void setupConfig() {
        this.addSlot(new AppEngSlot(getHost().inventory, 0), SlotSemantics.CONFIG);
        this.addSlot(new AppEngSlot(getHost().inventory, 1), SlotSemantics.CONFIG);
    }

    public void updateMissing(boolean selected) {
        getHost().skipEmpty = selected;
        this.skipEmpty = selected;
        if (isClientSide()) {
            sendClientAction(MISSING, selected);
        }
    }

    public void pushEnergyDisplay() {
        if (isServerSide()) {
            double ae = Math.max(0, getHost().getRequiredEnergyAE());
            this.energyNeededText = (LangDefs.ENERGY_NEEDED.getEnglishText() + String.format("%, .0f AE", ae)).replace('\u00A0', ' ');
        }
    }

    public void syncOffset() {
        syncOffset("%s|%s|%s".formatted(xax, yax, zax));
    }

    public void syncOffset(String offset) {
        xax = Integer.parseInt(offset.split("\\|")[0]);
        yax = Integer.parseInt(offset.split("\\|")[1]);
        zax = Integer.parseInt(offset.split("\\|")[2]);

        getHost().offset = new BlockPos(xax, yax, zax);
        getHost().onOffsetChanged();
        getHost().recalculateRequiredEnergy();
        pushEnergyDisplay();
        if (isClientSide()) {
            sendClientAction(OFFSET, offset);
            getHost().setPreviewDirty(true);
            getHost().setPreviewInfo(null);
        }
    }

    public void togglePreview() {
        getHost().togglePreview();
        this.preview = getHost().isPreviewEnabled();
        if (isClientSide()) {
            sendClientAction(TOGGLE_PREVIEW);
        }
    }
}
