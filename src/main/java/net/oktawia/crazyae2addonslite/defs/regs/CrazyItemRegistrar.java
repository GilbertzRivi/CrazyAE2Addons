package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.api.parts.PartModels;
import appeng.api.parts.IPart;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.items.*;

import java.util.List;

public class CrazyItemRegistrar {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CrazyAddonslite.MODID);

    public static List<? extends Item> getItems() {
        return ITEMS.getEntries()
                .stream()
                .map(DeferredHolder::get)
                .toList();
    }

    public static List<? extends Item> getParts() {
        return ITEMS.getEntries()
                .stream()
                .map(DeferredHolder::get)
                .filter(i -> i instanceof PartItem)
                .toList();
    }

    public static void registerPartModels() {
        for (Item item : getParts()) {
            if (item instanceof PartItem<?> partItem) {
                Class<?> partClass = partItem.getPartClass();
                if (partClass != null) {
                    PartModels.registerModels(PartModelsHelper.createModels(partClass.asSubclass(IPart.class)));
                }
            }
        }
    }

    // =========================================================
//
//    public static final DeferredItem<NBTExportBusPartItem> NBT_EXPORT_BUS_PART_ITEM =
//            ITEMS.register("nbt_export_bus", () -> new NBTExportBusPartItem(new Item.Properties()));
//
//    public static final DeferredItem<NBTStorageBusPartItem> NBT_STORAGE_BUS_PART_ITEM =
//            ITEMS.register("nbt_storage_bus", () -> new NBTStorageBusPartItem(new Item.Properties()));
//
//    // =========================================================
//
//    public static final DeferredItem<RRItemP2PTunnelPartItem> RR_ITEM_P2P_TUNNEL_PART =
//            ITEMS.register("round_robin_item_p2p_tunnel", () -> new RRItemP2PTunnelPartItem(new Item.Properties()));
//
//    public static final DeferredItem<RRFluidP2PTunnelPartItem> RR_FLUID_P2P_TUNNEL_PART =
//            ITEMS.register("round_robin_fluid_p2p_tunnel", () -> new RRFluidP2PTunnelPartItem(new Item.Properties()));
//
//    public static final DeferredItem<PartItem<?>> WORMHOLE_P2P_TUNNEL =
//            ITEMS.register("wormhole_tunnel", () -> new WormHoleP2PTunnelPartItem(new Item.Properties()));
//
//    // =========================================================
//
//    public static final DeferredItem<DisplayPartItem> DISPLAY_MONITOR_PART_ITEM =
//            ITEMS.register("display_monitor", () -> new DisplayPartItem(new Item.Properties()));
//
//    public static final DeferredItem<PartItem<MultiStorageLevelEmitterPart>> MULTI_LEVEL_EMITTER_ITEM =
//            ITEMS.register("multi_level_emitter",
//                    () -> new PartItem<>(new Item.Properties(), MultiStorageLevelEmitterPart.class, MultiStorageLevelEmitterPart::new));
//
//    public static final DeferredItem<RedstoneEmitterPartItem> REDSTONE_EMITTER =
//            ITEMS.register("redstone_emitter", () -> new RedstoneEmitterPartItem(new Item.Properties()));
//
//    public static final DeferredItem<RedstoneTerminalPartItem> REDSTONE_TERMINAL =
//            ITEMS.register("redstone_terminal", () -> new RedstoneTerminalPartItem(new Item.Properties()));
//
//    // =========================================================
//
//    public static final DeferredItem<AutomationUpgradeCard> AUTOMATION_UPGRADE_CARD =
//            ITEMS.register("automation_upgrade_card", () -> new AutomationUpgradeCard(new Item.Properties()));
//
//    public static final DeferredItem<AutomationUpgradeCard> PLAYER_UPGRADE_CARD =
//            ITEMS.register("player_upgrade_card", () -> new AutomationUpgradeCard(new Item.Properties()));

    public static final DeferredItem<Item> CRAZY_UPGRADE =
            ITEMS.register("crazy_upgrade", () -> new Item(new Item.Properties()));
//
//    // =========================================================
//
//    public static final DeferredItem<CrazyPatternMultiplierItem> CRAZY_PATTERN_MULTIPLIER_ITEM =
//            ITEMS.register("crazy_pattern_multiplier", () -> new CrazyPatternMultiplierItem(new Item.Properties()));
//
//    public static final DeferredItem<CrazyEmitterMultiplierItem> CRAZY_EMITTER_MULTIPLIER_ITEM =
//            ITEMS.register("crazy_emitter_multiplier", () -> new CrazyEmitterMultiplierItem(new Item.Properties()));
//
//    public static final DeferredItem<CpuPrioTunerItem> CPU_PRIO_TUNER =
//            ITEMS.register("cpu_prio_tuner", () -> new CpuPrioTunerItem(new Item.Properties()));
//
//    public static final DeferredItem<NbtViewCellItem> NBT_VIEW_CELL =
//            ITEMS.register("nbt_view_cell", () -> new NbtViewCellItem(new Item.Properties()));
//
//    public static final DeferredItem<TagViewCellItem> TAG_VIEW_CELL =
//            ITEMS.register("tag_view_cell", () -> new TagViewCellItem(new Item.Properties()));
//
//    // =========================================================
//
//    public static final DeferredItem<WirelessRedstoneTerminal> WIRELESS_REDSTONE_TERMINAL =
//            ITEMS.register("wireless_redstone_terminal", WirelessRedstoneTerminal::new);
//
//    public static final DeferredItem<WirelessNotificationTerminal> WIRELESS_NOTIFICATION_TERMINAL =
//            ITEMS.register("wireless_notification_terminal", WirelessNotificationTerminal::new);

    private CrazyItemRegistrar() {}
}
