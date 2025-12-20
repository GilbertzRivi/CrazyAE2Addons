package net.oktawia.crazyae2addons.logic;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.menu.ISubMenu;
import appeng.util.ConfigInventory;
import appeng.util.ConfigMenuInventory;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.menus.WirelessNotificationTerminalMenu;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import appeng.api.inventories.InternalInventory;
import net.minecraft.nbt.CompoundTag;

public class WirelessNotificationTerminalItemLogicHost extends WTMenuHost implements IUpgradeableObject {

    private static final String NBT_MONITOR_FILTERS = "crazy_notification_filters";

    private final ConfigInventory monitorCfg = ConfigInventory.configTypes(
            WirelessNotificationTerminalMenu.MAX_ROWS,
            this::onMonitorChanged
    );

    private final ConfigMenuInventory monitorMenuInv = new ConfigMenuInventory(monitorCfg);

    public WirelessNotificationTerminalItemLogicHost(Player player, @Nullable Integer slot, ItemStack itemStack,
                                                     BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, slot, itemStack, returnToMainMenu);
        this.readFromNbt();
    }

    public InternalInventory getMonitorInventory() {
        return monitorMenuInv;
    }

    private void onMonitorChanged() {
        if (!getPlayer().level().isClientSide()) {
            monitorCfg.writeToChildTag(getItemStack().getOrCreateTag(), NBT_MONITOR_FILTERS);
        }
    }

    @Override
    public void readFromNbt() {
        super.readFromNbt();

        CompoundTag tag = getItemStack().getOrCreateTag();
        if (tag.contains(NBT_MONITOR_FILTERS)) {
            monitorCfg.readFromChildTag(tag, NBT_MONITOR_FILTERS);
        }
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get());
    }
}
