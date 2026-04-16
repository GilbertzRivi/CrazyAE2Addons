package net.oktawia.crazyae2addons.menus.block;

import appeng.menu.AEBaseMenu;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;

public class AmpereMeterMenu extends AEBaseMenu {

    public AmpereMeterBE host;

    public String CHANGE_DIRECTION = "actionChangeDirection";
    public String CHANGE_MIN = "actionChangeMin";
    public String CHANGE_MAX = "actionChangeMax";

    public AmpereMeterMenu(int id, Inventory ip, AmpereMeterBE host) {
        super(CrazyMenuRegistrar.AMPERE_METER_MENU.get(), id, ip, host);
        this.host = host;

        registerClientAction(CHANGE_DIRECTION, Boolean.class, this::changeDirection);
        registerClientAction(CHANGE_MIN, Integer.class, this::changeMin);
        registerClientAction(CHANGE_MAX, Integer.class, this::changeMax);

        this.createPlayerInventorySlots(ip);
    }

    public void changeDirection(boolean dir) {
        host.setDirection(dir);
        if (isClientSide()){
            sendClientAction(CHANGE_DIRECTION, dir);
        }
    }

    public void changeMin(int min) {
        this.host.setMinFePerTick(min);

        if (isClientSide()){
            sendClientAction(CHANGE_MIN, min);
        }
    }

    public void changeMax(int max) {
        this.host.setMaxFePerTick(max);

        if (isClientSide()){
            sendClientAction(CHANGE_MAX, max);
        }
    }
}
