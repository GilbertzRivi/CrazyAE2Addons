package net.oktawia.crazyae2addonslite.defs;

import appeng.core.definitions.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;

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
//        item(
//            CrazyItemRegistrar.RR_ITEM_P2P_TUNNEL_PART.get(),
//            "PE",
//            Map.of(
//                "P", AEParts.ITEM_P2P_TUNNEL.asItem(),
//                "E", AEItems.EQUAL_DISTRIBUTION_CARD.asItem()
//            )
//        );
//
//        item(
//                CrazyItemRegistrar.NBT_EXPORT_BUS_PART_ITEM.get(),
//                "ET/TL",
//                Map.of(
//                        "E", AEParts.EXPORT_BUS.asItem(),
//                        "T", Items.NAME_TAG,
//                        "L", AEItems.LOGIC_PROCESSOR.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.NBT_STORAGE_BUS_PART_ITEM.get(),
//                "ET/TL",
//                Map.of(
//                        "E", AEParts.STORAGE_BUS.asItem(),
//                        "T", Items.NAME_TAG,
//                        "L", AEItems.LOGIC_PROCESSOR.asItem()
//                )
//        );
//
//        item(
//            CrazyItemRegistrar.DISPLAY_MONITOR_PART_ITEM.get(),
//            "TL",
//            Map.of(
//                "T", AEParts.SEMI_DARK_MONITOR.asItem(),
//                "L", AEItems.ADVANCED_CARD.asItem()
//            )
//        );
//
//        item(
//                CrazyItemRegistrar.CRAZY_EMITTER_MULTIPLIER_ITEM.get(),
//                "PC",
//                Map.of(
//                        "P", CrazyItemRegistrar.CRAZY_PATTERN_MULTIPLIER_ITEM.get(),
//                        "C", AEParts.LEVEL_EMITTER.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.REDSTONE_EMITTER.get(),
//                "LR",
//                Map.of(
//                        "L", AEParts.LEVEL_EMITTER.asItem(),
//                        "R", Items.REDSTONE
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.WORMHOLE_P2P_TUNNEL.get(),
//                "IAI/BNC/IDI",
//                Map.of(
//                        "A", AEParts.ITEM_P2P_TUNNEL.asItem(),
//                        "B", AEParts.FLUID_P2P_TUNNEL.asItem(),
//                        "C", AEParts.FE_P2P_TUNNEL.asItem(),
//                        "D", AEParts.ME_P2P_TUNNEL.asItem(),
//                        "N", Items.NETHER_STAR,
//                        "I", Items.DIAMOND
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(),
//                "R/T/D",
//                Map.of(
//                        "R", AEItems.WIRELESS_RECEIVER.asItem(),
//                        "T", CrazyItemRegistrar.REDSTONE_TERMINAL.get(),
//                        "D", AEBlocks.DENSE_ENERGY_CELL.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.REDSTONE_TERMINAL.get(),
//                "TR",
//                Map.of(
//                        "R", Items.REDSTONE,
//                        "T", AEParts.TERMINAL.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.CRAZY_UPGRADE.get(),
//                "PPP/PDP/PPP",
//                Map.of(
//                        "P", AEBlocks.PATTERN_PROVIDER.asItem(),
//                        "D", AEItems.ADVANCED_CARD.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.NBT_VIEW_CELL.get(),
//                "DI",
//                Map.of(
//                        "D", AEItems.VIEW_CELL.asItem(),
//                        "I", Items.NAME_TAG.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.TAG_VIEW_CELL.get(),
//                "DI",
//                Map.of(
//                        "D", AEItems.VIEW_CELL.asItem(),
//                        "I", Items.BOOK.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.AUTOMATION_UPGRADE_CARD.get(),
//                "CE",
//                Map.of(
//                        "C", AEItems.ADVANCED_CARD.asItem(),
//                        "E", AEBlocks.CRAFTING_UNIT.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.PLAYER_UPGRADE_CARD.get(),
//                "CE",
//                Map.of(
//                        "C", AEItems.ADVANCED_CARD.asItem(),
//                        "E", Items.IRON_PICKAXE.asItem()
//                )
//        );
//
//        item(
//                CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get(),
//                "CE",
//                Map.of(
//                        "C", CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get().asItem(),
//                        "E", CrazyItemRegistrar.AUTOMATION_UPGRADE_CARD.get().asItem()
//                )
//        );
    }
}