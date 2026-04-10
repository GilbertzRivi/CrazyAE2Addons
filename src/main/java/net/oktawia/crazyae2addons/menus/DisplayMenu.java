package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.DisplayPart;

public class DisplayMenu extends AEBaseMenu {

    public static final String ACTION_SYNC = "syncDisplayValue";
    public static final String ACTION_MODE = "changeMode";
    public static final String ACTION_MARGIN = "changeMargin";
    public static final String ACTION_CENTER = "changeCenter";
    public static final String ACTION_OPEN_INSERT = "openInsert";

    @GuiSync(145)
    public String displayValue = "";
    @GuiSync(29)
    public boolean mode;
    @GuiSync(31)
    public boolean margin;
    @GuiSync(32)
    public boolean centerText;
    @GuiSync(33)
    public String pendingInsert = "";
    @GuiSync(34)
    public int pendingInsertCursor = -1;

    public final DisplayPart host;

    public DisplayMenu(int id, Inventory inv, DisplayPart host) {
        super(CrazyMenuRegistrar.DISPLAY_MENU.get(), id, inv, host);
        this.host = host;
        this.displayValue = host.textValue;
        this.mode         = host.mode;
        this.margin       = host.margin;
        this.centerText   = host.center;

        String pending = host.pendingInsert;
        if (pending != null) {
            this.pendingInsert = pending;
            this.pendingInsertCursor = host.pendingInsertCursor;
            host.pendingInsert = null;
            host.pendingInsertCursor = -1;
        }

        registerClientAction(ACTION_SYNC,        String.class,  this::syncValue);
        registerClientAction(ACTION_MODE,         Boolean.class, this::changeMode);
        registerClientAction(ACTION_MARGIN,       Boolean.class, this::changeMargin);
        registerClientAction(ACTION_CENTER,       Boolean.class, this::changeCenter);
        registerClientAction(ACTION_OPEN_INSERT,  Integer.class, this::openInsert);
        createPlayerInventorySlots(inv);
    }

    public void syncValue(String value) {
        this.displayValue = value;
        host.textValue = value;
        host.getHost().markForSave();
        host.getHost().markForUpdate();
        if (isClientSide()) sendClientAction(ACTION_SYNC, value);
    }

    public void changeMode(boolean v) {
        this.mode = v; host.mode = v;
        host.getHost().markForUpdate();
        if (isClientSide()) sendClientAction(ACTION_MODE, v);
    }

    public void changeMargin(boolean v) {
        this.margin = v; host.margin = v;
        host.getHost().markForUpdate();
        if (isClientSide()) sendClientAction(ACTION_MARGIN, v);
    }

    public void changeCenter(boolean v) {
        this.centerText = v; host.center = v;
        host.getHost().markForUpdate();
        if (isClientSide()) sendClientAction(ACTION_CENTER, v);
    }

    public void openInsert(int cursorPos) {
        host.pendingInsertCursor = cursorPos;
        if (!isClientSide()) {
            appeng.menu.MenuOpener.open(
                    CrazyMenuRegistrar.DISPLAY_SUBMENU.get(),
                    getPlayer(),
                    appeng.menu.locator.MenuLocators.forPart(host)
            );
        }
        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_INSERT, cursorPos);
        }
    }
}
