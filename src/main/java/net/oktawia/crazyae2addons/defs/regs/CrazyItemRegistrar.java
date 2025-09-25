package net.oktawia.crazyae2addons.defs.regs;

import appeng.api.parts.PartModels;
import appeng.api.parts.IPart;
import appeng.items.AEBaseItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import appeng.items.storage.StorageTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.CC.CCDataExtractorPartItem;
import net.oktawia.crazyae2addons.compat.GregTech.*;
import net.oktawia.crazyae2addons.items.*;
import net.oktawia.crazyae2addons.mobstorage.*;
import net.oktawia.crazyae2addons.parts.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CrazyItemRegistrar {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, CrazyAddons.MODID);

    private static final List<DeferredHolder<Item, ? extends AEBaseItem>> CARDS = new ArrayList<>();

    public static List<? extends AEBaseItem> getCards() {
        return CARDS.stream().map(DeferredHolder::get).toList();
    }

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

    private static <T extends AEBaseItem> DeferredHolder<Item, T> regCard(String id, Supplier<T> sup) {
        DeferredHolder<Item, T> ro = ITEMS.register(id, sup);
        CARDS.add(ro);
        return ro;
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

    public static final DeferredHolder<Item, StructurePatternCopyItem> DEBUG_STRUCTURE_COPY_THING =
            ITEMS.register("debug_structure_copy_thingy",
                    () -> new StructurePatternCopyItem(new Item.Properties()));

    public static final DeferredHolder<Item, LogicCard> LOGIC_CARD = regCard(
            "logic_card", () -> new LogicCard(new Item.Properties()));

    public static final DeferredHolder<Item, RRItemP2PTunnelPartItem> RR_ITEM_P2P_TUNNEL_PART =
            ITEMS.register("round_robin_item_p2p_tunnel",
                    () -> new RRItemP2PTunnelPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, NBTExportBusPartItem> NBT_EXPORT_BUS_PART_ITEM =
            ITEMS.register("nbt_export_bus",
                    () -> new NBTExportBusPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, NBTStorageBusPartItem> NBT_STORAGE_BUS_PART_ITEM =
            ITEMS.register("nbt_storage_bus",
                    () -> new NBTStorageBusPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, DisplayPartItem> DISPLAY_MONITOR_PART_ITEM =
            ITEMS.register("display_monitor",
                    () -> new DisplayPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, PartItem<? extends DataExtractorPart>> DATA_EXTRACTOR_PART_ITEM =
            ITEMS.register("data_extractor",
                    () -> IsModLoaded.isCCLoaded()
                            ? new CCDataExtractorPartItem(new Item.Properties())
                            : new DataExtractorPartItem(new Item.Properties())
            );

    public static final DeferredHolder<Item, ChunkyFluidP2PTunnelPartItem> CHUNKY_FLUID_P2P_TUNNEL_PART =
            ITEMS.register("chunky_fluid_p2p_tunnel",
                    () -> new ChunkyFluidP2PTunnelPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, PartItem<? extends EnergyExporterPart>> ENERGY_EXPORTER_PART_ITEM =
            ITEMS.register("energy_exporter",
                    () -> IsModLoaded.isGTCEuLoaded()
                            ? new GTEnergyExporterPartItem(new Item.Properties())
                            : new EnergyExporterPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, CircuitUpgradeCard> CIRCUIT_UPGRADE_CARD_ITEM =
            ITEMS.register("circuit_upgrade_card",
                    () -> new CircuitUpgradeCard(new Item.Properties()));

    public static final DeferredHolder<Item, EntityTickerPartItem> ENTITY_TICKER_PART_ITEM =
            ITEMS.register("entity_ticker",
                    () -> new EntityTickerPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, CrazyPatternModifierItem> CRAZY_PATTERN_MODIFIER_ITEM =
            ITEMS.register("crazy_pattern_modifier",
                    () -> new CrazyPatternModifierItem(new Item.Properties()));
    public static final DeferredHolder<Item, CrazyPatternMultiplierItem> CRAZY_PATTERN_MULTIPLIER_ITEM =
            ITEMS.register("crazy_pattern_multiplier",
                    () -> new CrazyPatternMultiplierItem(new Item.Properties()));

    public static final DeferredHolder<Item, XpShardItem> XP_SHARD_ITEM =
            ITEMS.register("xp_shard", () -> new XpShardItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> MOB_CELL_HOUSING =
            ITEMS.register("mob_cell_housing",
                    () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> MOB_CELL_1K = ITEMS.register("mob_storage_cell_1k",
            () -> new MobStorageCell(new Item.Properties().stacksTo(1), StorageTier.SIZE_1K, MOB_CELL_HOUSING.get()));
    public static final DeferredHolder<Item, Item> MOB_CELL_4K = ITEMS.register("mob_storage_cell_4k",
            () -> new MobStorageCell(new Item.Properties().stacksTo(1), StorageTier.SIZE_4K, MOB_CELL_HOUSING.get()));
    public static final DeferredHolder<Item, Item> MOB_CELL_16K = ITEMS.register("mob_storage_cell_16k",
            () -> new MobStorageCell(new Item.Properties().stacksTo(1), StorageTier.SIZE_16K, MOB_CELL_HOUSING.get()));
    public static final DeferredHolder<Item, Item> MOB_CELL_64K = ITEMS.register("mob_storage_cell_64k",
            () -> new MobStorageCell(new Item.Properties().stacksTo(1), StorageTier.SIZE_64K, MOB_CELL_HOUSING.get()));
    public static final DeferredHolder<Item, Item> MOB_CELL_256K = ITEMS.register("mob_storage_cell_256k",
            () -> new MobStorageCell(new Item.Properties().stacksTo(1), StorageTier.SIZE_256K,
                    MOB_CELL_HOUSING.get()));

    public static final DeferredHolder<Item, MobAnnihilationPlaneItem> MOB_ANNIHILATION_PLANE =
            ITEMS.register("mob_annihilation_plane",
                    () -> new MobAnnihilationPlaneItem(new Item.Properties()));

    public static final DeferredHolder<Item, MobFormationPlaneItem> MOB_FORMATION_PLANE =
            ITEMS.register("mob_formation_plane",
                    () -> new MobFormationPlaneItem(new Item.Properties()));

    public static final DeferredHolder<Item, MobExportBusItem> MOB_EXPORT_BUS =
            ITEMS.register("mob_export_bus",
                    () -> new MobExportBusItem(new Item.Properties()));

    public static final DeferredHolder<Item, LootingUpgradeCard> LOOTING_UPGRADE_CARD =
            ITEMS.register("looting_upgrade_card",
                    () -> new LootingUpgradeCard(new Item.Properties()));

    public static final DeferredHolder<Item, ExperienceUpgradeCard> EXPERIENCE_UPGRADE_CARD =
            ITEMS.register("experience_upgrade_card",
                    () -> new ExperienceUpgradeCard(new Item.Properties()));

    public static final DeferredHolder<Item, CrazyEmitterMultiplierItem> CRAZY_EMITTER_MULTIPLIER_ITEM =
            ITEMS.register("crazy_emitter_multiplier",
                    () -> new CrazyEmitterMultiplierItem(new Item.Properties()));

    public static final DeferredHolder<Item, CrazyCalculatorItem> CRAZY_CALCULATOR_ITEM =
            ITEMS.register("crazy_calculator",
                    () -> new CrazyCalculatorItem(new Item.Properties()));

    public static final DeferredHolder<Item, RedstoneEmitterPartItem> REDSTONE_EMITTER =
            ITEMS.register("redstone_emitter",
                    () -> new RedstoneEmitterPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, RedstoneTerminalPartItem> REDSTONE_TERMINAL =
            ITEMS.register("redstone_terminal",
                    () -> new RedstoneTerminalPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, WirelessRedstoneTerminal> WIRELESS_REDSTONE_TERMINAL =
            ITEMS.register("wireless_redstone_terminal",
                    WirelessRedstoneTerminal::new);

    public static final DeferredHolder<Item, VariableTerminalPartItem> VARIABLE_TERMINAL =
            ITEMS.register("variable_terminal",
                    () -> new VariableTerminalPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, SuperSingularityItem> SUPER_SINGULARITY =
            ITEMS.register("super_singularity",
                    () -> new SuperSingularityItem(new Item.Properties()));

    public static final DeferredHolder<Item, ExtractingFEP2PTunnelPartItem> EXTRACTING_FE_P2P_TUNNEL =
            ITEMS.register("extracting_fe_p2p_tunnel",
                    () -> new ExtractingFEP2PTunnelPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, ExtractingItemP2PTunnelPartItem> EXTRACTING_ITEM_P2P_TUNNEL =
            ITEMS.register("extracting_item_p2p_tunnel",
                    () -> new ExtractingItemP2PTunnelPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, ExtractingFluidP2PTunnelPartItem> EXTRACTING_FLUID_P2P_TUNNEL =
            ITEMS.register("extracting_fluid_p2p_tunnel",
                    () -> new ExtractingFluidP2PTunnelPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, WormHoleP2PTunnelPartItem> WORMHOLE_P2P_TUNNEL =
            ITEMS.register("wormhole_tunnel",
                    () -> new WormHoleP2PTunnelPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, EnergyInterfacePartItem> ENERGY_INTERFACE_PART =
            ITEMS.register("energy_interface",
                    () -> new EnergyInterfacePartItem(new Item.Properties()));

    public static final DeferredHolder<Item, BuilderPatternItem> BUILDER_PATTERN =
            ITEMS.register("builder_pattern",
                    () -> new BuilderPatternItem(new Item.Properties()));

    public static final DeferredHolder<Item, CrazyUpgradeItem> CRAZY_UPGRADE =
            ITEMS.register("crazy_upgrade",
                    () -> new CrazyUpgradeItem(new Item.Properties()));

    public static final DeferredHolder<Item, PlayerDataExtractorPartItem> PLAYER_DATA_EXTRACTOR =
            ITEMS.register("player_data_extractor",
                    () -> new PlayerDataExtractorPartItem(new Item.Properties()));

    public static final DeferredHolder<Item, DataFlowPatternItem> DATAFLOW_PATTERN =
            ITEMS.register("dataflow_pattern",
                    () -> new DataFlowPatternItem(new Item.Properties()));

    public static final DeferredHolder<Item, NbtViewCellItem> NBT_VIEW_CELL =
            ITEMS.register("nbt_view_cell",
                    () -> new NbtViewCellItem(new Item.Properties()));

    public static final DeferredHolder<Item, TagViewCellItem> TAG_VIEW_CELL =
            ITEMS.register("tag_view_cell",
                    () -> new TagViewCellItem(new Item.Properties()));

    public static final DeferredHolder<Item, CpuPrioTunerItem> CPU_PRIO_TUNER =
            ITEMS.register("cpu_prio_tuner",
                    () -> new CpuPrioTunerItem(new Item.Properties()));

    public static final DeferredHolder<Item, StructureGadgetItem> STRUCTURE_GADGET =
            ITEMS.register("nokia_3310",
                    () -> new StructureGadgetItem(new Item.Properties()));

    public static final DeferredHolder<Item, DataDrive> DATA_DRIVE =
            ITEMS.register("data_drive",
                    () -> new DataDrive(new Item.Properties()));

    public static final DeferredHolder<Item, UnassembledCrazyPatternProviderItem> UNASSEMBLED_CRAZY_PATTERN_PROVIDER =
            ITEMS.register("unassembled_crazy_pattern_provider",
                    () -> new UnassembledCrazyPatternProviderItem(new Item.Properties()));

    public static final DeferredHolder<Item, MobKeyItem> MOB_KEY_ITEM =
            ITEMS.register("mob_key",
                    () -> new MobKeyItem(new Item.Properties()));

    public static final DeferredHolder<Item, MobKeySelectorItem> MOB_KEY_SELECTOR_ITEM =
            ITEMS.register("mob_key_selector",
                    () -> new MobKeySelectorItem(new Item.Properties()));

    private CrazyItemRegistrar() {}
}
