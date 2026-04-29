package net.oktawia.crazyae2addons.client.screens;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CrazyConfigScreen {

    private CrazyConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder b = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(t(LangDefs.CONFIG_TITLE));

        b.setSavingRunnable(() -> CrazyConfig.COMMON_SPEC.save());

        ConfigEntryBuilder eb = b.entryBuilder();
        CrazyConfig.Common cfg = CrazyConfig.COMMON;

        ConfigCategory root = b.getOrCreateCategory(t(LangDefs.CONFIG_CATEGORY_SETTINGS));

        addSection(root, eb, LangDefs.CONFIG_SECTION_DISPLAY, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.DISPLAY_ENABLED.get(), true,
                            cfg.DISPLAY_ENABLED::set,
                            LangDefs.CONFIG_DESC_DISPLAY_ENABLED
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_IMAGES_ENABLED, cfg.DISPLAY_IMAGES_ENABLED.get(), true,
                            cfg.DISPLAY_IMAGES_ENABLED::set,
                            LangDefs.CONFIG_DESC_DISPLAY_IMAGES_ENABLED
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_STOCK_ENABLED, cfg.DISPLAY_STOCK_ENABLED.get(), true,
                            cfg.DISPLAY_STOCK_ENABLED::set,
                            LangDefs.CONFIG_DESC_DISPLAY_STOCK_ENABLED
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ICONS_ENABLED, cfg.DISPLAY_ICONS_ENABLED.get(), true,
                            cfg.DISPLAY_ICONS_ENABLED::set,
                            LangDefs.CONFIG_DESC_DISPLAY_ICONS_ENABLED
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_DELTA_ENABLED, cfg.DISPLAY_DELTA_ENABLED.get(), true,
                            cfg.DISPLAY_DELTA_ENABLED::set,
                            LangDefs.CONFIG_DESC_DISPLAY_DELTA_ENABLED
                    ));
                },
                LangDefs.CONFIG_SECTION_DISPLAY_DESC_1,
                LangDefs.CONFIG_SECTION_DISPLAY_DESC_2,
                LangDefs.CONFIG_SECTION_DISPLAY_DESC_3
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_EMITTER_TERMINAL, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.EMITTER_TERMINAL_ENABLED.get(), true,
                            cfg.EMITTER_TERMINAL_ENABLED::set,
                            LangDefs.CONFIG_DESC_EMITTER_TERMINAL_ENABLED
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_WIRELESS_ENABLED, cfg.WIRELESS_EMITTER_TERMINAL_ENABLED.get(), true,
                            cfg.WIRELESS_EMITTER_TERMINAL_ENABLED::set,
                            LangDefs.CONFIG_DESC_WIRELESS_VARIANT_ENABLED
                    ));
                },
                LangDefs.CONFIG_SECTION_EMITTER_TERMINAL_DESC_1,
                LangDefs.CONFIG_SECTION_EMITTER_TERMINAL_DESC_2
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_WIRELESS_NOTIFICATION_TERMINAL, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.WIRELESS_NOTIFICATION_TERMINAL_ENABLED.get(), true,
                            cfg.WIRELESS_NOTIFICATION_TERMINAL_ENABLED::set,
                            LangDefs.CONFIG_DESC_WIRELESS_NOTIFICATION_ENABLED
                    ));
                    entries.add(integer(eb, LangDefs.CONFIG_ENTRY_CONFIG_SLOTS, cfg.WIRELESS_NOTIFICATION_TERMINAL_CONFIG_SLOT.get(), 16, 0,
                            cfg.WIRELESS_NOTIFICATION_TERMINAL_CONFIG_SLOT::set,
                            LangDefs.CONFIG_DESC_WIRELESS_NOTIFICATION_CONFIG_SLOTS
                    ));
                },
                LangDefs.CONFIG_SECTION_WIRELESS_NOTIFICATION_TERMINAL_DESC_1,
                LangDefs.CONFIG_SECTION_WIRELESS_NOTIFICATION_TERMINAL_DESC_2,
                LangDefs.CONFIG_SECTION_WIRELESS_NOTIFICATION_TERMINAL_DESC_3
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_MULTI_LEVEL_EMITTER, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.MULTI_LEVEL_EMITTER_ENABLED.get(), true,
                            cfg.MULTI_LEVEL_EMITTER_ENABLED::set,
                            LangDefs.CONFIG_DESC_MULTI_LEVEL_EMITTER_ENABLED
                    ));
                    entries.add(integer(eb, LangDefs.CONFIG_ENTRY_CONFIG_SLOTS, cfg.MULTI_LEVEL_EMITTER_CONFIG_SLOT.get(), 16, 0,
                            cfg.MULTI_LEVEL_EMITTER_CONFIG_SLOT::set,
                            LangDefs.CONFIG_DESC_MULTI_LEVEL_EMITTER_CONFIG_SLOTS
                    ));
                },
                LangDefs.CONFIG_SECTION_MULTI_LEVEL_EMITTER_DESC_1,
                LangDefs.CONFIG_SECTION_MULTI_LEVEL_EMITTER_DESC_2
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_TAG_LEVEL_EMITTER, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.TAG_LEVEL_EMITTER_ENABLED.get(), true,
                            cfg.TAG_LEVEL_EMITTER_ENABLED::set,
                            LangDefs.CONFIG_DESC_TAG_LEVEL_EMITTER_ENABLED
                    ));
                },
                LangDefs.CONFIG_SECTION_TAG_LEVEL_EMITTER_DESC_1,
                LangDefs.CONFIG_SECTION_TAG_LEVEL_EMITTER_DESC_2
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_REDSTONE_EMITTER_TERMINAL, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.REDSTONE_EMITTER_TERMINAL_ENABLED.get(), true,
                            cfg.REDSTONE_EMITTER_TERMINAL_ENABLED::set,
                            LangDefs.CONFIG_DESC_REDSTONE_EMITTER_TERMINAL_ENABLED
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_WIRELESS_ENABLED, cfg.WIRELESS_REDSTONE_TERMINAL_ENABLED.get(), true,
                            cfg.WIRELESS_REDSTONE_TERMINAL_ENABLED::set,
                            LangDefs.CONFIG_DESC_WIRELESS_VARIANT_ENABLED
                    ));
                },
                LangDefs.CONFIG_SECTION_REDSTONE_EMITTER_TERMINAL_DESC_1,
                LangDefs.CONFIG_SECTION_REDSTONE_EMITTER_TERMINAL_DESC_2
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_WORMHOLE, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.WORMHOLE_ENABLED.get(), true,
                            cfg.WORMHOLE_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_ENABLED
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_TELEPORTATION_ENABLED, cfg.WORMHOLE_TELEPORTATION_ENABLED.get(), true,
                            cfg.WORMHOLE_TELEPORTATION_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_TELEPORTATION_1,
                            LangDefs.CONFIG_DESC_WORMHOLE_TELEPORTATION_2
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_NESTED_P2PS_ENABLED, cfg.WORMHOLE_NESTED_P2PS_ENABLED.get(), false,
                            cfg.WORMHOLE_NESTED_P2PS_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_NESTED_P2PS
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ITEM_PROXY_ENABLED, cfg.WORMHOLE_ITEM_PROXY_ENABLED.get(), true,
                            cfg.WORMHOLE_ITEM_PROXY_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_ITEM_PROXY
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_FLUID_PROXY_ENABLED, cfg.WORMHOLE_FLUID_PROXY_ENABLED.get(), true,
                            cfg.WORMHOLE_FLUID_PROXY_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_FLUID_PROXY
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_FE_PROXY_ENABLED, cfg.WORMHOLE_FE_PROXY_ENABLED.get(), true,
                            cfg.WORMHOLE_FE_PROXY_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_FE_PROXY
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_EU_PROXY_ENABLED, cfg.WORMHOLE_EU_PROXY_ENABLED.get(), true,
                            cfg.WORMHOLE_EU_PROXY_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_EU_PROXY
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_OTHER_CAPABILITY_PROXY_ENABLED, cfg.WORMHOLE_OTHER_CAPABILITY_PROXY_ENABLED.get(), true,
                            cfg.WORMHOLE_OTHER_CAPABILITY_PROXY_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_OTHER_CAPABILITY_1,
                            LangDefs.CONFIG_DESC_WORMHOLE_OTHER_CAPABILITY_2
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_MERGED_CAPABILITY_PROXY_ENABLED, cfg.WORMHOLE_MERGED_CAPABILITY_PROXY_ENABLED.get(), true,
                            cfg.WORMHOLE_MERGED_CAPABILITY_PROXY_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_MERGED_CAPABILITY_1,
                            LangDefs.CONFIG_DESC_WORMHOLE_MERGED_CAPABILITY_2
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_REMOTE_INTERACTIONS_ENABLED, cfg.WORMHOLE_REMOTE_INTERACTIONS_ENABLED.get(), true,
                            cfg.WORMHOLE_REMOTE_INTERACTIONS_ENABLED::set,
                            LangDefs.CONFIG_DESC_WORMHOLE_REMOTE_INTERACTIONS_1,
                            LangDefs.CONFIG_DESC_WORMHOLE_REMOTE_INTERACTIONS_2
                    ));
                },
                LangDefs.CONFIG_SECTION_WORMHOLE_DESC_1,
                LangDefs.CONFIG_SECTION_WORMHOLE_DESC_2,
                LangDefs.CONFIG_SECTION_WORMHOLE_DESC_3,
                LangDefs.CONFIG_SECTION_WORMHOLE_DESC_4,
                LangDefs.CONFIG_SECTION_WORMHOLE_DESC_5
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_ROUND_ROBIN_P2P, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ITEM_P2P_ENABLED, cfg.RR_ITEM_P2P_ENABLED.get(), true,
                            cfg.RR_ITEM_P2P_ENABLED::set,
                            LangDefs.CONFIG_DESC_RR_ITEM_P2P
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_FLUID_P2P_ENABLED, cfg.RR_FLUID_P2P_ENABLED.get(), true,
                            cfg.RR_FLUID_P2P_ENABLED::set,
                            LangDefs.CONFIG_DESC_RR_FLUID_P2P
                    ));
                },
                LangDefs.CONFIG_SECTION_ROUND_ROBIN_P2P_DESC_1,
                LangDefs.CONFIG_SECTION_ROUND_ROBIN_P2P_DESC_2
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_CPU_PRIORITIES, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.CPU_PRIORITIES_ENABLED.get(), true,
                            cfg.CPU_PRIORITIES_ENABLED::set,
                            LangDefs.CONFIG_DESC_CPU_PRIORITIES
                    ));
                },
                LangDefs.CONFIG_SECTION_CPU_PRIORITIES_DESC_1,
                LangDefs.CONFIG_SECTION_CPU_PRIORITIES_DESC_2,
                LangDefs.CONFIG_SECTION_CPU_PRIORITIES_DESC_3
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_TAG_VIEW_CELL, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.TAG_VIEW_CELL_ENABLED.get(), true,
                            cfg.TAG_VIEW_CELL_ENABLED::set,
                            LangDefs.CONFIG_DESC_TAG_VIEW_CELL
                    ));
                },
                LangDefs.CONFIG_SECTION_TAG_VIEW_CELL_DESC_1,
                LangDefs.CONFIG_SECTION_TAG_VIEW_CELL_DESC_2
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_PATTERN_MULTIPLIER, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.PATTERN_MULTIPLIER_ENABLED.get(), true,
                            cfg.PATTERN_MULTIPLIER_ENABLED::set,
                            LangDefs.CONFIG_DESC_PATTERN_MULTIPLIER
                    ));
                },
                LangDefs.CONFIG_SECTION_PATTERN_MULTIPLIER_DESC_1,
                LangDefs.CONFIG_SECTION_PATTERN_MULTIPLIER_DESC_2,
                LangDefs.CONFIG_SECTION_PATTERN_MULTIPLIER_DESC_3
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_CRAZY_PATTERN_PROVIDER, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_BLOCK_ENABLED, cfg.CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED.get(), true,
                            cfg.CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED::set,
                            LangDefs.CONFIG_DESC_CRAZY_PATTERN_PROVIDER_BLOCK
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_PART_ENABLED, cfg.CRAZY_PATTERN_PROVIDER_PART_ENABLED.get(), true,
                            cfg.CRAZY_PATTERN_PROVIDER_PART_ENABLED::set,
                            LangDefs.CONFIG_DESC_CRAZY_PATTERN_PROVIDER_PART
                    ));
                    entries.add(integer(eb, LangDefs.CONFIG_ENTRY_MAX_UPGRADES_UNLIMITED, cfg.CRAZY_PROVIDER_MAX_UPGRADES.get(), -1, -1,
                            cfg.CRAZY_PROVIDER_MAX_UPGRADES::set,
                            LangDefs.CONFIG_DESC_CRAZY_PROVIDER_MAX_UPGRADES,
                            LangDefs.CONFIG_DESC_UNLIMITED_MINUS_ONE,
                            LangDefs.CONFIG_DESC_CRAZY_PROVIDER_MAX_UPGRADES_NOTE
                    ));
                },
                LangDefs.CONFIG_SECTION_CRAZY_PATTERN_PROVIDER_DESC_1,
                LangDefs.CONFIG_SECTION_CRAZY_PATTERN_PROVIDER_DESC_2,
                LangDefs.CONFIG_SECTION_CRAZY_PATTERN_PROVIDER_DESC_3
        );

        addSection(root, eb, LangDefs.CONFIG_SECTION_EJECTOR, entries -> {
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_ENABLED, cfg.EJECTOR_ENABLED.get(), true,
                            cfg.EJECTOR_ENABLED::set,
                            LangDefs.CONFIG_DESC_EJECTOR_ENABLED
                    ));
                    entries.add(bool(eb, LangDefs.CONFIG_ENTRY_CRAFT_MISSING_ENABLED, cfg.EJECTOR_CRAFT_MISSING_ENABLED.get(), true,
                            cfg.EJECTOR_CRAFT_MISSING_ENABLED::set,
                            LangDefs.CONFIG_DESC_EJECTOR_CRAFT_MISSING
                    ));
                },
                LangDefs.CONFIG_SECTION_EJECTOR_DESC_1,
                LangDefs.CONFIG_SECTION_EJECTOR_DESC_2,
                LangDefs.CONFIG_SECTION_EJECTOR_DESC_3,
                LangDefs.CONFIG_SECTION_EJECTOR_DESC_4,
                LangDefs.CONFIG_SECTION_EJECTOR_DESC_5
        );
        return b.build();
    }

    private static void addSection(
            ConfigCategory root,
            ConfigEntryBuilder eb,
            LangDefs name,
            Consumer<List<AbstractConfigListEntry>> entriesBuilder,
            LangDefs... tooltip
    ) {
        List<AbstractConfigListEntry> entries = new ArrayList<>();
        entriesBuilder.accept(entries);

        root.addEntry(eb.startSubCategory(t(name), entries)
                .setTooltip(tooltip(tooltip))
                .setExpanded(false)
                .build());
    }

    private static AbstractConfigListEntry bool(
            ConfigEntryBuilder eb,
            LangDefs name,
            boolean value,
            boolean defaultValue,
            Consumer<Boolean> saveConsumer,
            LangDefs... tooltip
    ) {
        return eb.startBooleanToggle(t(name), value)
                .setDefaultValue(defaultValue)
                .setTooltip(tooltip(tooltip))
                .setSaveConsumer(saveConsumer)
                .build();
    }

    private static AbstractConfigListEntry integer(
            ConfigEntryBuilder eb,
            LangDefs name,
            int value,
            int defaultValue,
            int min,
            Consumer<Integer> saveConsumer,
            LangDefs... tooltip
    ) {
        return eb.startIntField(t(name), value)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setTooltip(tooltip(tooltip))
                .setSaveConsumer(saveConsumer)
                .build();
    }

    private static AbstractConfigListEntry decimal(
            ConfigEntryBuilder eb,
            LangDefs name,
            double value,
            double defaultValue,
            double min,
            Consumer<Double> saveConsumer,
            LangDefs... tooltip
    ) {
        return eb.startDoubleField(t(name), value)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setTooltip(tooltip(tooltip))
                .setSaveConsumer(saveConsumer)
                .build();
    }

    private static Component t(LangDefs def) {
        return Component.translatable(def.getTranslationKey());
    }

    private static Component[] tooltip(LangDefs... defs) {
        List<Component> out = new ArrayList<>();

        for (LangDefs def : defs) {
            addWrappedTooltipLine(out, t(def));
        }

        return out.toArray(Component[]::new);
    }

    private static void addWrappedTooltipLine(List<Component> out, Component component) {
        String text = component.getString();
        if (text == null || text.isBlank()) {
            out.add(Component.empty());
            return;
        }

        int maxWidth = getTooltipMaxWidth();
        var font = Minecraft.getInstance().font;

        StringBuilder line = new StringBuilder();

        for (String word : text.split(" ")) {
            if (word.isBlank()) {
                continue;
            }

            String candidate = line.length() == 0
                    ? word
                    : line + " " + word;

            if (font.width(candidate) <= maxWidth || line.length() == 0) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }

            out.add(Component.literal(line.toString()));
            line.setLength(0);
            line.append(word);
        }

        if (line.length() > 0) {
            out.add(Component.literal(line.toString()));
        }
    }

    private static int getTooltipMaxWidth() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen == null) {
            return 240;
        }

        return Math.max(160, Math.min(240, minecraft.screen.width - 100));
    }
}