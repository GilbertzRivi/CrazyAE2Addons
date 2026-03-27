package net.oktawia.crazyae2addons.menus;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.EntropyCradleControllerBE;
import net.oktawia.crazyae2addons.entities.PenroseControllerBE;


public class EntropyCradleControllerMenu extends AEBaseMenu {

    private final EntropyCradleControllerBE host;
    public String PREVIEW = "actionPrev";
    @GuiSync(893)
    public boolean preview;

    public EntropyCradleControllerMenu(int id, Inventory ip, EntropyCradleControllerBE host) {
        super(CrazyMenuRegistrar.ENTROPY_CRADLE_CONTROLLER_MENU.get(), id, ip, host);
        this.createPlayerInventorySlots(ip);
        this.host = host;
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
