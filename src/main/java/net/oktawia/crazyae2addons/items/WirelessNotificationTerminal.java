package net.oktawia.crazyae2addons.items;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.util.IConfigManager;
import appeng.util.ConfigInventory;
import appeng.util.ConfigMenuInventory;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.WirelessNotificationTerminalMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.StockThresholdToastPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class WirelessNotificationTerminal extends ItemWT implements IUniversalWirelessTerminalItem {
    private static final String NBT_ROOT = "crazy_notification_monitor";
    private static final String NBT_ROWS = "rows";
    private static final String NBT_THRESHOLDS = "thresholds";
    private static final String NBT_LAST_ABOVE = "lastAbove";
    private static final String NBT_LAST_FILTER_FP = "lastFilterFp";
    private static final String NBT_MONITOR_FILTERS = "crazy_notification_filters";

    private static final int MIN_ROWS = WirelessNotificationTerminalMenu.MIN_ROWS;
    private static final int MAX_ROWS = WirelessNotificationTerminalMenu.MAX_ROWS;

    public WirelessNotificationTerminal() {
        super();
    }

    @Override
    public @NotNull MenuType<?> getMenuType(@NotNull ItemStack stack) {
        return CrazyMenuRegistrar.WIRELESS_NOTIFICATION_TERMINAL_MENU.get();
    }

    public @NotNull IConfigManager getConfigManager(@NotNull ItemStack target) {
        IConfigManager configManager = super.getConfigManager(target);
        configManager.registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        configManager.registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        return configManager;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if ((player.tickCount % 20) != 0) return;
        if (player.containerMenu instanceof WirelessNotificationTerminalMenu) return;
        checkAndNotify(player, stack);
    }

    private void checkAndNotify(ServerPlayer player, ItemStack terminalStack) {
        IGrid grid = this.getLinkedGrid(terminalStack, player.level(), player);
        if (grid == null) return;

        IStorageService storage = grid.getStorageService();
        var cached = storage.getCachedInventory();

        CompoundTag tag = terminalStack.getOrCreateTag();
        CompoundTag root = tag.contains(NBT_ROOT) ? tag.getCompound(NBT_ROOT) : new CompoundTag();

        int savedRows = root.contains(NBT_ROWS) ? root.getInt(NBT_ROWS) : MIN_ROWS;
        int rows = Math.max(MIN_ROWS, Math.min(savedRows, MAX_ROWS));

        long[] thresholds = root.getLongArray(NBT_THRESHOLDS);
        byte[] lastAboveRaw = root.getByteArray(NBT_LAST_ABOVE);

        String[] lastFp = new String[rows];
        Arrays.fill(lastFp, "");

        if (root.contains(NBT_LAST_FILTER_FP, Tag.TAG_LIST)) {
            ListTag list = root.getList(NBT_LAST_FILTER_FP, Tag.TAG_STRING);
            for (int i = 0; i < Math.min(rows, list.size()); i++) {
                lastFp[i] = list.getString(i);
            }
        }

        boolean dirty = false;

        ConfigInventory cfg = ConfigInventory.configTypes(MAX_ROWS, () -> {});
        ConfigMenuInventory inv = new ConfigMenuInventory(cfg);
        if (tag.contains(NBT_MONITOR_FILTERS)) {
            cfg.readFromChildTag(tag, NBT_MONITOR_FILTERS);
        }

        for (int i = 0; i < rows; i++) {
            ItemStack filter = inv.getStackInSlot(i);
            long th = (i < thresholds.length) ? thresholds[i] : 0;

            if (filter.isEmpty() || th <= 0) {
                if (!Objects.equals(lastFp[i], "")) {
                    lastFp[i] = "";
                    dirty = true;
                }
                continue;
            }

            AEKey key = keyFromFilter(filter);
            if (key == null) continue;

            long amount = cached.get(key);
            boolean above = amount >= th;

            boolean lastAbove = (i < lastAboveRaw.length) && lastAboveRaw[i] != 0;

            String fpNow = fingerprint(filter);

            if (!fpNow.equals(lastFp[i]) || i >= lastAboveRaw.length) {
                lastFp[i] = fpNow;
                ensureLastAboveSize(root, rows);
                root.putByteArray(NBT_LAST_ABOVE, setByte(root.getByteArray(NBT_LAST_ABOVE), i, (byte) (above ? 1 : 0)));
                dirty = true;
                continue;
            }

            if (above != lastAbove) {
                root.putByteArray(NBT_LAST_ABOVE, setByte(root.getByteArray(NBT_LAST_ABOVE), i, (byte) (above ? 1 : 0)));
                dirty = true;

                NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new StockThresholdToastPacket(filter.copy(), above, th, amount)
                );
            }
        }

        if (dirty) {
            root.putInt(NBT_ROWS, rows);

            ListTag fpList = new ListTag();
            for (int i = 0; i < rows; i++) fpList.add(StringTag.valueOf(lastFp[i] == null ? "" : lastFp[i]));
            root.put(NBT_LAST_FILTER_FP, fpList);

            tag.put(NBT_ROOT, root);
        }
    }

    private static @Nullable AEKey keyFromFilter(ItemStack filter) {
        var gs = GenericStack.fromItemStack(filter);
        return gs != null ? gs.what() : null;
    }

    private static void ensureLastAboveSize(CompoundTag root, int rows) {
        byte[] la = root.getByteArray(NBT_LAST_ABOVE);
        if (la.length >= rows) return;
        byte[] n = new byte[rows];
        System.arraycopy(la, 0, n, 0, la.length);
        root.putByteArray(NBT_LAST_ABOVE, n);
    }

    private static byte[] setByte(byte[] arr, int idx, byte val) {
        if (idx < 0) return arr;
        if (idx >= arr.length) {
            byte[] n = new byte[idx + 1];
            System.arraycopy(arr, 0, n, 0, arr.length);
            arr = n;
        }
        arr[idx] = val;
        return arr;
    }

    private static String fingerprint(ItemStack s) {
        var id = s.getItem().builtInRegistryHolder().key().location().toString();
        var t = s.getTag();
        return id + "|" + (t == null ? "" : t.toString());
    }
}
