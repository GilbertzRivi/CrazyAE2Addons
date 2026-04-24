package net.oktawia.crazyae2addons.client.screens;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addons.CrazyConfig;

public class CrazyConfigScreen {

    private CrazyConfigScreen() {}

    public static Screen create(Screen parent) {
        ConfigBuilder b = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Crazy AE2 Addons"));

        b.setSavingRunnable(() -> CrazyConfig.COMMON_SPEC.save());

        ConfigEntryBuilder eb = b.entryBuilder();
        CrazyConfig.Common cfg = CrazyConfig.COMMON;

        ConfigCategory display = b.getOrCreateCategory(Component.literal("Display"));
        display.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.DISPLAY_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.DISPLAY_ENABLED::set)
                .build());
        display.addEntry(eb.startBooleanToggle(Component.literal("Images Enabled"), cfg.DISPLAY_IMAGES_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.DISPLAY_IMAGES_ENABLED::set)
                .build());
        display.addEntry(eb.startBooleanToggle(Component.literal("Stock Enabled"), cfg.DISPLAY_STOCK_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.DISPLAY_STOCK_ENABLED::set)
                .build());
        display.addEntry(eb.startBooleanToggle(Component.literal("Icons Enabled"), cfg.DISPLAY_ICONS_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.DISPLAY_ICONS_ENABLED::set)
                .build());
        display.addEntry(eb.startBooleanToggle(Component.literal("Delta Enabled"), cfg.DISPLAY_DELTA_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.DISPLAY_DELTA_ENABLED::set)
                .build());

        ConfigCategory emitterTerm = b.getOrCreateCategory(Component.literal("Emitter Terminal"));
        emitterTerm.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.EMITTER_TERMINAL_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.EMITTER_TERMINAL_ENABLED::set)
                .build());
        emitterTerm.addEntry(eb.startBooleanToggle(Component.literal("Wireless Enabled"), cfg.WIRELESS_EMITTER_TERMINAL_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WIRELESS_EMITTER_TERMINAL_ENABLED::set)
                .build());

        ConfigCategory notifTerm = b.getOrCreateCategory(Component.literal("Wireless Notification Terminal"));
        notifTerm.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.WIRELESS_NOTIFICATION_TERMINAL_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WIRELESS_NOTIFICATION_TERMINAL_ENABLED::set)
                .build());
        notifTerm.addEntry(eb.startIntField(Component.literal("Config Slots"), cfg.WIRELESS_NOTIFICATION_TERMINAL_CONFIG_SLOT.get())
                .setDefaultValue(16)
                .setMin(0)
                .setSaveConsumer(cfg.WIRELESS_NOTIFICATION_TERMINAL_CONFIG_SLOT::set)
                .build());

        ConfigCategory mle = b.getOrCreateCategory(Component.literal("Multi Level Emitter"));
        mle.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.MULTI_LEVEL_EMITTER_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.MULTI_LEVEL_EMITTER_ENABLED::set)
                .build());
        mle.addEntry(eb.startIntField(Component.literal("Config Slots"), cfg.MULTI_LEVEL_EMITTER_CONFIG_SLOT.get())
                .setDefaultValue(16)
                .setMin(0)
                .setSaveConsumer(cfg.MULTI_LEVEL_EMITTER_CONFIG_SLOT::set)
                .build());

        ConfigCategory tle = b.getOrCreateCategory(Component.literal("Tag Level Emitter"));
        tle.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.TAG_LEVEL_EMITTER_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.TAG_LEVEL_EMITTER_ENABLED::set)
                .build());

        ConfigCategory redstone = b.getOrCreateCategory(Component.literal("Redstone Emitter / Terminal"));
        redstone.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.REDSTONE_EMITTER_TERMINAL_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.REDSTONE_EMITTER_TERMINAL_ENABLED::set)
                .build());
        redstone.addEntry(eb.startBooleanToggle(Component.literal("Wireless Enabled"), cfg.WIRELESS_REDSTONE_TERMINAL_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WIRELESS_REDSTONE_TERMINAL_ENABLED::set)
                .build());

        ConfigCategory wormhole = b.getOrCreateCategory(Component.literal("Wormhole"));
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.WORMHOLE_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("Teleportation Enabled"), cfg.WORMHOLE_TELEPORTATION_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_TELEPORTATION_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("Nested P2Ps Enabled"), cfg.WORMHOLE_NESTED_P2PS_ENABLED.get())
                .setDefaultValue(false)
                .setSaveConsumer(cfg.WORMHOLE_NESTED_P2PS_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("Item Proxy Enabled"), cfg.WORMHOLE_ITEM_PROXY_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_ITEM_PROXY_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("Fluid Proxy Enabled"), cfg.WORMHOLE_FLUID_PROXY_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_FLUID_PROXY_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("FE Proxy Enabled"), cfg.WORMHOLE_FE_PROXY_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_FE_PROXY_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("EU Proxy Enabled"), cfg.WORMHOLE_EU_PROXY_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_EU_PROXY_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("Other Capability Proxy Enabled"), cfg.WORMHOLE_OTHER_CAPABILITY_PROXY_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_OTHER_CAPABILITY_PROXY_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("Merged Capability Proxy Enabled"), cfg.WORMHOLE_MERGED_CAPABILITY_PROXY_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_MERGED_CAPABILITY_PROXY_ENABLED::set)
                .build());
        wormhole.addEntry(eb.startBooleanToggle(Component.literal("Remote Interactions Enabled"), cfg.WORMHOLE_REMOTE_INTERACTIONS_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.WORMHOLE_REMOTE_INTERACTIONS_ENABLED::set)
                .build());

        ConfigCategory rrP2p = b.getOrCreateCategory(Component.literal("Round Robin P2P"));
        rrP2p.addEntry(eb.startBooleanToggle(Component.literal("Item P2P Enabled"), cfg.RR_ITEM_P2P_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.RR_ITEM_P2P_ENABLED::set)
                .build());
        rrP2p.addEntry(eb.startBooleanToggle(Component.literal("Fluid P2P Enabled"), cfg.RR_FLUID_P2P_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.RR_FLUID_P2P_ENABLED::set)
                .build());

        ConfigCategory cpuPrio = b.getOrCreateCategory(Component.literal("CPU Priorities"));
        cpuPrio.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.CPU_PRIORITIES_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.CPU_PRIORITIES_ENABLED::set)
                .build());

        ConfigCategory tagCell = b.getOrCreateCategory(Component.literal("Tag View Cell"));
        tagCell.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.TAG_VIEW_CELL_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.TAG_VIEW_CELL_ENABLED::set)
                .build());

        ConfigCategory patternMul = b.getOrCreateCategory(Component.literal("Pattern Multiplier"));
        patternMul.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.PATTERN_MULTIPLIER_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.PATTERN_MULTIPLIER_ENABLED::set)
                .build());

        ConfigCategory cpp = b.getOrCreateCategory(Component.literal("Crazy Pattern Provider"));
        cpp.addEntry(eb.startBooleanToggle(Component.literal("Block Enabled"), cfg.CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED::set)
                .build());
        cpp.addEntry(eb.startBooleanToggle(Component.literal("Part Enabled"), cfg.CRAZY_PATTERN_PROVIDER_PART_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.CRAZY_PATTERN_PROVIDER_PART_ENABLED::set)
                .build());
        cpp.addEntry(eb.startIntField(Component.literal("Max Upgrades (-1 = unlimited)"), cfg.CRAZY_PROVIDER_MAX_UPGRADES.get())
                .setDefaultValue(-1)
                .setMin(-1)
                .setSaveConsumer(cfg.CRAZY_PROVIDER_MAX_UPGRADES::set)
                .build());

        ConfigCategory ejector = b.getOrCreateCategory(Component.literal("Ejector"));
        ejector.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.EJECTOR_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.EJECTOR_ENABLED::set)
                .build());
        ejector.addEntry(eb.startBooleanToggle(Component.literal("Craft Missing Enabled"), cfg.EJECTOR_CRAFT_MISSING_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.EJECTOR_CRAFT_MISSING_ENABLED::set)
                .build());

        ConfigCategory pss = b.getOrCreateCategory(Component.literal("Portable Spatial Storage"));
        pss.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.PORTABLE_SPATIAL_STORAGE_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.PORTABLE_SPATIAL_STORAGE_ENABLED::set)
                .build());
        pss.addEntry(eb.startIntField(Component.literal("Cost per Block"), cfg.PORTABLE_SPATIAL_STORAGE_COST.get())
                .setDefaultValue(1)
                .setMin(0)
                .setSaveConsumer(cfg.PORTABLE_SPATIAL_STORAGE_COST::set)
                .build());
        pss.addEntry(eb.startIntField(Component.literal("Base Internal Power Capacity"), cfg.PORTABLE_SPATIAL_STORAGE_BASE_INTERNAL_POWER_CAPACITY.get())
                .setDefaultValue(200000)
                .setMin(0)
                .setSaveConsumer(cfg.PORTABLE_SPATIAL_STORAGE_BASE_INTERNAL_POWER_CAPACITY::set)
                .build());
        pss.addEntry(eb.startIntField(Component.literal("Max Structure Size (-1 = unlimited)"), cfg.PORTABLE_SPATIAL_STORAGE_MAX_STRUCTURE_SIZE.get())
                .setDefaultValue(-1)
                .setMin(-1)
                .setSaveConsumer(cfg.PORTABLE_SPATIAL_STORAGE_MAX_STRUCTURE_SIZE::set)
                .build());

        ConfigCategory psc = b.getOrCreateCategory(Component.literal("Portable Spatial Cloner"));
        psc.addEntry(eb.startBooleanToggle(Component.literal("Enabled"), cfg.PORTABLE_SPATIAL_CLONER_ENABLED.get())
                .setDefaultValue(true)
                .setSaveConsumer(cfg.PORTABLE_SPATIAL_CLONER_ENABLED::set)
                .build());
        psc.addEntry(eb.startIntField(Component.literal("Cost per Block"), cfg.PORTABLE_SPATIAL_CLONER_COST.get())
                .setDefaultValue(1)
                .setMin(0)
                .setSaveConsumer(cfg.PORTABLE_SPATIAL_CLONER_COST::set)
                .build());
        psc.addEntry(eb.startIntField(Component.literal("Base Internal Power Capacity"), cfg.PORTABLE_SPATIAL_CLONER_BASE_INTERNAL_POWER_CAPACITY.get())
                .setDefaultValue(200000)
                .setMin(0)
                .setSaveConsumer(cfg.PORTABLE_SPATIAL_CLONER_BASE_INTERNAL_POWER_CAPACITY::set)
                .build());
        psc.addEntry(eb.startIntField(Component.literal("Max Structure Size (-1 = unlimited)"), cfg.PORTABLE_SPATIAL_CLONER_MAX_STRUCTURE_SIZE.get())
                .setDefaultValue(-1)
                .setMin(-1)
                .setSaveConsumer(cfg.PORTABLE_SPATIAL_CLONER_MAX_STRUCTURE_SIZE::set)
                .build());

        return b.build();
    }
}