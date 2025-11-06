package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.VariableTerminalPart;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

public class VariableTerminalMenu extends AEBaseMenu {

    private final VariableTerminalPart terminal;

    @GuiSync(0)
    public String serializedVariables = "";

    public static final String ACTION_ADD = "addVariable";
    public static final String ACTION_REMOVE = "removeVariable";

    public VariableTerminalMenu(int id, Inventory inv, VariableTerminalPart host) {
        super(CrazyMenuRegistrar.VARIABLE_TERMINAL_MENU.get(), id, inv, host);
        createPlayerInventorySlots(inv);
        this.terminal = host;

        if (!isClientSide()) {
            this.loadVariables();
        }

        registerClientAction(ACTION_ADD, String.class, this::addVariable);
        registerClientAction(ACTION_REMOVE, String.class, this::removeVariable);
    }

    public void addVariable(String payload) {
        if (isClientSide()) {
            sendClientAction(ACTION_ADD, payload);
            return;
        }

        String[] parts = payload.split("=", 2);
        if (parts.length == 2) {
            String name = parts[0];
            String value = parts[1];
            terminal.findController().ifPresent(controller -> {
                controller.addVariable(VariableTerminalPart.randomHexId(), VariableTerminalPart.class, VariableTerminalPart.hexId, name, value);
                loadVariables();
            });
        }
    }

    public void removeVariable(String idAndName) {
        if (isClientSide()) {
            sendClientAction(ACTION_REMOVE, idAndName);
            return;
        }

        String[] parts = idAndName.split("\\|", 2);
        if (parts.length == 2) {
            String id = parts[0];
            String name = parts[1];

            terminal.findController().ifPresent(controller -> {
                controller.removeVariable(id, name);
                loadVariables();
            });
        }
    }


    private void loadVariables() {
        terminal.findController().ifPresent(controller -> {
            StringBuilder sb = new StringBuilder();
            controller.variables.forEach((id, var) -> {
                sb.append(var.id()).append(":").append(var.name()).append("=").append(var.value()).append("|");
            });
            this.serializedVariables = sb.toString();
        });
    }


    public List<Triple<String, String, String>> getVariableList() {
        List<Triple<String, String, String>> result = new ArrayList<>();
        if (serializedVariables == null || serializedVariables.isEmpty()) return result;

        String[] entries = serializedVariables.split("\\|");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;

            int colonIdx = entry.indexOf(':');
            int eqIdx = entry.indexOf('=', colonIdx + 1);
            if (colonIdx == -1 || eqIdx == -1) continue;

            String id = entry.substring(0, colonIdx);
            String name = entry.substring(colonIdx + 1, eqIdx);
            String value = entry.substring(eqIdx + 1);

            result.add(Triple.of(id, name, value));
        }
        return result;
    }


    public VariableTerminalPart getTerminal() {
        return terminal;
    }
}
