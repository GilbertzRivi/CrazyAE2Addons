package net.oktawia.crazyae2addons.defs.regs;

import appeng.api.parts.PartModels;
import appeng.items.materials.UpgradeCardItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.items.*;
import net.oktawia.crazyae2addons.items.part.*;
import net.oktawia.crazyae2addons.items.wireless.*;
import net.oktawia.crazyae2addons.parts.p2p.*;

import java.util.List;

public class CrazyItemRegistrar {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CrazyAddons.MODID);

    public static List<Item> getItems() {
        return ITEMS.getEntries()
                .stream()
                .map(RegistryObject::get)
                .toList();
    }

    public static List<Item> getParts() {
        return ITEMS.getEntries()
                .stream()
                .map(RegistryObject::get)
                .filter(item -> item instanceof PartItem)
                .toList();
    }

    public static void registerPartModels() {
        for (Item item : getParts()) {
            if (item instanceof PartItem<?> partItem) {
                Class<?> partClass = partItem.getPartClass();
                if (partClass != null) {
                    PartModels.registerModels(
                            PartModelsHelper.createModels(partClass.asSubclass(appeng.api.parts.IPart.class))
                    );
                }
            }
        }
    }

    public static final RegistryObject<Item> ANALOG_CARD =
            ITEMS.register("analog_card", () -> new UpgradeCardItem(new Item.Properties()));

    public static final RegistryObject<DisplayPartItem> DISPLAY =
            ITEMS.register("display", () -> new DisplayPartItem(new Item.Properties()));

    public static final RegistryObject<EmitterTerminalPartItem> EMITTER_TERMINAL =
            ITEMS.register("emitter_terminal",
                    () -> new EmitterTerminalPartItem(new Item.Properties()));

    public static final RegistryObject<RedstoneTerminalPartItem> REDSTONE_TERMINAL =
            ITEMS.register("redstone_terminal",
                    () -> new RedstoneTerminalPartItem(new Item.Properties()));

    public static final RegistryObject<RedstoneEmitterPartItem> REDSTONE_EMITTER =
            ITEMS.register("redstone_emitter",
                    () -> new RedstoneEmitterPartItem(new Item.Properties()));

    public static final RegistryObject<MultiLevelEmitterPartItem> MULTI_LEVEL_EMITTER =
            ITEMS.register("multi_level_emitter",
                    () -> new MultiLevelEmitterPartItem(new Item.Properties()));

    public static final RegistryObject<TagLevelEmitterPartItem> TAG_LEVEL_EMITTER =
            ITEMS.register("tag_level_emitter",
                    () -> new TagLevelEmitterPartItem(new Item.Properties()));

    public static final RegistryObject<WirelessEmitterTerminalItem> WIRELESS_EMITTER_TERMINAL =
            ITEMS.register("wireless_emitter_terminal", WirelessEmitterTerminalItem::new);

    public static final RegistryObject<WirelessRedstoneTerminal> WIRELESS_REDSTONE_TERMINAL =
            ITEMS.register("wireless_redstone_terminal", WirelessRedstoneTerminal::new);

    public static final RegistryObject<WirelessNotificationTerminalItem> WIRELESS_NOTIFICATION_TERMINAL =
            ITEMS.register("wireless_notification_terminal", WirelessNotificationTerminalItem::new);

    public static final RegistryObject<WormholeP2PTunnelPartItem> WORMHOLE =
            ITEMS.register("wormhole", () -> new WormholeP2PTunnelPartItem(new Item.Properties()));

    public static final RegistryObject<RRItemP2PTunnelPartItem> RR_ITEM_P2P =
            ITEMS.register("round_robin_item_p2p_tunnel",
                    () -> new RRItemP2PTunnelPartItem(new Item.Properties()));

    public static final RegistryObject<RRFluidP2PTunnelPartItem> RR_FLUID_P2P =
            ITEMS.register("round_robin_fluid_p2p_tunnel",
                    () -> new RRFluidP2PTunnelPartItem(new Item.Properties()));

    public static final RegistryObject<PortableSpatialStorage> PORTABLE_SPATIAL_STORAGE =
            ITEMS.register("portable_spatial_storage",
                    () -> new PortableSpatialStorage(new Item.Properties()));

    public static final RegistryObject<PortableSpatialCloner> PORTABLE_SPATIAL_CLONER =
            ITEMS.register("portable_spatial_cloner",
                    () -> new PortableSpatialCloner(new Item.Properties()));

    public static final RegistryObject<CpuPrioTunerItem> CPU_PRIO_TUNER =
            ITEMS.register("cpu_priority_tuner",
                    () -> new CpuPrioTunerItem(new Item.Properties()));

    public static final RegistryObject<TagViewCellItem> TAG_VIEW_CELL =
            ITEMS.register("tag_view_cell",
                    () -> new TagViewCellItem(new Item.Properties()));

    public static final RegistryObject<PatternMultiplierItem> PATTERN_MULTIPLIER =
            ITEMS.register("pattern_multiplier",
                    () -> new PatternMultiplierItem(new Item.Properties()));

    public static final RegistryObject<Item> CRAZY_UPGRADE =
            ITEMS.register("crazy_upgrade",
                    () -> new Item(new Item.Properties()));

    public static final RegistryObject<CrazyPatternProviderPartItem> CRAZY_PATTERN_PROVIDER_PART =
            ITEMS.register("crazy_pattern_provider_part",
                    () -> new CrazyPatternProviderPartItem(new Item.Properties()));

    private CrazyItemRegistrar() {}
}