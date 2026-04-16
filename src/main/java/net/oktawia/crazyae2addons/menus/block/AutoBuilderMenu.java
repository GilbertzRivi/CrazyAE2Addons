package net.oktawia.crazyae2addons.menus.block;

import appeng.api.util.IConfigManager;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;

public class AutoBuilderMenu extends UpgradeableMenu<AutoBuilderBE> {

    public int xax;
    public int yax;
    public int zax;
    @GuiSync(941)
    public boolean skipEmptyLocked = false;

    private static final String MISSING = "actionUpdateMissing";
    private static final String OFFSET = "actionUpdateOffset";
    private static final String TOGGLE_PREVIEW = "actionTogglePreview";

    public AutoBuilderMenu(int id, Inventory playerInventory, AutoBuilderBE host) {
        super(CrazyMenuRegistrar.AUTO_BUILDER_MENU.get(), id, playerInventory, host);

        this.xax = host.offset.getX();
        this.yax = host.offset.getY();
        this.zax = host.offset.getZ();

        this.registerClientAction(MISSING, Boolean.class, this::updateMissing);
        this.registerClientAction(OFFSET, String.class, this::syncOffset);
        this.registerClientAction(TOGGLE_PREVIEW, this::togglePreview);

        getHost().loadCode();
        getHost().updateSkipEmptyFromCode();
        this.skipEmptyLocked = getHost().skipEmpty;
        getHost().recalculateRequiredEnergy();

        this.addSlot(new AppEngSlot(getHost().inventory, 0), SlotSemantics.CONFIG);
        this.addSlot(new AppEngSlot(getHost().inventory, 1), SlotSemantics.CONFIG);
    }

    public void updateMissing(boolean selected) {
        getHost().skipEmpty = selected;
        if (isClientSide()) {
            sendClientAction(MISSING, selected);
        }
    }

    public void syncOffset() {
        syncOffset("%s|%s|%s".formatted(xax, yax, zax));
    }

    public void syncOffset(String offset) {
        xax = Integer.parseInt(offset.split("\\|")[0]);
        yax = Integer.parseInt(offset.split("\\|")[1]);
        zax = Integer.parseInt(offset.split("\\|")[2]);

        BlockPos oldOffset = getHost().offset;
        getHost().offset = new BlockPos(xax, yax, zax);
        getHost().onOffsetChanged(oldOffset);
        getHost().recalculateRequiredEnergy();
        if (isClientSide()) {
            sendClientAction(OFFSET, offset);
        }
    }

    public void togglePreview() {
        getHost().togglePreview();
        if (isClientSide()) {
            sendClientAction(TOGGLE_PREVIEW);
        }
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
    }
}
