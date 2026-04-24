package net.oktawia.crazyae2addons.defs.regs;

import appeng.api.parts.PartModels;
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
import net.oktawia.crazyae2addons.items.CrazyPatternProviderPartItem;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.items.wireless.*;
import net.oktawia.crazyae2addons.parts.Display;
import net.oktawia.crazyae2addons.parts.EmitterTerminal;
import net.oktawia.crazyae2addons.parts.MultiLevelEmitter;
import net.oktawia.crazyae2addons.parts.RedstoneEmitter;
import net.oktawia.crazyae2addons.parts.RedstoneTerminal;
import net.oktawia.crazyae2addons.parts.TagLevelEmitter;

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

    public static final RegistryObject<CrazyPatternProviderPartItem> CRAZY_PATTERN_PROVIDER_PART =
            ITEMS.register(
                    "crazy_pattern_provider_part",
                    () -> new CrazyPatternProviderPartItem(new Item.Properties())
            );

    public static final RegistryObject<Item> CRAZY_UPGRADE =
            ITEMS.register(
                    "crazy_upgrade",
                    () -> new Item(new Item.Properties())
            );

    public static final RegistryObject<PartItem<Display>> DISPLAY =
            ITEMS.register(
                    "display",
                    () -> new PartItem<>(new Item.Properties(), Display.class, Display::new)
            );

    public static final RegistryObject<WirelessNotificationTerminalItem> WIRELESS_NOTIFICATION_TERMINAL =
            ITEMS.register("wireless_notification_terminal", WirelessNotificationTerminalItem::new);

    public static final RegistryObject<PartItem<EmitterTerminal>> EMITTER_TERMINAL =
            ITEMS.register(
                    "emitter_terminal",
                    () -> new PartItem<>(new Item.Properties(), EmitterTerminal.class, EmitterTerminal::new)
            );

    public static final RegistryObject<WirelessEmitterTerminalItem> WIRELESS_EMITTER_TERMINAL =
            ITEMS.register("wireless_emitter_terminal", WirelessEmitterTerminalItem::new);

    public static final RegistryObject<PartItem<MultiLevelEmitter>> MULTI_LEVEL_EMITTER =
            ITEMS.register(
                    "multi_level_emitter",
                    () -> new PartItem<>(new Item.Properties(), MultiLevelEmitter.class, MultiLevelEmitter::new)
            );

    public static final RegistryObject<PartItem<TagLevelEmitter>> TAG_LEVEL_EMITTER =
            ITEMS.register(
                    "tag_level_emitter",
                    () -> new PartItem<>(new Item.Properties(), TagLevelEmitter.class, TagLevelEmitter::new)
            );

    public static final RegistryObject<PartItem<RedstoneTerminal>> REDSTONE_TERMINAL =
            ITEMS.register(
                    "redstone_terminal",
                    () -> new PartItem<>(new Item.Properties(), RedstoneTerminal.class, RedstoneTerminal::new)
            );

    public static final RegistryObject<WirelessRedstoneTerminal> WIRELESS_REDSTONE_TERMINAL =
            ITEMS.register("wireless_redstone_terminal", WirelessRedstoneTerminal::new);

    public static final RegistryObject<PartItem<RedstoneEmitter>> REDSTONE_EMITTER =
            ITEMS.register("redstone_emitter",
                    () -> new PartItem<>(new Item.Properties(), RedstoneEmitter.class, RedstoneEmitter::new)
            );

    public static final RegistryObject<PortableSpatialStorage> PORTABLE_SPATIAL_STORAGE =
            ITEMS.register("portable_spatial_storage", () ->
                    IsModLoaded.GTCEU ? new PortableSpatialStorageGTCEu(new Item.Properties()) : new PortableSpatialStorage(new Item.Properties()));

    public static final RegistryObject<PortableSpatialCloner> PORTABLE_SPATIAL_CLONER =
            ITEMS.register("portable_spatial_cloner", () ->
                    IsModLoaded.GTCEU ? new PortableSpatialClonerGTCEu(new Item.Properties()) : new PortableSpatialCloner(new Item.Properties()));

    private CrazyItemRegistrar() {}
}