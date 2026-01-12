package net.oktawia.crazyae2addonslite.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.parts.RedstoneEmitterPart;

import java.util.stream.Collectors;

public class RedstoneEmitterMenu extends AEBaseMenu {

    public RedstoneEmitterPart host;
    @GuiSync(48)
    public String name;

    public String NAME = "actionSyncName";
    @GuiSync(432)
    public String emitterNames;

    public RedstoneEmitterMenu(int id, Inventory ip, RedstoneEmitterPart host) {
        super(CrazyMenuRegistrar.REDSTONE_EMITTER_MENU.get(), id, ip, host);
        this.host = host;
        this.name = host.name;
        this.emitterNames = host.getEmitters().stream().map(RedstoneTerminalMenu.EmitterInfo::name).collect(Collectors.joining("|"));
        registerClientAction(NAME, String.class, this::changeName);
        this.createPlayerInventorySlots(ip);
    }

    public void changeName(String name) {
        this.host.name = name;
        this.name = name;
        if (isClientSide()) {
            sendClientAction(NAME, name);
        }
    }
}
