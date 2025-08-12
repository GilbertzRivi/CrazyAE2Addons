package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.PatternManagementUnitControllerBE;

public class PatternManagementUnitControllerMenu extends AEBaseMenu {

    public static final int ROWS = CrazyConfig.COMMON.PatternUnitCapacity.get();
    public String PREVIEW = "actionPrev";
    @GuiSync(893)
    public boolean preview;

    private final PatternManagementUnitControllerBE host;

    public PatternManagementUnitControllerMenu(int id, Inventory ip, PatternManagementUnitControllerBE host) {
        super(CrazyMenuRegistrar.PATTERN_MANAGEMENT_UNIT_CONTROLLER_MENU.get(), id, ip, host);
        this.host = host;

        for (int i = 0; i < host.inv.size(); i++) {
            this.addSlot(new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, host.inv, i), SlotSemantics.ENCODED_PATTERN);
        }

        this.createPlayerInventorySlots(ip);
        this.preview = host.preview;
        this.registerClientAction(PREVIEW, Boolean.class, this::changePreview);
    }

    public void changePreview(Boolean preview) {
        host.preview = preview;
        this.preview = preview;
        if (isClientSide()){
            sendClientAction(PREVIEW, preview);
        }
    }
}
