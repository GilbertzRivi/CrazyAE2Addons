package net.oktawia.crazyae2addons.menus.block;

import appeng.menu.AEBaseMenu;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;

public class AmpereMeterMenu extends AEBaseMenu {

    private static final String CHANGE_DIRECTION = "actionChangeDirection";
    private static final String CHANGE_MIN = "actionChangeMin";
    private static final String CHANGE_MAX = "actionChangeMax";

    @Getter
    private final AmpereMeterBE host;

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
        if (isClientSide()) {
            sendClientAction(CHANGE_DIRECTION, dir);
        }
    }

    public void changeMin(int min) {
        host.setMinFePerTick(min);
        if (isClientSide()) {
            sendClientAction(CHANGE_MIN, min);
        }
    }

    public void changeMax(int max) {
        host.setMaxFePerTick(max);
        if (isClientSide()) {
            sendClientAction(CHANGE_MAX, max);
        }
    }
}