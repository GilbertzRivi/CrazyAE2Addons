package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.parts.DisplayPart;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;


public class DisplayMenu extends AEBaseMenu {

    @GuiSync(145)
    public String displayValue;
    @GuiSync(29)
    public boolean mode;
    @GuiSync(31)
    public boolean margin;
    @GuiSync(32)
    public boolean centerText;

    public String ACTION_SYNC_DISPLAY_VALUE = "syncDisplayValue";
    public String MODE = "changeMode";
    public String MARGIN = "changeMargin";
    public String CENTERTEXT = "changeCenter";
    public DisplayPart host;

    public DisplayMenu(int id, Inventory ip, DisplayPart host) {
        super(CrazyMenuRegistrar.DISPLAY_MENU.get(), id, ip, host);
        this.host = host;
        this.displayValue = host.textValue;
        this.mode = host.mode;
        this.margin = host.margin;
        this.centerText = host.center;
        registerClientAction(ACTION_SYNC_DISPLAY_VALUE, String.class, this::syncValue);
        registerClientAction(MODE, Boolean.class, this::changeMode);
        registerClientAction(MARGIN, Boolean.class, this::changeMargin);
        registerClientAction(CENTERTEXT, Boolean.class, this::changeCenter);
        createPlayerInventorySlots(ip);
    }

    public void syncValue(String value) {
        this.displayValue = value;
        host.updateController(value);
        if (isClientSide()){
            sendClientAction(ACTION_SYNC_DISPLAY_VALUE, value);
        }
        this.host.getHost().markForSave();
    }

    public void changeMode(boolean btn) {
        this.mode = btn;
        host.mode = btn;
        if (isClientSide()){
            sendClientAction(MODE, btn);
        }
    }

    public void changeCenter(boolean btn) {
        this.centerText = btn;
        host.center = btn;
        if (isClientSide()){
            sendClientAction(CENTERTEXT, btn);
        }
    }

    public void changeMargin(boolean btn) {
        this.margin = btn;
        host.margin = btn;
        if (isClientSide()){
            sendClientAction(MARGIN, btn);
        }
    }
}
