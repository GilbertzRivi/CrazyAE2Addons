package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.api.parts.PartModels;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.IsModLoaded;
import net.oktawia.crazyae2addonslite.compat.GregTech.*;
import net.oktawia.crazyae2addonslite.items.*;
import net.oktawia.crazyae2addonslite.parts.*;

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
                .filter(i -> i instanceof PartItem)
                .toList();
    }

    public static void registerPartModels() {
        for (Item item : getParts()) {
            if (item instanceof PartItem<?> partItem) {
                Class<?> partClass = partItem.getPartClass();
                if (partClass != null) {
                    PartModels.registerModels(PartModelsHelper.createModels(partClass.asSubclass(appeng.api.parts.IPart.class)));
                }
            }
        }
    }

    // =========================================================
    // =========================================================

    public static final RegistryObject<NBTExportBusPartItem> NBT_EXPORT_BUS_PART_ITEM =
            ITEMS.register("nbt_export_bus",
                    () -> new NBTExportBusPartItem(new Item.Properties()));

    public static final RegistryObject<NBTStorageBusPartItem> NBT_STORAGE_BUS_PART_ITEM =
            ITEMS.register("nbt_storage_bus",
                    () -> new NBTStorageBusPartItem(new Item.Properties()));

    // =========================================================
    // =========================================================

    public static final RegistryObject<RRItemP2PTunnelPartItem> RR_ITEM_P2P_TUNNEL_PART =
            ITEMS.register("round_robin_item_p2p_tunnel",
                    () -> new RRItemP2PTunnelPartItem(new Item.Properties()));

    public static final RegistryObject<RRFluidP2PTunnelPartItem> RR_FLUID_P2P_TUNNEL_PART =
            ITEMS.register("round_robin_fluid_p2p_tunnel",
                    () -> new RRFluidP2PTunnelPartItem(new Item.Properties()));

    public static final RegistryObject<PartItem<? extends WormholeP2PTunnelPart>> WORMHOLE_P2P_TUNNEL =
            ITEMS.register("wormhole_tunnel",
                    () -> IsModLoaded.isGTCEuLoaded()
                            ? new GTWormHoleP2PTunnelPartItem(new Item.Properties())
                            : new WormHoleP2PTunnelPartItem(new Item.Properties()));

    // =========================================================
    // =========================================================

    public static final RegistryObject<DisplayPartItem> DISPLAY_MONITOR_PART_ITEM =
            ITEMS.register("display_monitor",
                    () -> new DisplayPartItem(new Item.Properties()));

    public static final RegistryObject<PartItem<MultiStorageLevelEmitterPart>> MULTI_LEVEL_EMITTER_ITEM =
            ITEMS.register("multi_level_emitter",
                    () -> new PartItem<>(new Item.Properties(), MultiStorageLevelEmitterPart.class, MultiStorageLevelEmitterPart::new));

    public static final RegistryObject<RedstoneEmitterPartItem> REDSTONE_EMITTER =
            ITEMS.register("redstone_emitter",
                    () -> new RedstoneEmitterPartItem(new Item.Properties()));

    public static final RegistryObject<RedstoneTerminalPartItem> REDSTONE_TERMINAL =
            ITEMS.register("redstone_terminal",
                    () -> new RedstoneTerminalPartItem(new Item.Properties()));

    // =========================================================
    // =========================================================

    public static final RegistryObject<AutomationUpgradeCard> AUTOMATION_UPGRADE_CARD =
            ITEMS.register("automation_upgrade_card",
                    () -> new AutomationUpgradeCard(new Item.Properties()));

    public static final RegistryObject<AutomationUpgradeCard> PLAYER_UPGRADE_CARD =
            ITEMS.register("player_upgrade_card",
                    () -> new AutomationUpgradeCard(new Item.Properties()));

    public static final RegistryObject<CrazyUpgradeItem> CRAZY_UPGRADE =
            ITEMS.register("crazy_upgrade",
                    () -> new CrazyUpgradeItem(new Item.Properties()));

    // =========================================================
    // =========================================================

    public static final RegistryObject<CrazyPatternMultiplierItem> CRAZY_PATTERN_MULTIPLIER_ITEM =
            ITEMS.register("crazy_pattern_multiplier",
                    () -> new CrazyPatternMultiplierItem(new Item.Properties()));

    public static final RegistryObject<CrazyEmitterMultiplierItem> CRAZY_EMITTER_MULTIPLIER_ITEM =
            ITEMS.register("crazy_emitter_multiplier",
                    () -> new CrazyEmitterMultiplierItem(new Item.Properties()));

    public static final RegistryObject<CpuPrioTunerItem> CPU_PRIO_TUNER =
            ITEMS.register("cpu_prio_tuner",
                    () -> new CpuPrioTunerItem(new Item.Properties()));

    public static final RegistryObject<NbtViewCellItem> NBT_VIEW_CELL =
            ITEMS.register("nbt_view_cell",
                    () -> new NbtViewCellItem(new Item.Properties()));

    public static final RegistryObject<TagViewCellItem> TAG_VIEW_CELL =
            ITEMS.register("tag_view_cell",
                    () -> new TagViewCellItem(new Item.Properties()));

    // =========================================================
    // =========================================================

    public static final RegistryObject<WirelessRedstoneTerminal> WIRELESS_REDSTONE_TERMINAL =
            ITEMS.register("wireless_redstone_terminal",
                    WirelessRedstoneTerminal::new);

    public static final RegistryObject<WirelessNotificationTerminal> WIRELESS_NOTIFICATION_TERMINAL =
            ITEMS.register("wireless_notification_terminal",
                    WirelessNotificationTerminal::new);

    private CrazyItemRegistrar() {}
}
