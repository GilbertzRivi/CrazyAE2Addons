package net.oktawia.crazyae2addons.defs;

import appeng.core.definitions.*;
import appeng.items.parts.PartItem;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.GregTech.GTEnergyExporterPart;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.parts.*;
import net.oktawia.crazyae2addons.items.*;
import net.oktawia.crazyae2addons.compat.GregTech.*;

import java.util.*;

public class ItemDefs {

    private static final Map<Item, List<Map.Entry<String, Map<String, Item>>>> ITEM_RECIPES = new HashMap<>();

    public static Map<Item, List<Map.Entry<String, Map<String, Item>>>> getItemRecipes() {
        return ITEM_RECIPES;
    }

    public static void item(Item item, String recipe, Map<String, Item> recipeMap) {
        ITEM_RECIPES.computeIfAbsent(item, k -> new ArrayList<>())
                .add(Map.entry(recipe, recipeMap));
    }

    public static void registerRecipes(){
        item(
                CrazyItemRegistrar.CIRCUIT_UPGRADE_CARD_ITEM.get(),
                "CT",
                Map.of(
                        "C", AEItems.ADVANCED_CARD.asItem(),
                        "T", AEItems.LOGIC_PROCESSOR.asItem()
                )
        );

        item(
            CrazyItemRegistrar.LOGIC_CARD.get(),
            "AS",
            Map.of(
                "A", AEItems.ADVANCED_CARD.asItem(),
                "S", AEItems.SKY_DUST.asItem()
            )
        );

        item(
            CrazyItemRegistrar.ADD_CARD.get(),
            "DC",
            Map.of(
                "D", AEItems.SKY_DUST.asItem(),
                "C", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.SUB_CARD.get(),
            "CD",
            Map.of(
                "D", AEItems.SKY_DUST.asItem(),
                "C", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.MUL_CARD.get(),
            "D/C",
            Map.of(
                "D", AEItems.SKY_DUST.asItem(),
                "C", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.DIV_CARD.get(),
            "C/D",
            Map.of(
                "D", AEItems.SKY_DUST.asItem(),
                "C", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.MAX_CARD.get(),
            "DC",
            Map.of(
                "D", Items.GLOWSTONE_DUST,
                "C", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.MIN_CARD.get(),
            "CD",
            Map.of(
                "D", Items.GLOWSTONE_DUST,
                "C", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.BSR_CARD.get(),
            "D/C",
            Map.of(
                "D",  Items.GLOWSTONE_DUST,
                "C", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.BSL_CARD.get(),
            "C/D",
            Map.of(
                "D", Items.GLOWSTONE_DUST,
                "C", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.HIT_CARD.get(),
            "DCR",
            Map.of(
                "D", Items.GLOWSTONE_DUST,
                "C", CrazyItemRegistrar.LOGIC_CARD.get(),
                "R", Items.REDSTONE
            )
        );

        item(
            CrazyItemRegistrar.HIF_CARD.get(),
            "DCR",
            Map.of(
                "D", AEItems.SKY_DUST.asItem(),
                "C", CrazyItemRegistrar.LOGIC_CARD.get(),
                "R", Items.REDSTONE
            )
        );

        item(
            CrazyItemRegistrar.RR_ITEM_P2P_TUNNEL_PART.get(),
            "PE",
            Map.of(
                "P", AEParts.ITEM_P2P_TUNNEL.asItem(),
                "E", AEItems.EQUAL_DISTRIBUTION_CARD.asItem()
            )
        );

        item(
                CrazyItemRegistrar.NBT_EXPORT_BUS_PART_ITEM.get(),
                "ET/TL",
                Map.of(
                        "E", AEParts.EXPORT_BUS.asItem(),
                        "T", Items.NAME_TAG,
                        "L", AEItems.LOGIC_PROCESSOR.asItem()
                )
        );

        item(
                CrazyItemRegistrar.NBT_STORAGE_BUS_PART_ITEM.get(),
                "ET/TL",
                Map.of(
                        "E", AEParts.STORAGE_BUS.asItem(),
                        "T", Items.NAME_TAG,
                        "L", AEItems.LOGIC_PROCESSOR.asItem()
                )
        );

        item(
            CrazyItemRegistrar.DISPLAY_MONITOR_PART_ITEM.get(),
            "TL",
            Map.of(
                "T", AEParts.SEMI_DARK_MONITOR.asItem(),
                "L", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.DATA_EXTRACTOR_PART_ITEM.get(),
            "IL",
            Map.of(
                "I", AEParts.IMPORT_BUS.asItem(),
                "L", CrazyItemRegistrar.LOGIC_CARD.get()
            )
        );

        item(
            CrazyItemRegistrar.CHUNKY_FLUID_P2P_TUNNEL_PART.get(),
            "TL",
            Map.of(
                "T", AEParts.FLUID_P2P_TUNNEL.asItem(),
                "L", AEItems.LOGIC_PROCESSOR.asItem()
            )
        );

        item(
            CrazyItemRegistrar.ENERGY_EXPORTER_PART_ITEM.get(),
            "ERR",
            Map.of(
                "E", AEParts.EXPORT_BUS.asItem(),
                "R", Items.REDSTONE
            )
        );

        item(
            CrazyItemRegistrar.ENTITY_TICKER_PART_ITEM.get(),
            "DND/NEN/DND",
            Map.of(
                "D", Items.DIAMOND,
                "N", Items.NETHER_STAR,
                "E", CrazyItemRegistrar.ENERGY_EXPORTER_PART_ITEM.get()
            )
        );

        item(
            CrazyItemRegistrar.CRAZY_PATTERN_MODIFIER_ITEM.get(),
            "PZ/ZP",
            Map.of(
                "P", AEItems.BLANK_PATTERN.asItem(),
                "Z", AEItems.LOGIC_PROCESSOR.asItem()
            )
        );

        item(
                CrazyItemRegistrar.CRAZY_PATTERN_MULTIPLIER_ITEM.get(),
                "PZ/ZP",
                Map.of(
                        "P", AEItems.BLANK_PATTERN.asItem(),
                        "Z", AEItems.CALCULATION_PROCESSOR.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_HOUSING.get(),
                "GRG/R R/EFE",
                Map.of(
                        "G", AEBlocks.QUARTZ_VIBRANT_GLASS.asItem(),
                        "R", Items.REDSTONE.asItem(),
                        "E", Items.ECHO_SHARD.asItem(),
                        "F", Items.FLINT.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_1K.get(),
                "HC",
                Map.of(
                        "H", CrazyItemRegistrar.MOB_CELL_HOUSING.get().asItem().asItem(),
                        "C", AEItems.CELL_COMPONENT_1K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_4K.get(),
                "HC",
                Map.of(
                        "H", CrazyItemRegistrar.MOB_CELL_HOUSING.get().asItem().asItem(),
                        "C", AEItems.CELL_COMPONENT_4K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_16K.get(),
                "HC",
                Map.of(
                        "H", CrazyItemRegistrar.MOB_CELL_HOUSING.get().asItem().asItem(),
                        "C", AEItems.CELL_COMPONENT_16K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_64K.get(),
                "HC",
                Map.of(
                        "H", CrazyItemRegistrar.MOB_CELL_HOUSING.get().asItem().asItem(),
                        "C", AEItems.CELL_COMPONENT_64K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_256K.get(),
                "HC",
                Map.of(
                        "H", CrazyItemRegistrar.MOB_CELL_HOUSING.get().asItem().asItem(),
                        "C", AEItems.CELL_COMPONENT_256K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_1K.get(),
                "GRG/RCR/EFE",
                Map.of(
                        "G", AEBlocks.QUARTZ_VIBRANT_GLASS.asItem(),
                        "R", Items.REDSTONE.asItem(),
                        "E", Items.ECHO_SHARD.asItem(),
                        "F", Items.FLINT.asItem(),
                        "C", AEItems.CELL_COMPONENT_1K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_4K.get(),
                "GRG/RCR/EFE",
                Map.of(
                        "G", AEBlocks.QUARTZ_VIBRANT_GLASS.asItem(),
                        "R", Items.REDSTONE.asItem(),
                        "E", Items.ECHO_SHARD.asItem(),
                        "F", Items.FLINT.asItem(),
                        "C", AEItems.CELL_COMPONENT_4K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_16K.get(),
                "GRG/RCR/EFE",
                Map.of(
                        "G", AEBlocks.QUARTZ_VIBRANT_GLASS.asItem(),
                        "R", Items.REDSTONE.asItem(),
                        "E", Items.ECHO_SHARD.asItem(),
                        "F", Items.FLINT.asItem(),
                        "C", AEItems.CELL_COMPONENT_16K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_64K.get(),
                "GRG/RCR/EFE",
                Map.of(
                        "G", AEBlocks.QUARTZ_VIBRANT_GLASS.asItem(),
                        "R", Items.REDSTONE.asItem(),
                        "E", Items.ECHO_SHARD.asItem(),
                        "F", Items.FLINT.asItem(),
                        "C", AEItems.CELL_COMPONENT_64K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_CELL_256K.get(),
                "GRG/RCR/EFE",
                Map.of(
                        "G", AEBlocks.QUARTZ_VIBRANT_GLASS.asItem(),
                        "R", Items.REDSTONE.asItem(),
                        "E", Items.ECHO_SHARD.asItem(),
                        "F", Items.FLINT.asItem(),
                        "C", AEItems.CELL_COMPONENT_256K.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_ANNIHILATION_PLANE.get(),
                "AE",
                Map.of(
                        "A", AEParts.ANNIHILATION_PLANE.asItem(),
                        "E", Items.ECHO_SHARD.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_FORMATION_PLANE.get(),
                "AE",
                Map.of(
                        "A", AEParts.FORMATION_PLANE.asItem(),
                        "E", Items.ECHO_SHARD.asItem()
                )
        );

        item(
                CrazyItemRegistrar.MOB_EXPORT_BUS.get(),
                "BE",
                Map.of(
                        "B", AEParts.EXPORT_BUS.asItem(),
                        "E", Items.ECHO_SHARD.asItem()
                )
        );

        item(
                CrazyItemRegistrar.LOOTING_UPGRADE_CARD.get(),
                "ACS",
                Map.of(
                        "A", AEItems.ADVANCED_CARD.asItem(),
                        "C", Items.DIAMOND,
                        "S", Items.IRON_SWORD
                )
        );

        item(
                CrazyItemRegistrar.EXPERIENCE_UPGRADE_CARD.get(),
                "ACB",
                Map.of(
                        "A", AEItems.ADVANCED_CARD.asItem(),
                        "C", Items.EXPERIENCE_BOTTLE,
                        "B", Items.GOLD_INGOT
                )
        );

        item(
                CrazyItemRegistrar.CRAZY_EMITTER_MULTIPLIER_ITEM.get(),
                "AC",
                Map.of(
                        "A", AEParts.LEVEL_EMITTER.asItem(),
                        "C", AEItems.LOGIC_PROCESSOR.asItem()
                )
        );

        item(
                CrazyItemRegistrar.CRAZY_CALCULATOR_ITEM.get(),
                "ABC",
                Map.of(
                        "A", AEItems.ENGINEERING_PROCESSOR.asItem(),
                        "B", AEItems.CALCULATION_PROCESSOR.asItem(),
                        "C", AEItems.LOGIC_PROCESSOR.asItem()
                )
        );

        item(
                CrazyItemRegistrar.REDSTONE_EMITTER.get(),
                "LR",
                Map.of(
                        "L", AEParts.LEVEL_EMITTER.asItem(),
                        "R", Items.REDSTONE
                )
        );

        item(
                CrazyItemRegistrar.EXTRACTING_FE_P2P_TUNNEL.get(),
                "FEA",
                Map.of(
                        "F", AEParts.FE_P2P_TUNNEL.asItem(),
                        "E", AEParts.IMPORT_BUS.asItem(),
                        "A", AEItems.SPEED_CARD.asItem()
                )
        );

        item(
                CrazyItemRegistrar.EXTRACTING_ITEM_P2P_TUNNEL.get(),
                "FEA",
                Map.of(
                        "F", AEParts.ITEM_P2P_TUNNEL.asItem(),
                        "E", AEParts.IMPORT_BUS.asItem(),
                        "A", AEItems.SPEED_CARD.asItem()
                )
        );

        item(
                CrazyItemRegistrar.EXTRACTING_FLUID_P2P_TUNNEL.get(),
                "FEA",
                Map.of(
                        "F", AEParts.FLUID_P2P_TUNNEL.asItem(),
                        "E", AEParts.IMPORT_BUS.asItem(),
                        "A", AEItems.SPEED_CARD.asItem()
                )
        );

        item(
                CrazyItemRegistrar.WORMHOLE_P2P_TUNNEL.get(),
                "IAI/BNC/IDI",
                Map.of(
                        "A", AEParts.ITEM_P2P_TUNNEL.asItem(),
                        "B", AEParts.FLUID_P2P_TUNNEL.asItem(),
                        "C", AEParts.FE_P2P_TUNNEL.asItem(),
                        "D", AEParts.ME_P2P_TUNNEL.asItem(),
                        "N", Items.NETHER_STAR,
                        "I", Items.DIAMOND
                )
        );

        item(
                CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(),
                "R/T/D",
                Map.of(
                        "R", AEItems.WIRELESS_RECEIVER.asItem(),
                        "T", CrazyItemRegistrar.REDSTONE_TERMINAL.get(),
                        "D", AEBlocks.DENSE_ENERGY_CELL.asItem()
                )
        );

        item(
                CrazyItemRegistrar.REDSTONE_TERMINAL.get(),
                "T/R",
                Map.of(
                        "R", Items.REDSTONE,
                        "T", AEParts.TERMINAL.asItem()
                )
        );

        item(
                CrazyItemRegistrar.ENERGY_INTERFACE_PART.get(),
                "TR",
                Map.of(
                        "R", CrazyItemRegistrar.ENERGY_EXPORTER_PART_ITEM.get(),
                        "T", AEBlocks.INTERFACE.asItem()
                )
        );

        item(
                CrazyItemRegistrar.CRAZY_UPGRADE.get(),
                "PPP/PDP/PPP",
                Map.of(
                        "P", AEBlocks.PATTERN_PROVIDER.asItem(),
                        "D", Items.DIAMOND.asItem()
                )
        );

        item(
                CrazyItemRegistrar.BUILDER_PATTERN.get(),
                "PE",
                Map.of(
                        "P", AEItems.BLANK_PATTERN.asItem(),
                        "E", Items.EMERALD.asItem()
                )
        );

        item(
                CrazyItemRegistrar.VARIABLE_TERMINAL.get(),
                "RL",
                Map.of(
                        "R", CrazyItemRegistrar.REDSTONE_TERMINAL.get().asItem(),
                        "L", CrazyItemRegistrar.LOGIC_CARD.get().asItem()
                )
        );
    }
}