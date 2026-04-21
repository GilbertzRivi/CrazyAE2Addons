package net.oktawia.crazyae2addons.menus.part;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.RedstoneEmitter;

public class RedstoneEmitterMenu extends AEBaseMenu {

    private static final String ACTION_NAME = "actionSyncName";

    public final RedstoneEmitter host;

    @GuiSync(48)
    public String name;

    public RedstoneEmitterMenu(int id, Inventory ip, RedstoneEmitter host) {
        super(CrazyMenuRegistrar.REDSTONE_EMITTER_MENU.get(), id, ip, host);
        this.host = host;
        this.name = host.getNameId();
        registerClientAction(ACTION_NAME, String.class, this::changeName);
        createPlayerInventorySlots(ip);
    }

    public void changeName(String name) {
        this.host.setName(name);
        this.name = name;
        if (isClientSide()) {
            sendClientAction(ACTION_NAME, name);
        }
    }
}
