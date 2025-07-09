package net.oktawia.crazyae2addons.screens;

import net.minecraft.network.chat.Component;

public interface CrazyScreen {
    static void i18n(String name, String key, String value) {
        AllCrazyScreens.I18N.put("gui.crazyae2addons." + name + "." + key, value);
    }

    default Component l10n(String name, String key, Object... args) {
        return Component.translatable("gui.crazyae2addons." + name + "." + key, args);
    }
}
