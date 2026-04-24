package net.oktawia.crazyae2addons.defs.regs;

import appeng.api.parts.PartModels;
import appeng.items.AEBaseItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.gtceu.PortableSpatialClonerGTCEu;
import net.oktawia.crazyae2addons.compat.gtceu.PortableSpatialStorageGTCEu;
import net.oktawia.crazyae2addons.items.*;
import net.oktawia.crazyae2addons.items.wireless.*;
import net.oktawia.crazyae2addons.parts.Display;
import net.oktawia.crazyae2addons.parts.EmitterTerminal;
import net.oktawia.crazyae2addons.parts.MultiLevelEmitter;
import net.oktawia.crazyae2addons.parts.RedstoneEmitter;
import net.oktawia.crazyae2addons.parts.RedstoneTerminal;
import net.oktawia.crazyae2addons.parts.TagLevelEmitter;
import net.oktawia.crazyae2addons.parts.p2p.RRFluidP2PTunnelPart;
import net.oktawia.crazyae2addons.parts.p2p.RRItemP2PTunnelPart;
import net.oktawia.crazyae2addons.parts.p2p.WormholeP2PTunnelPart;

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

    // --- Terminals ---
    public static final RegistryObject<PartItem<Display>> DISPLAY =
            ITEMS.register("display",
                    () -> new PartItem<>(new Item.Properties(), Display.class, Display::new));

    public static final RegistryObject<PartItem<EmitterTerminal>> EMITTER_TERMINAL =
            ITEMS.register("emitter_terminal",
                    () -> new PartItem<>(new Item.Properties(), EmitterTerminal.class, EmitterTerminal::new));

    public static final RegistryObject<WirelessEmitterTerminalItem> WIRELESS_EMITTER_TERMINAL =
            ITEMS.register("wireless_emitter_terminal", WirelessEmitterTerminalItem::new);

    public static final RegistryObject<PartItem<RedstoneTerminal>> REDSTONE_TERMINAL =
            ITEMS.register("redstone_terminal",
                    () -> new PartItem<>(new Item.Properties(), RedstoneTerminal.class, RedstoneTerminal::new));

    public static final RegistryObject<WirelessRedstoneTerminal> WIRELESS_REDSTONE_TERMINAL =
            ITEMS.register("wireless_redstone_terminal", WirelessRedstoneTerminal::new);

    public static final RegistryObject<WirelessNotificationTerminalItem> WIRELESS_NOTIFICATION_TERMINAL =
            ITEMS.register("wireless_notification_terminal", WirelessNotificationTerminalItem::new);

    // --- Level Emitters ---
    public static final RegistryObject<PartItem<MultiLevelEmitter>> MULTI_LEVEL_EMITTER =
            ITEMS.register("multi_level_emitter",
                    () -> new PartItem<>(new Item.Properties(), MultiLevelEmitter.class, MultiLevelEmitter::new));

    public static final RegistryObject<PartItem<TagLevelEmitter>> TAG_LEVEL_EMITTER =
            ITEMS.register("tag_level_emitter",
                    () -> new PartItem<>(new Item.Properties(), TagLevelEmitter.class, TagLevelEmitter::new));

    public static final RegistryObject<PartItem<RedstoneEmitter>> REDSTONE_EMITTER =
            ITEMS.register("redstone_emitter",
                    () -> new PartItem<>(new Item.Properties(), RedstoneEmitter.class, RedstoneEmitter::new));

    // --- P2P Tunnels ---
    public static final RegistryObject<PartItem<WormholeP2PTunnelPart>> WORMHOLE =
            ITEMS.register("wormhole",
                    () -> new PartItem<>(new Item.Properties(), WormholeP2PTunnelPart.class, WormholeP2PTunnelPart::new));

    public static final RegistryObject<PartItem<RRItemP2PTunnelPart>> RR_ITEM_P2P =
            ITEMS.register("round_robin_item_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(), RRItemP2PTunnelPart.class, RRItemP2PTunnelPart::new));

    public static final RegistryObject<PartItem<RRFluidP2PTunnelPart>> RR_FLUID_P2P =
            ITEMS.register("round_robin_fluid_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(), RRFluidP2PTunnelPart.class, RRFluidP2PTunnelPart::new));

    // --- Spatial ---
    public static final RegistryObject<PortableSpatialStorage> PORTABLE_SPATIAL_STORAGE =
            ITEMS.register("portable_spatial_storage", () ->
                    IsModLoaded.GTCEU ? new PortableSpatialStorageGTCEu(new Item.Properties()) : new PortableSpatialStorage(new Item.Properties()));

    public static final RegistryObject<PortableSpatialCloner> PORTABLE_SPATIAL_CLONER =
            ITEMS.register("portable_spatial_cloner", () ->
                    IsModLoaded.GTCEU ? new PortableSpatialClonerGTCEu(new Item.Properties()) : new PortableSpatialCloner(new Item.Properties()));

    // --- Tools ---
    public static final RegistryObject<CpuPrioTunerItem> CPU_PRIO_TUNER =
            ITEMS.register("cpu_priority_tuner",
                    () -> new CpuPrioTunerItem(new Item.Properties()));

    public static final RegistryObject<TagViewCellItem> TAG_VIEW_CELL =
            ITEMS.register("tag_view_cell",
                    () -> new TagViewCellItem(new Item.Properties()));

    // --- Crazy Pattern Provider ---
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