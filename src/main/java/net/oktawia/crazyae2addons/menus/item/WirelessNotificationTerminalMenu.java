package net.oktawia.crazyae2addons.menus.item;

import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.GenericStack;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.wct.WCTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.logic.wireless.WirelessNotificationTerminalItemLogicHost;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.WirelessNotificationWindowPacket;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WirelessNotificationTerminalMenu extends UpgradeableMenu<WirelessNotificationTerminalItemLogicHost> {

    public record NotificationSlotInfo(@Nullable GenericStack config, long threshold) {}

    public static final int VISIBLE_ROWS = 6;

    private static final String NBT_ROOT = "crazy_notification_monitor";
    private static final String NBT_THRESHOLDS = "thresholds";
    private static final String NBT_HUD_X = "hudX";
    private static final String NBT_HUD_Y = "hudY";
    private static final String NBT_HIDE_ABOVE = "hideAbove";
    private static final String NBT_HIDE_BELOW = "hideBelow";
    private static final String NBT_HUD_SCALE = "hudscale";

    private static final String ACTION_SET_THRESHOLD = "setThreshold";
    private static final String ACTION_SET_HUD_X = "setHudX";
    private static final String ACTION_SET_HUD_Y = "setHudY";
    private static final String ACTION_SET_HIDE_ABOVE = "setHideAbove";
    private static final String ACTION_SET_HIDE_BELOW = "setHideBelow";
    private static final String ACTION_SCROLL = "scroll";
    private static final String ACTION_SET_HUD_SCALE = "sendhudscale";


    public static int getConfiguredSlotCount() {
        return Math.max(0, CrazyConfig.COMMON.WIRELESS_NOTIFICATION_TERMINAL_CONFIG_SLOT.get());
    }

    public final WirelessNotificationTerminalItemLogicHost host;

    @Getter
    @GuiSync(266) public int hudX = 100;

    @Getter
    @GuiSync(267) public int hudY = 0;

    @Getter
    @GuiSync(268) public boolean hideAbove = false;

    @Getter
    @GuiSync(269) public boolean hideBelow = false;

    @Getter
    @GuiSync(270) public int hudScale = 100;

    public int clientWindowOffset = 0;
    public int clientWindowRevision = 0;
    public int totalCount = 0;
    public List<NotificationSlotInfo> clientWindow = List.of();

    private final InternalInventory configInventory;
    private final WindowedNotificationInventory windowedInventory;

    private long[] thresholds = new long[0];
    private int currentOffset = 0;
    private int windowRevision = 0;

    public static final MenuType<WirelessNotificationTerminalMenu> TYPE =
            MenuTypeBuilder.create(WirelessNotificationTerminalMenu::new, WirelessNotificationTerminalItemLogicHost.class)
                    .build("wireless_notification_terminal");

    public WirelessNotificationTerminalMenu(int id, Inventory ip, WirelessNotificationTerminalItemLogicHost host) {
        super(TYPE, id, ip, host);
        this.host = host;

        this.configInventory = host.getConfig().createMenuWrapper();
        this.windowedInventory = new WindowedNotificationInventory();

        ensureThresholdSize();

        if (!isClientSide()) {
            loadFromItemNbt();
        }

        registerClientAction(ACTION_SET_THRESHOLD, String.class, this::setThresholdPayload);
        registerClientAction(ACTION_SET_HUD_X, Integer.class, this::setHudX);
        registerClientAction(ACTION_SET_HUD_Y, Integer.class, this::setHudY);
        registerClientAction(ACTION_SET_HUD_SCALE, Integer.class, this::setHudScale);
        registerClientAction(ACTION_SET_HIDE_ABOVE, Boolean.class, this::setHideAboveServer);
        registerClientAction(ACTION_SET_HIDE_BELOW, Boolean.class, this::setHideBelowServer);
        registerClientAction(ACTION_SCROLL, Integer.class, this::onScroll);

        this.addSlot(
                new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                        this.host.getSubInventory(WCTMenuHost.INV_SINGULARITY),
                        0
                ),
                AE2wtlibSlotSemantics.SINGULARITY
        );

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            this.addSlot(new FakeSlot(windowedInventory, i), SlotSemantics.CONFIG);
        }
    }

    @Override
    public void onSlotChange(Slot s) {
        super.onSlotChange(s);
        if (!isClientSide()) {
            host.saveChanges();
            refreshWindow();
        }
    }

    public boolean isWUT() {
        return this.host.getItemStack().getItem() instanceof ItemWUT;
    }

    public void applyClientWindow(WirelessNotificationWindowPacket pkt) {
        this.totalCount = pkt.totalCount();
        this.clientWindowOffset = pkt.windowOffset();
        this.clientWindowRevision = pkt.revision();
        this.clientWindow = pkt.window();
        this.windowedInventory.setClientCache(pkt.window());
    }

    public void onScroll(int offset) {
        if (isClientSide()) {
            sendClientAction(ACTION_SCROLL, offset);
            return;
        }

        this.currentOffset = Math.max(0, offset);
        clampOffset();
        sendWindow();
    }

    public void setThreshold(int slot, long value) {
        if (slot < 0 || slot >= getConfiguredSlotCount()) {
            return;
        }

        long clamped = Math.max(0L, value);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_THRESHOLD, slot + "|" + clamped);
            return;
        }

        setThresholdValue(slot, clamped);
        saveToItemNbt();
        sendWindow();
    }

    public void setHudX(int v) {
        this.hudX = Math.max(0, Math.min(100, v));
        if (isClientSide()) {
            sendClientAction(ACTION_SET_HUD_X, this.hudX);
        } else {
            saveToItemNbt();
        }
    }

    public void setHudY(int v) {
        this.hudY = Math.max(0, Math.min(100, v));
        if (isClientSide()) {
            sendClientAction(ACTION_SET_HUD_Y, this.hudY);
        } else {
            saveToItemNbt();
        }
    }

    public void setHideAbove(boolean v) {
        this.hideAbove = v;
        if (isClientSide()) {
            sendClientAction(ACTION_SET_HIDE_ABOVE, v);
        } else {
            saveToItemNbt();
        }
    }

    public void setHideBelow(boolean v) {
        this.hideBelow = v;
        if (isClientSide()) {
            sendClientAction(ACTION_SET_HIDE_BELOW, v);
        } else {
            saveToItemNbt();
        }
    }

    private void setThresholdPayload(String payload) {
        if (isClientSide() || payload == null || payload.isBlank()) {
            return;
        }

        String[] parts = payload.split("\\|", 2);
        if (parts.length != 2) {
            return;
        }

        try {
            int slot = Integer.parseInt(parts[0].trim());
            long value = Long.parseLong(parts[1].trim());

            if (slot < 0 || slot >= getConfiguredSlotCount()) {
                return;
            }

            setThresholdValue(slot, Math.max(0L, value));
            saveToItemNbt();
            sendWindow();
        } catch (NumberFormatException ignored) {
        }
    }

    private void setHideAboveServer(Boolean v) {
        if (isClientSide()) {
            return;
        }
        this.hideAbove = v != null && v;
        saveToItemNbt();
    }

    private void setHideBelowServer(Boolean v) {
        if (isClientSide()) {
            return;
        }
        this.hideBelow = v != null && v;
        saveToItemNbt();
    }

    private void ensureThresholdSize() {
        int size = getConfiguredSlotCount();
        if (thresholds.length != size) {
            thresholds = Arrays.copyOf(thresholds, size);
        }
    }

    private long getThresholdValue(int slot) {
        ensureThresholdSize();
        if (slot < 0 || slot >= thresholds.length) {
            return 0L;
        }
        return thresholds[slot];
    }

    private void setThresholdValue(int slot, long value) {
        ensureThresholdSize();
        if (slot < 0 || slot >= thresholds.length) {
            return;
        }
        thresholds[slot] = Math.max(0L, value);
    }

    private void clampOffset() {
        int total = getConfiguredSlotCount();
        currentOffset = Math.max(0, Math.min(currentOffset, Math.max(0, total - VISIBLE_ROWS)));
    }

    private void refreshWindow() {
        if (isClientSide()) {
            return;
        }
        ensureThresholdSize();
        clampOffset();
        sendWindow();
    }

    private void sendWindow() {
        if (!(getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        int total = getConfiguredSlotCount();
        int from = currentOffset;
        int to = Math.min(from + VISIBLE_ROWS, total);

        var window = new ArrayList<NotificationSlotInfo>(Math.max(0, to - from));
        for (int i = from; i < to; i++) {
            window.add(new NotificationSlotInfo(host.getConfig().getStack(i), getThresholdValue(i)));
        }

        this.totalCount = total;

        NetworkHandler.sendToPlayer(player, new WirelessNotificationWindowPacket(total, from, ++windowRevision, window));
    }

    private void loadFromItemNbt() {
        ensureThresholdSize();

        CompoundTag tag = host.getItemStack().getOrCreateTag();
        CompoundTag root = tag.contains(NBT_ROOT) ? tag.getCompound(NBT_ROOT) : new CompoundTag();

        long[] saved = root.getLongArray(NBT_THRESHOLDS);
        for (int i = 0; i < Math.min(saved.length, thresholds.length); i++) {
            thresholds[i] = Math.max(0L, saved[i]);
        }

        hudX = Math.max(0, Math.min(100, root.getInt(NBT_HUD_X)));
        hudY = Math.max(0, Math.min(100, root.getInt(NBT_HUD_Y)));
        hudScale = Math.max(0, Math.min(100, root.contains(NBT_HUD_SCALE) ? root.getInt(NBT_HUD_SCALE) : 100));

        hideAbove = root.getBoolean(NBT_HIDE_ABOVE);
        hideBelow = root.getBoolean(NBT_HIDE_BELOW);
    }

    private void saveToItemNbt() {
        ensureThresholdSize();

        CompoundTag tag = host.getItemStack().getOrCreateTag();
        CompoundTag root = tag.contains(NBT_ROOT) ? tag.getCompound(NBT_ROOT) : new CompoundTag();

        root.putLongArray(NBT_THRESHOLDS, Arrays.copyOf(thresholds, thresholds.length));
        root.putInt(NBT_HUD_X, Math.max(0, Math.min(100, hudX)));
        root.putInt(NBT_HUD_Y, Math.max(0, Math.min(100, hudY)));
        root.putInt(NBT_HUD_SCALE, Math.max(0, Math.min(100, hudScale)));
        root.putBoolean(NBT_HIDE_ABOVE, hideAbove);
        root.putBoolean(NBT_HIDE_BELOW, hideBelow);

        tag.put(NBT_ROOT, root);
    }

    @Override
    public void removed(Player player) {
        if (!isClientSide()) {
            saveToItemNbt();
        }
        super.removed(player);
    }

    private final class WindowedNotificationInventory implements InternalInventory {

        private final ItemStack[] clientCache = new ItemStack[VISIBLE_ROWS];

        WindowedNotificationInventory() {
            Arrays.fill(clientCache, ItemStack.EMPTY);
        }

        void setClientCache(List<NotificationSlotInfo> window) {
            for (int i = 0; i < VISIBLE_ROWS; i++) {
                if (i < window.size() && window.get(i).config() != null) {
                    clientCache[i] = GenericStack.wrapInItemStack(window.get(i).config());
                } else {
                    clientCache[i] = ItemStack.EMPTY;
                }
            }
        }

        @Override
        public int size() {
            return VISIBLE_ROWS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= VISIBLE_ROWS) {
                return ItemStack.EMPTY;
            }

            if (isClientSide()) {
                return clientCache[slot];
            }

            int globalSlot = currentOffset + slot;
            if (globalSlot < 0 || globalSlot >= getConfiguredSlotCount()) {
                return ItemStack.EMPTY;
            }

            return configInventory.getStackInSlot(globalSlot);
        }

        @Override
        public void setItemDirect(int slot, ItemStack stack) {
            if (slot < 0 || slot >= VISIBLE_ROWS) {
                return;
            }

            if (isClientSide()) {
                clientCache[slot] = stack;
                return;
            }

            int globalSlot = currentOffset + slot;
            if (globalSlot < 0 || globalSlot >= getConfiguredSlotCount()) {
                return;
            }

            configInventory.setItemDirect(globalSlot, stack);
            host.saveChanges();
            refreshWindow();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < 0 || slot >= VISIBLE_ROWS) {
                return false;
            }

            int globalSlot = currentOffset + slot;
            return globalSlot >= 0
                    && globalSlot < getConfiguredSlotCount()
                    && configInventory.isItemValid(globalSlot, stack);
        }

        @Override
        public void sendChangeNotification(int slot) {
            if (!isClientSide()) {
                host.saveChanges();
                refreshWindow();
            }
        }
    }

    public void setHudScale(int v) {
        this.hudScale = Math.max(0, Math.min(100, v));

        if (isClientSide()) {
            sendClientAction(ACTION_SET_HUD_SCALE, this.hudScale);
        } else {
            saveToItemNbt();
        }
    }
}