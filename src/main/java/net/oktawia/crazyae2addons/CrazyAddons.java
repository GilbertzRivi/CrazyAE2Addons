package net.oktawia.crazyae2addons;

import appeng.api.AECapabilities;
import appeng.api.features.GridLinkables;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.parts.RegisterPartCapabilitiesEvent;
import com.mojang.logging.LogUtils;
import de.mari_023.ae2wtlib.api.gui.Icon;
import de.mari_023.ae2wtlib.api.registration.AddTerminalEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.UpgradeCards;
import net.oktawia.crazyae2addons.defs.regs.*;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;
import net.oktawia.crazyae2addons.entities.AutoEnchanterBE;
import net.oktawia.crazyae2addons.entities.PenroseControllerBE;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.parts.p2p.ChunkyFluidP2PTunnelPart;
import net.oktawia.crazyae2addons.parts.p2p.RRItemP2PTunnelPart;
import net.oktawia.crazyae2addons.items.wireless.EmitterTerminalMenuHost;
import net.oktawia.crazyae2addons.items.wireless.WirelessEmitterTerminalItem;
import net.oktawia.crazyae2addons.items.wireless.WirelessEmitterTerminalMenu;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(CrazyAddons.MODID)
public class CrazyAddons {
    public static final String MODID = "crazyae2addons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrazyAddons(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Loading Crazy AE2 Addons");

        modContainer.registerConfig(ModConfig.Type.COMMON, CrazyConfig.COMMON_SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(CrazyItemRegistrar::registerPartModels);

        CrazyItemRegistrar.ITEMS.register(modEventBus);
        CrazyBlockRegistrar.BLOCKS.register(modEventBus);
        CrazyBlockRegistrar.BLOCK_ITEMS.register(modEventBus);
        CrazyBlockEntityRegistrar.BLOCK_ENTITIES.register(modEventBus);
        CrazyMenuRegistrar.MENU_TYPES.register(modEventBus);
        CrazyDataComponents.COMPONENTS.register(modEventBus);

        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::registerCreativeTab);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPartCapabilities);
        modEventBus.addListener(NetworkHandler::registerMessages);

        AddTerminalEvent.register(TerminalEvent -> {
            TerminalEvent.builder(
                    "emitter_terminal",
                    EmitterTerminalMenuHost::new,
                    WirelessEmitterTerminalMenu.TYPE,
                    CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get(),
                    Icon.CRAFTING
            )
            .upgradeCount(1)
            .addTerminal();
        });
    }

    public static @NotNull ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.MENU)) {
            event.register(Registries.MENU, WirelessEmitterTerminalMenu.ID, () -> WirelessEmitterTerminalMenu.TYPE);
        }
    }

    private void registerCreativeTab(final RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            event.register(
                    Registries.CREATIVE_MODE_TAB,
                    CrazyCreativeTabRegistrar.ID,
                    () -> CrazyCreativeTabRegistrar.TAB
            );
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var beType : CrazyBlockEntityRegistrar.getEntities()) {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, beType,
                    (be, ctx) -> be instanceof IInWorldGridNodeHost host ? host : null);
        }

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                CrazyBlockEntityRegistrar.AMPERE_METER_BE.get(),
                (be, dir) -> be instanceof AmpereMeterBE ampereMeter ? ampereMeter.getEnergyStorage(dir) : null);

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                CrazyBlockEntityRegistrar.AUTO_ENCHANTER_BE.get(),
                (be, dir) -> be instanceof AutoEnchanterBE enchanter ? enchanter.getItemHandler(dir) : null);

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                CrazyBlockEntityRegistrar.PENROSE_CONTROLLER_BE.get(),
                (be, dir) -> be instanceof PenroseControllerBE ctrl
                        && CrazyConfig.COMMON.PenroseFEOutputEnabled.get()
                        ? ctrl.getEnergyStorage(dir) : null);
    }

    private void registerPartCapabilities(RegisterPartCapabilitiesEvent event) {
        event.register(Capabilities.ItemHandler.BLOCK, (part, dir) -> part.getExposedApi(), RRItemP2PTunnelPart.class);
        event.register(Capabilities.FluidHandler.BLOCK, (part, dir) -> part.getExposedApi(), ChunkyFluidP2PTunnelPart.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            new UpgradeCards(event);
            CrazyBlockEntityRegistrar.setupBlockEntityTypes();
            GridLinkables.register(CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get(), WirelessEmitterTerminalItem.LINKABLE_HANDLER);
        });
    }
}
