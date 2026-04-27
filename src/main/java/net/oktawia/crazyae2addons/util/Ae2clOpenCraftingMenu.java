package net.oktawia.crazyae2addons.util;

import appeng.api.stacks.AEKey;
import appeng.menu.locator.MenuLocator;
import appeng.menu.me.crafting.CraftAmountMenu;
import net.minecraft.server.level.ServerPlayer;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Ae2clOpenCraftingMenu {

    public static volatile boolean IS_AE2_CL = false;

    private Ae2clOpenCraftingMenu() {
    }

    public static void open(ServerPlayer player, MenuLocator locator, AEKey whatToCraft, long amount) {
        if (IS_AE2_CL) {
            openLong(player, locator, whatToCraft, amount);
        } else {
            openInt(player, locator, whatToCraft, amount);
        }
    }

    private static void openLong(ServerPlayer player, MenuLocator locator, AEKey whatToCraft, long amount) {
        try {
            Method method = CraftAmountMenu.class.getMethod(
                    "open",
                    ServerPlayer.class,
                    MenuLocator.class,
                    AEKey.class,
                    long.class
            );

            method.invoke(null, player, locator, whatToCraft, amount);
        } catch (NoSuchMethodException e) {
            openInt(player, locator, whatToCraft, amount);
        } catch (InvocationTargetException e) {
            CrazyAddons.LOGGER.debug("Fork detection went wrong? detected FORK", e.getCause());
        } catch (ReflectiveOperationException e) {
            CrazyAddons.LOGGER.debug("Couldn't invoke open menu with long arg", e.getCause());
        }
    }

    private static void openInt(ServerPlayer player, MenuLocator locator, AEKey whatToCraft, long amount) {
        try {
            Method method = CraftAmountMenu.class.getMethod(
                    "open",
                    ServerPlayer.class,
                    MenuLocator.class,
                    AEKey.class,
                    int.class
            );

            int clampedAmount = (int) Math.min(amount, Integer.MAX_VALUE);
            method.invoke(null, player, locator, whatToCraft, clampedAmount);
        } catch (NoSuchMethodException e) {
            openLong(player, locator, whatToCraft, amount);
        } catch (InvocationTargetException e) {
            CrazyAddons.LOGGER.debug("Fork detection went wrong? detected NO FORK", e.getCause());
        } catch (ReflectiveOperationException e) {
            CrazyAddons.LOGGER.debug("Couldn't invoke open menu with int arg", e.getCause());
        }
    }
}