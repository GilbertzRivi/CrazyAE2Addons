package net.oktawia.crazyae2addons.logic;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.menu.ISubMenu;
import appeng.util.ConfigInventory;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class WirelessNotificationTerminalItemLogicHost extends WTMenuHost {

    public static final int FILTER_SLOTS = 16;
    public static final String NBT_CONFIG = "config";

    private final ConfigInventory config = ConfigInventory.configTypes(FILTER_SLOTS, this::onConfigChanged);

    public WirelessNotificationTerminalItemLogicHost(Player player, @Nullable Integer slot, ItemStack itemStack,
                                                     BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, slot, itemStack, returnToMainMenu);
        readFromNbt();
    }

    public ConfigInventory getConfig() {
        return config;
    }

    private void onConfigChanged() {
        if (!getPlayer().level().isClientSide()) {
            config.writeToChildTag(getItemStack().getOrCreateTag(), NBT_CONFIG);
        }
    }

    @Override
    public void readFromNbt() {
        super.readFromNbt();
        CompoundTag tag = getItemStack().getOrCreateTag();
        if (tag.contains(NBT_CONFIG)) {
            config.readFromChildTag(tag, NBT_CONFIG);
        }
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get());
    }
}
