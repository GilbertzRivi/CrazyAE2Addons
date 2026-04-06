package net.oktawia.crazyae2addons.defs.regs;

import appeng.api.parts.PartModels;
import appeng.api.parts.IPart;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.world.item.Item;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.items.BuilderPatternItem;
import net.oktawia.crazyae2addons.items.CrazyPatternProviderPartItem;
import net.oktawia.crazyae2addons.parts.ChunkyFluidP2PTunnelPart;
import net.oktawia.crazyae2addons.parts.EmitterTerminalPart;
import net.oktawia.crazyae2addons.parts.RRItemP2PTunnelPart;
import net.oktawia.crazyae2addons.items.wireless.WirelessEmitterTerminalItem;

import java.util.List;

public class CrazyItemRegistrar {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CrazyAddons.MODID);

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

    public static void registerPartModels(FMLCommonSetupEvent event) {
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

    public static final DeferredItem<PartItem<EmitterTerminalPart>> EMITTER_TERMINAL_PART =
            ITEMS.register("emitter_terminal", () -> new PartItem<>(new Item.Properties(), EmitterTerminalPart.class, EmitterTerminalPart::new));

    public static final DeferredItem<PartItem<RRItemP2PTunnelPart>> RR_ITEM_P2P_TUNNEL_PART =
            ITEMS.register("round_robin_item_p2p_tunnel", () -> new PartItem<>(new Item.Properties(), RRItemP2PTunnelPart.class, RRItemP2PTunnelPart::new));

    public static final DeferredItem<PartItem<ChunkyFluidP2PTunnelPart>> CHUNKY_FLUID_P2P_TUNNEL_PART =
            ITEMS.register("chunky_fluid_p2p_tunnel", () -> new PartItem<>(new Item.Properties(), ChunkyFluidP2PTunnelPart.class, ChunkyFluidP2PTunnelPart::new));

    public static final DeferredItem<CrazyPatternProviderPartItem> CRAZY_PATTERN_PROVIDER_PART =
            ITEMS.register("crazy_pattern_provider_part", () -> new CrazyPatternProviderPartItem(new Item.Properties()));

    public static final DeferredItem<Item> CRAZY_UPGRADE =
            ITEMS.register("crazy_upgrade", () -> new Item(new Item.Properties()));

    public static final DeferredItem<BuilderPatternItem> BUILDER_PATTERN =
            ITEMS.register("builder_pattern", () -> new BuilderPatternItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<WirelessEmitterTerminalItem> WIRELESS_EMITTER_TERMINAL =
            ITEMS.register("wireless_emitter_terminal", WirelessEmitterTerminalItem::new);

    private CrazyItemRegistrar() {}
}
