package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.DataProcessorBE;
import net.oktawia.crazyae2addons.entities.MEDataControllerBE;

import java.util.*;
import java.util.stream.Collectors;

public class DataProcessorMenu extends AEBaseMenu {

    public static final char SEP = '\u001F';
    public static final String ACTION_SELECT = "selectVar";

    private final DataProcessorBE host;
    private final List<String> variablesServer = new ArrayList<>();

    @GuiSync(0) public String syncedVariables = "";
    @GuiSync(1) public String syncedWatchedVar = "";

    public DataProcessorMenu(int id, Inventory ip, DataProcessorBE host) {
        super(CrazyMenuRegistrar.DATA_PROCESSOR_MENU.get(), id, ip, host);
        this.host = host;

        this.addSlot(new AppEngSlot(host.inv, 0), SlotSemantics.STORAGE);
        this.createPlayerInventorySlots(ip);

        if (!ip.player.level().isClientSide) {
            refreshVariablesFromController();
            this.syncedWatchedVar = host.getWatchedVar() == null ? "" : host.getWatchedVar();
        }

        registerClientAction(ACTION_SELECT, String.class, this::onClientSelectedIndex);
    }

    private void refreshVariablesFromController() {
        this.variablesServer.clear();

        if (host.getMainNode() != null && host.getMainNode().getGrid() != null) {
            host.getMainNode().getGrid().getMachines(MEDataControllerBE.class).stream().findFirst().ifPresent(controller -> {
                Set<String> set = controller.variables.values().stream().map(MEDataControllerBE.VariableRecord::name).collect(Collectors.toSet());
                this.variablesServer.addAll(set);
            });
        }

        this.variablesServer.sort(Comparator.naturalOrder());
        this.syncedVariables = String.join(String.valueOf(SEP), this.variablesServer);
    }

    public void onClientSelectedIndex(String name) {
        if (this.isClientSide()){
            this.sendClientAction(ACTION_SELECT, name);
        }

        host.setWatchedVar(name);
        this.syncedWatchedVar = name;
        refreshVariablesFromController();
    }
}
