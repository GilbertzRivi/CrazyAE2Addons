package net.oktawia.crazyae2addons.items;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEKey;
import appeng.api.util.IConfigManager;
import appeng.util.ConfigInventory;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.WirelessNotificationTerminalItemLogicHost;
import net.oktawia.crazyae2addons.menus.WirelessNotificationTerminalMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.NotificationHudPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class WirelessNotificationTerminal extends ItemWT implements IUniversalWirelessTerminalItem {

    private static final int SLOTS = 16;
    private static final String NBT_CONFIG = WirelessNotificationTerminalItemLogicHost.NBT_CONFIG;
    private static final String NBT_ROOT = "crazy_notification_monitor";
    private static final String NBT_THRESHOLDS = "thresholds";
    private static final String NBT_HUD_X = "hudX";
    private static final String NBT_HUD_Y = "hudY";
    private static final String NBT_HIDE_ABOVE = "hideAbove";
    private static final String NBT_HIDE_BELOW = "hideBelow";

    @Override
    public @NotNull MenuType<?> getMenuType(@NotNull ItemStack stack) {
        return CrazyMenuRegistrar.WIRELESS_NOTIFICATION_TERMINAL_MENU.get();
    }

    @Override
    public @NotNull IConfigManager getConfigManager(@NotNull ItemStack target) {
        IConfigManager cm = super.getConfigManager(target);
        cm.registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        cm.registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        return cm;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if ((player.tickCount % 20) != 0) return;
        if (player.containerMenu instanceof WirelessNotificationTerminalMenu) return;

        sendHudUpdate(player, stack);
    }

    private void sendHudUpdate(ServerPlayer player, ItemStack terminalStack) {
        IGrid grid = this.getLinkedGrid(terminalStack, player.level(), player);
        if (grid == null) return;

        IStorageService storage = grid.getStorageService();
        var cached = storage.getCachedInventory();

        CompoundTag tag = terminalStack.getOrCreateTag();
        CompoundTag root = tag.contains(NBT_ROOT) ? tag.getCompound(NBT_ROOT) : new CompoundTag();

        long[] thresholds = Arrays.copyOf(root.getLongArray(NBT_THRESHOLDS), SLOTS);

        int hudX = clampPercent(root.getInt(NBT_HUD_X), 100);
        int hudY = clampPercent(root.getInt(NBT_HUD_Y), 0);

        boolean hideAbove = root.getBoolean(NBT_HIDE_ABOVE);
        boolean hideBelow = root.getBoolean(NBT_HIDE_BELOW);

        ConfigInventory cfg = ConfigInventory.configTypes(SLOTS, () -> {});
        if (tag.contains(NBT_CONFIG)) {
            cfg.readFromChildTag(tag, NBT_CONFIG);
        }
        var inv = cfg.createMenuWrapper();

        var out = new ArrayList<NotificationHudPacket.Entry>(SLOTS);

        for (int i = 0; i < SLOTS; i++) {
            long th = thresholds[i];
            if (th <= 0) continue;

            ItemStack icon = inv.getStackInSlot(i);
            if (icon.isEmpty()) continue;

            AEKey key = cfg.getKey(i);
            if (key == null) continue;

            long amount = cached.get(key);
            boolean above = amount >= th;

            if (hideAbove && above) continue;
            if (hideBelow && !above) continue;

            ItemStack sendIcon = icon.copy();
            sendIcon.setCount(1);

            out.add(new NotificationHudPacket.Entry(sendIcon, amount, th));
        }

        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new NotificationHudPacket(out, (byte) hudX, (byte) hudY)
        );
    }

    private static int clampPercent(int v, int def) {
        if (v < 0 || v > 100) return def;
        return v;
    }
}
