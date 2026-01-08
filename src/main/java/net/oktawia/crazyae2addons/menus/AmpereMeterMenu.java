package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.util.BlockUpdate;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;


public class AmpereMeterMenu extends AEBaseMenu {

    public AmpereMeterBE host;
    @GuiSync(48)
    public boolean direction;
    @GuiSync(49)
    public String transfer = "-";
    @GuiSync(51)
    public String unit = "-";

    @GuiSync(52)
    public int minFePerTick = 0;
    @GuiSync(53)
    public int maxFePerTick = 0;

    public String CHANGE_DIRECTION = "actionChangeDirection";
    public String CHANGE_MIN = "actionChangeMin";
    public String CHANGE_MAX = "actionChangeMax";

    public AmpereMeterMenu(int id, Inventory ip, AmpereMeterBE host) {
        super(CrazyMenuRegistrar.AMPERE_METER_MENU.get(), id, ip, host);
        this.host = host;
        this.direction = host.direction;
        this.minFePerTick = host.minFePerTick;
        this.maxFePerTick = host.maxFePerTick;

        this.host.setMenu(this);

        registerClientAction(CHANGE_DIRECTION, Boolean.class, this::changeDirection);
        registerClientAction(CHANGE_MIN, Integer.class, this::changeMin);
        registerClientAction(CHANGE_MAX, Integer.class, this::changeMax);
    }

    public void changeDirection(boolean dir) {
        this.host.direction = dir;
        this.direction = dir;

        // Natychmiast wyzeruj pomiar i odśwież komparator po zmianie strony.
        this.host.resetTransfer();

        if (isClientSide()){
            sendClientAction(CHANGE_DIRECTION, dir);
        } else if (host.getLevel() != null) {
            host.setChanged();
            host.getLevel().sendBlockUpdated(host.getBlockPos(), host.getBlockState(), host.getBlockState(), 3);
        }
    }

    public void changeMin(int min) {
        if (min < 0) min = 0;
        if (min > this.host.maxFePerTick) min = this.host.maxFePerTick;

        this.host.minFePerTick = min;
        this.minFePerTick = min;

        if (isClientSide()){
            sendClientAction(CHANGE_MIN, min);
        } else if (host.getLevel() != null) {
            host.setChanged();
            host.updateComparator();
            host.getLevel().sendBlockUpdated(host.getBlockPos(), host.getBlockState(), host.getBlockState(), 3);
        }
    }

    public void changeMax(int max) {
        if (max < 0) max = 0;
        if (max < this.host.minFePerTick) max = this.host.minFePerTick;

        this.host.maxFePerTick = max;
        this.maxFePerTick = max;

        if (isClientSide()){
            sendClientAction(CHANGE_MAX, max);
        } else if (host.getLevel() != null) {
            host.setChanged();
            host.updateComparator();
            host.getLevel().sendBlockUpdated(host.getBlockPos(), host.getBlockState(), host.getBlockState(), 3);
        }
    }
}
