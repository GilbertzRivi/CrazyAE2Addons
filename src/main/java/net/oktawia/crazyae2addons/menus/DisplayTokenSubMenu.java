package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.DisplayPart;

public class DisplayTokenSubMenu extends AEBaseMenu implements ISubMenu {

    public static final String ACTION_INSERT = "insertToken";

    private final DisplayPart host;

    public DisplayTokenSubMenu(int id, Inventory inv, DisplayPart host) {
        super(CrazyMenuRegistrar.DISPLAY_TOKEN_SUBMENU.get(), id, inv, host);
        this.host = host;
        registerClientAction(ACTION_INSERT, String.class, this::doInsert);
    }

    @Override
    public DisplayPart getHost() {
        return host;
    }

    public void doInsert(String token) {
        host.pendingInsert = token;
        if (!isClientSide()) {
            host.returnToMainMenu(getPlayer(), this);
        }
        if (isClientSide()) {
            sendClientAction(ACTION_INSERT, token);
        }
    }
}
