package net.oktawia.crazyae2addonslite.menus;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import de.mari_023.ae2wtlib.wct.WCTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.logic.WirelessNotificationTerminalItemLogicHost;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;

public class WirelessNotificationTerminalMenu extends UpgradeableMenu<WirelessNotificationTerminalItemLogicHost> {

    public static final int MAX_ROWS = 32;
    public static final int MIN_ROWS = 6;

    public static final String ACT_ADD_ROW = "addRow";
    public static final String ACT_SET_THRESHOLD = "setThreshold";

    private static final int CHECK_EVERY_TICKS = 20;

    private static final String NBT_ROOT = "crazy_notification_monitor";
    private static final String NBT_ROWS = "rows";
    private static final String NBT_THRESHOLDS = "thresholds";
    private static final String NBT_LAST_ABOVE = "lastAbove";

    private static final Gson GSON = new GsonBuilder().create();

    public final WirelessNotificationTerminalItemLogicHost host;

    public static final MenuType<WirelessNotificationTerminalMenu> TYPE =
            MenuTypeBuilder.create(WirelessNotificationTerminalMenu::new, WirelessNotificationTerminalItemLogicHost.class)
                    .build("wireless_notification_terminal");

    @GuiSync(240)
    public int rows;

    @GuiSync(241)
    public String thresholdsJson = "";

    @GuiSync(242)
    public int toastSeq;

    @GuiSync(243)
    public String toastJson = "";

    private final InternalInventory monitorInv;

    private final long[] thresholds = new long[MAX_ROWS];
    private final boolean[] lastAbove = new boolean[MAX_ROWS];

    private final ItemStack[] lastSeenFilter = new ItemStack[MAX_ROWS];
    private final boolean[] thresholdDirty = new boolean[MAX_ROWS];

    private final ArrayDeque<ToastPayload> pendingToasts = new ArrayDeque<>();

    private int monitorSlotStart;
    private int tickCounter;

    public WirelessNotificationTerminalMenu(int id, Inventory ip, WirelessNotificationTerminalItemLogicHost host) {
        super(CrazyMenuRegistrar.WIRELESS_NOTIFICATION_TERMINAL_MENU.get(), id, ip, host);
        this.host = host;

        this.monitorInv = host.getMonitorInventory();

        this.rows = MIN_ROWS;

        if (!isClientSide()) {
            loadFromItemNbt();

            int inferred = computeRowsFromLastFilled();
            if (this.rows != inferred) {
                this.rows = inferred;
                saveToItemNbt();
            }

            syncThresholdsJson();
        }

        registerClientAction(ACT_ADD_ROW, Integer.class, this::handleAddRow);
        registerClientAction(ACT_SET_THRESHOLD, String.class, this::handleSetThreshold);

        this.monitorSlotStart = this.slots.size();
        for (int i = 0; i < MAX_ROWS; i++) {
            this.addSlot(new FakeSlot(monitorInv, i));
            lastSeenFilter[i] = ItemStack.EMPTY;
            thresholdDirty[i] = false;
        }

        this.addSlot(
                new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                        this.host.getSubInventory(WCTMenuHost.INV_SINGULARITY),
                        0
                ),
                AE2wtlibSlotSemantics.SINGULARITY
        );
    }

    public int getMonitorSlotStart() {
        return monitorSlotStart;
    }

    public int getRows() {
        return rows;
    }

    public void requestAddRow() {
        if (isClientSide()) {
            sendClientAction(ACT_ADD_ROW, 0);
        } else {
            handleAddRow(0);
        }
    }

    public void requestSetThreshold(int row, long value) {
        String payload = row + ":" + value;
        if (isClientSide()) {
            sendClientAction(ACT_SET_THRESHOLD, payload);
        } else {
            handleSetThreshold(payload);
        }
    }

    public boolean isWUT() {
        return this.host.getItemStack().getItem() instanceof ItemWUT;
    }

    private void handleAddRow(Integer ignored) {
        if (isClientSide()) {
            sendClientAction(ACT_ADD_ROW, 0);
            return;
        }
        if (rows >= MAX_ROWS) return;

        rows++;
        syncThresholdsJson();
        saveToItemNbt();
    }

    private void handleSetThreshold(String payload) {
        if (isClientSide()) {
            sendClientAction(ACT_SET_THRESHOLD, payload);
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

        thresholds[row] = Math.max(0, value);
        thresholdDirty[row] = true;
        syncThresholdsJson();
        saveToItemNbt();
    }

    private void syncThresholdsJson() {
        long[] visible = new long[Math.max(0, rows)];
        for (int i = 0; i < visible.length; i++) visible[i] = thresholds[i];
        thresholdsJson = GSON.toJson(visible);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (isClientSide()) return;

        tickCounter++;
        if (tickCounter % CHECK_EVERY_TICKS != 0) {
            dispatchOneToast();
            return;
        }

        IGrid grid = tryGetGrid();
        if (grid == null) {
            dispatchOneToast();
            return;
        }

        IStorageService storage = grid.getStorageService();
        var cached = storage.getCachedInventory();

        for (int i = 0; i < rows; i++) {
            ItemStack filter = safeGetFilterStack(i);
            long th = thresholds[i];

            if (filter.isEmpty() || th <= 0) {
                lastSeenFilter[i] = ItemStack.EMPTY;
                thresholdDirty[i] = false;
                continue;
            }

            boolean filterChanged = lastSeenFilter[i].isEmpty()
                    || !ItemStack.isSameItemSameTags(lastSeenFilter[i], filter);

            AEKey key = keyFromFilter(filter);
            if (key == null) {
                lastSeenFilter[i] = filter.copy();
                thresholdDirty[i] = false;
                continue;
            }

            long amount = cached.get(key);
            boolean above = amount >= th;

            if (filterChanged || thresholdDirty[i]) {
                lastAbove[i] = above;
                lastSeenFilter[i] = filter.copy();
                thresholdDirty[i] = false;
                saveToItemNbt();
                continue;
            }

            if (above != lastAbove[i]) {
                lastAbove[i] = above;
                pendingToasts.add(new ToastPayload(i, above, th, amount));
                saveToItemNbt();
            }
        }

        dispatchOneToast();
    }

    private static @Nullable AEKey keyFromFilter(ItemStack filter) {
        var gs = GenericStack.fromItemStack(filter);
        return gs != null ? gs.what() : null;
    }

    private ItemStack safeGetFilterStack(int slot) {
        try {
            ItemStack s = monitorInv.getStackInSlot(slot);
            return s == null ? ItemStack.EMPTY : s;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private void dispatchOneToast() {
        if (pendingToasts.isEmpty()) return;
        ToastPayload p = pendingToasts.pollFirst();
        toastSeq++;
        toastJson = GSON.toJson(p);
    }

    @Nullable
    private IGrid tryGetGrid() {
        if (!(this.host.getItemStack().getItem() instanceof ItemWT wt)) return null;
        return wt.getLinkedGrid(this.host.getItemStack(), this.getPlayer().level(), this.getPlayer());
    }

    private int computeRowsFromLastFilled() {
        int last = -1;
        for (int i = 0; i < MAX_ROWS; i++) {
            if (!safeGetFilterStack(i).isEmpty()) last = i;
        }
        return Math.max(MIN_ROWS, last + 1);
    }

    private void trimRowsNow() {
        int target = computeRowsFromLastFilled();
        if (target != rows) {
            rows = target;
            syncThresholdsJson();
        }
    }

    private void loadFromItemNbt() {
        CompoundTag tag = host.getItemStack().getOrCreateTag();
        if (!tag.contains(NBT_ROOT)) {
            rows = MIN_ROWS;
            Arrays.fill(thresholds, 0);
            Arrays.fill(lastAbove, false);
            return;
        }

        CompoundTag root = tag.getCompound(NBT_ROOT);

        int r = root.getInt(NBT_ROWS);
        rows = Math.max(MIN_ROWS, Math.min(r, MAX_ROWS));

        long[] th = root.getLongArray(NBT_THRESHOLDS);
        for (int i = 0; i < MAX_ROWS; i++) thresholds[i] = i < th.length ? th[i] : 0;

        byte[] la = root.getByteArray(NBT_LAST_ABOVE);
        for (int i = 0; i < MAX_ROWS; i++) lastAbove[i] = i < la.length && la[i] != 0;
    }

    private void saveToItemNbt() {
        CompoundTag tag = host.getItemStack().getOrCreateTag();
        CompoundTag root = new CompoundTag();

        root.putInt(NBT_ROWS, rows);

        long[] th = new long[rows];
        for (int i = 0; i < rows; i++) th[i] = thresholds[i];
        root.putLongArray(NBT_THRESHOLDS, th);

        byte[] la = new byte[rows];
        for (int i = 0; i < rows; i++) la[i] = (byte) (lastAbove[i] ? 1 : 0);
        root.putByteArray(NBT_LAST_ABOVE, la);

        tag.put(NBT_ROOT, root);
    }

    @Override
    public void removed(Player player) {
        if (!isClientSide()) {
            trimRowsNow();
            saveToItemNbt();
        }
        super.removed(player);
    }

    public static final class ToastPayload {
        public int row;
        public boolean above;
        public long threshold;
        public long amount;

        public ToastPayload(int row, boolean above, long threshold, long amount) {
            this.row = row;
            this.above = above;
            this.threshold = threshold;
            this.amount = amount;
        }
    }
}
