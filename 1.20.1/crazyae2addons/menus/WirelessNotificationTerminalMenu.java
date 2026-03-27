package net.oktawia.crazyae2addons.menus;

import appeng.api.stacks.AEKey;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.wct.WCTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.WirelessNotificationTerminalItemLogicHost;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class WirelessNotificationTerminalMenu extends AEBaseMenu {

    public static final int FILTER_SLOTS = 16;
    public static final int VISIBLE_ROWS = 6;

    private static final String NBT_ROOT = "crazy_notification_monitor";
    private static final String NBT_THRESHOLDS = "thresholds";
    private static final String NBT_HUD_X = "hudX";
    private static final String NBT_HUD_Y = "hudY";
    private static final String NBT_HIDE_ABOVE = "hideAbove";
    private static final String NBT_HIDE_BELOW = "hideBelow";

    private static final String ACTION_SET_THRESHOLD = "setThresholdPacked";
    private static final String ACTION_SET_HUD_X = "setHudX";
    private static final String ACTION_SET_HUD_Y = "setHudY";
    private static final String ACTION_SET_HIDE_ABOVE = "setHideAbove";
    private static final String ACTION_SET_HIDE_BELOW = "setHideBelow";

    private static final long VALUE_MASK_60 = (1L << 60) - 1L;

    public final WirelessNotificationTerminalItemLogicHost host;

    @GuiSync(250) public long t0;
    @GuiSync(251) public long t1;
    @GuiSync(252) public long t2;
    @GuiSync(253) public long t3;
    @GuiSync(254) public long t4;
    @GuiSync(255) public long t5;
    @GuiSync(256) public long t6;
    @GuiSync(257) public long t7;
    @GuiSync(258) public long t8;
    @GuiSync(259) public long t9;
    @GuiSync(260) public long t10;
    @GuiSync(261) public long t11;
    @GuiSync(262) public long t12;
    @GuiSync(263) public long t13;
    @GuiSync(264) public long t14;
    @GuiSync(265) public long t15;

    @GuiSync(266) public int hudX = 100;
    @GuiSync(267) public int hudY = 0;

    @GuiSync(268) public boolean hideAbove = false;
    @GuiSync(269) public boolean hideBelow = false;

    public static final MenuType<WirelessNotificationTerminalMenu> TYPE =
            MenuTypeBuilder.create(WirelessNotificationTerminalMenu::new, WirelessNotificationTerminalItemLogicHost.class)
                    .build("wireless_notification_terminal");

    public WirelessNotificationTerminalMenu(int id, Inventory ip, WirelessNotificationTerminalItemLogicHost host) {
        super(CrazyMenuRegistrar.WIRELESS_NOTIFICATION_TERMINAL_MENU.get(), id, ip, host);
        this.host = host;

        if (!isClientSide()) {
            loadFromItemNbt();
        }

        registerClientAction(ACTION_SET_THRESHOLD, Long.class, this::setThresholdPacked);
        registerClientAction(ACTION_SET_HUD_X, Integer.class, this::setHudX);
        registerClientAction(ACTION_SET_HUD_Y, Integer.class, this::setHudY);
        registerClientAction(ACTION_SET_HIDE_ABOVE, Boolean.class, this::setHideAboveServer);
        registerClientAction(ACTION_SET_HIDE_BELOW, Boolean.class, this::setHideBelowServer);

        this.addSlot(
                new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                        this.host.getSubInventory(WCTMenuHost.INV_SINGULARITY),
                        0
                ),
                AE2wtlibSlotSemantics.SINGULARITY
        );

        var inv = host.getConfig().createMenuWrapper();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            this.addSlot(new FakeSlot(inv, i), SlotSemantics.CONFIG);
        }

        this.createPlayerInventorySlots(ip);
    }

    public boolean isWUT() {
        return this.host.getItemStack().getItem() instanceof ItemWUT;
    }

    @Nullable
    public AEKey getConfiguredFilter(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return null;
        return host.getConfig().getKey(slot);
    }

    public long getThresholdClient(int slot) {
        return getThresholdField(slot);
    }

    public void setThreshold(int slot, long value) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        if (value < 0) value = 0;

        setThresholdField(slot, value);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_THRESHOLD, packThreshold(slot, value));
        } else {
            saveToItemNbt();
        }
    }

    public int getHudX() { return hudX; }
    public int getHudY() { return hudY; }

    public void setHudX(int v) {
        v = Math.min(100, Math.max(v, 0));
        hudX = v;
        if (isClientSide()) sendClientAction(ACTION_SET_HUD_X, v);
        else saveToItemNbt();
    }

    public void setHudY(int v) {
        v = Math.min(100, Math.max(v, 0));
        hudY = v;
        if (isClientSide()) sendClientAction(ACTION_SET_HUD_Y, v);
        else saveToItemNbt();
    }

    public boolean isHideAbove() { return hideAbove; }
    public boolean isHideBelow() { return hideBelow; }

    public void setHideAbove(boolean v) {
        hideAbove = v;
        if (isClientSide()) sendClientAction(ACTION_SET_HIDE_ABOVE, v);
        else saveToItemNbt();
    }

    public void setHideBelow(boolean v) {
        hideBelow = v;
        if (isClientSide()) sendClientAction(ACTION_SET_HIDE_BELOW, v);
        else saveToItemNbt();
    }

    private void setHideAboveServer(Boolean v) {
        if (isClientSide()) return;
        hideAbove = v != null && v;
        saveToItemNbt();
    }

    private void setHideBelowServer(Boolean v) {
        if (isClientSide()) return;
        hideBelow = v != null && v;
        saveToItemNbt();
    }

    private void setThresholdPacked(long packed) {
        int slot = (int) (packed >>> 60);
        long value = packed & VALUE_MASK_60;

        if (!isClientSide() && slot >= 0 && slot < FILTER_SLOTS) {
            setThresholdField(slot, value);
            saveToItemNbt();
        }
    }

    private static long packThreshold(int slot, long value) {
        if (value < 0) value = 0;
        value &= VALUE_MASK_60;
        return ((long) slot << 60) | value;
    }

    private long getThresholdField(int slot) {
        return switch (slot) {
            case 0 -> t0; case 1 -> t1; case 2 -> t2; case 3 -> t3;
            case 4 -> t4; case 5 -> t5; case 6 -> t6; case 7 -> t7;
            case 8 -> t8; case 9 -> t9; case 10 -> t10; case 11 -> t11;
            case 12 -> t12; case 13 -> t13; case 14 -> t14; case 15 -> t15;
            default -> 0L;
        };
    }

    private void setThresholdField(int slot, long v) {
        if (v < 0) v = 0;
        switch (slot) {
            case 0 -> t0 = v; case 1 -> t1 = v; case 2 -> t2 = v; case 3 -> t3 = v;
            case 4 -> t4 = v; case 5 -> t5 = v; case 6 -> t6 = v; case 7 -> t7 = v;
            case 8 -> t8 = v; case 9 -> t9 = v; case 10 -> t10 = v; case 11 -> t11 = v;
            case 12 -> t12 = v; case 13 -> t13 = v; case 14 -> t14 = v; case 15 -> t15 = v;
        }
    }

    private void loadFromItemNbt() {
        CompoundTag tag = host.getItemStack().getOrCreateTag();
        CompoundTag root = tag.contains(NBT_ROOT) ? tag.getCompound(NBT_ROOT) : new CompoundTag();

        long[] th = Arrays.copyOf(root.getLongArray(NBT_THRESHOLDS), FILTER_SLOTS);
        for (int i = 0; i < FILTER_SLOTS; i++) setThresholdField(i, Math.max(0, th[i]));

        int x = root.getInt(NBT_HUD_X);
        int y = root.getInt(NBT_HUD_Y);
        hudX = Math.min(100, Math.max(x, 0));
        hudY = Math.min(100, Math.max(y, 0));

        hideAbove = root.getBoolean(NBT_HIDE_ABOVE);
        hideBelow = root.getBoolean(NBT_HIDE_BELOW);
    }

    private void saveToItemNbt() {
        CompoundTag tag = host.getItemStack().getOrCreateTag();
        CompoundTag root = tag.contains(NBT_ROOT) ? tag.getCompound(NBT_ROOT) : new CompoundTag();

        long[] th = new long[FILTER_SLOTS];
        for (int i = 0; i < FILTER_SLOTS; i++) th[i] = getThresholdField(i);
        root.putLongArray(NBT_THRESHOLDS, th);

        root.putInt(NBT_HUD_X, Math.min(100, Math.max(hudX, 0)));
        root.putInt(NBT_HUD_Y, Math.min(100, Math.max(hudY, 0)));

        root.putBoolean(NBT_HIDE_ABOVE, hideAbove);
        root.putBoolean(NBT_HIDE_BELOW, hideBelow);

        tag.put(NBT_ROOT, root);
    }

    @Override
    public void removed(Player player) {
        if (!isClientSide()) saveToItemNbt();
        super.removed(player);
    }
}
