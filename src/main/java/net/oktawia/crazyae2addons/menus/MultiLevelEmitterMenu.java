package net.oktawia.crazyae2addons.menus;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.MultiStorageLevelEmitterPart;
import org.jetbrains.annotations.Nullable;

public class MultiLevelEmitterMenu extends UpgradeableMenu<MultiStorageLevelEmitterPart> {

    public static final int MAX_ROWS = MultiStorageLevelEmitterPart.MAX_RULES;
    public static final int MIN_ROWS = 6;

    public static final String ACT_ADD_ROW = "addRow";
    public static final String ACT_SET_LIMIT = "setLimit";
    public static final String ACT_TOGGLE_COMPARE = "toggleCompare";
    public static final String ACT_TOGGLE_LOGIC = "toggleLogic";

    private static final Gson GSON = new GsonBuilder().create();
    public final MultiStorageLevelEmitterPart host;

    @GuiSync(200)
    public int rows;

    @GuiSync(201)
    public String limitsJson = "";

    @GuiSync(202)
    public String modesJson = "";

    @GuiSync(203)
    public int logicMode; // 0 OR, 1 AND

    private final InternalInventory monitorInv;

    private final long[] limits = new long[MAX_ROWS];
    private final int[] modes = new int[MAX_ROWS];

    private int monitorSlotStart;

    public MultiLevelEmitterMenu(int id, Inventory ip, MultiStorageLevelEmitterPart host) {
        super(CrazyMenuRegistrar.MULTI_LEVEL_EMITTER_MENU.get(), id, ip, host);
        this.host = host;

        this.monitorInv = host.getMonitorInventory();

        this.rows = MIN_ROWS;

        if (!isClientSide()) {
            loadFromHost();
            int inferred = computeRowsFromLastFilled();
            if (rows != inferred) {
                rows = inferred;
                host.setRows(rows);
            }
            syncJson();
        }

        registerClientAction(ACT_ADD_ROW, Integer.class, this::handleAddRow);
        registerClientAction(ACT_SET_LIMIT, String.class, this::handleSetLimit);
        registerClientAction(ACT_TOGGLE_COMPARE, Integer.class, this::handleToggleCompare);
        registerClientAction(ACT_TOGGLE_LOGIC, Integer.class, this::handleToggleLogic);

        this.monitorSlotStart = this.slots.size();
        for (int i = 0; i < MAX_ROWS; i++) {
            this.addSlot(new FakeSlot(monitorInv, i));
        }
    }

    public int getMonitorSlotStart() {
        return monitorSlotStart;
    }

    public int getRows() {
        return rows;
    }

    public int getLogicMode() {
        return logicMode;
    }

    // ---- client requests ----
    public void requestAddRow() {
        if (isClientSide()) sendClientAction(ACT_ADD_ROW, 0);
        else handleAddRow(0);
    }

    public void requestSetLimit(int row, long value) {
        String payload = row + ":" + value;
        if (isClientSide()) sendClientAction(ACT_SET_LIMIT, payload);
        else handleSetLimit(payload);
    }

    public void requestToggleCompare(int row) {
        if (isClientSide()) sendClientAction(ACT_TOGGLE_COMPARE, row);
        else handleToggleCompare(row);
    }

    public void requestToggleLogic() {
        if (isClientSide()) sendClientAction(ACT_TOGGLE_LOGIC, 0);
        else handleToggleLogic(0);
    }

    // ---- handlers (server-side) ----
    private void handleAddRow(Integer ignored) {
        if (isClientSide()) {
            sendClientAction(ACT_ADD_ROW, 0);
            return;
        }
        if (rows >= MAX_ROWS) return;

        rows++;
        host.setRows(rows);
        syncJson();
    }

    private void handleSetLimit(String payload) {
        if (isClientSide()) {
            sendClientAction(ACT_SET_LIMIT, payload);
            return;
        }

        int sep = payload.indexOf(':');
        if (sep <= 0) return;

        int row;
        long value;
        try {
            row = Integer.parseInt(payload.substring(0, sep));
            value = Long.parseLong(payload.substring(sep + 1));
        } catch (Exception ignored) {
            return;
        }

        if (row < 0 || row >= MAX_ROWS) return;

        value = Math.max(0, value);
        limits[row] = value;
        host.setLimit(row, value);

        syncJson();
    }

    private void handleToggleCompare(Integer row) {
        if (isClientSide()) {
            sendClientAction(ACT_TOGGLE_COMPARE, row);
            return;
        }
        if (row == null || row < 0 || row >= MAX_ROWS) return;

        modes[row] = (modes[row] == 0) ? 1 : 0;

        host.setCompareMode(row, modes[row] == 1
                ? MultiStorageLevelEmitterPart.CompareMode.BELOW
                : MultiStorageLevelEmitterPart.CompareMode.ABOVE_OR_EQUAL);

        syncJson();
    }

    private void handleToggleLogic(Integer ignored) {
        if (isClientSide()) {
            sendClientAction(ACT_TOGGLE_LOGIC, 0);
            return;
        }

        logicMode = (logicMode == 0) ? 1 : 0;

        host.setLogicMode(logicMode == 1
                ? MultiStorageLevelEmitterPart.LogicMode.AND
                : MultiStorageLevelEmitterPart.LogicMode.OR);

        syncJson();
    }

    private void loadFromHost() {
        rows = Math.max(MIN_ROWS, Math.min(host.getRows(), MAX_ROWS));
        logicMode = (host.getLogicMode() == MultiStorageLevelEmitterPart.LogicMode.AND) ? 1 : 0;

        for (int i = 0; i < MAX_ROWS; i++) {
            limits[i] = host.getLimit(i);
            modes[i] = host.getCompareMode(i) == MultiStorageLevelEmitterPart.CompareMode.BELOW ? 1 : 0;
        }
    }

    private void syncJson() {
        int r = Math.max(0, Math.min(rows, MAX_ROWS));

        long[] l = new long[r];
        int[] m = new int[r];
        for (int i = 0; i < r; i++) {
            l[i] = limits[i];
            m[i] = modes[i];
        }

        limitsJson = GSON.toJson(l);
        modesJson = GSON.toJson(m);
    }

    private ItemStack safeGetFilterStack(int slot) {
        try {
            ItemStack s = monitorInv.getStackInSlot(slot);
            return s == null ? ItemStack.EMPTY : s;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private int computeRowsFromLastFilled() {
        int last = -1;
        for (int i = 0; i < MAX_ROWS; i++) {
            if (!safeGetFilterStack(i).isEmpty()) last = i;
        }
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, last + 1));
    }

    @Override
    public void removed(Player player) {
        if (!isClientSide()) {
            int inferred = computeRowsFromLastFilled();
            if (rows != inferred) {
                rows = inferred;
                host.setRows(rows);
                syncJson();
            }
        }
        super.removed(player);
    }
}
