package net.oktawia.crazyae2addons;

import appeng.api.features.GridLinkables;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.mojang.logging.LogUtils;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.oktawia.crazyae2addons.client.screens.CrazyConfigScreen;
import net.oktawia.crazyae2addons.client.renderer.preview.PortableSpatialStoragePreviewRenderer;
import net.oktawia.crazyae2addons.compat.gtceu.GTWormholeCapabilityExtension;
import net.oktawia.crazyae2addons.compat.gtceu.PortableSpatialStoragePreviewRendererGTCEu;
import net.oktawia.crazyae2addons.parts.p2p.WormholeP2PTunnelPart;
import net.oktawia.crazyae2addons.defs.Screens;
import net.oktawia.crazyae2addons.defs.UpgradeCards;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyCreativeTabRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.ldlib.CrazyLDLibPlugin;
import net.oktawia.crazyae2addons.logic.wireless.WirelessEmitterTerminalItemLogicHost;
import net.oktawia.crazyae2addons.logic.wireless.WirelessNotificationTerminalItemLogicHost;
import net.oktawia.crazyae2addons.logic.wireless.WirelessRedstoneTerminalItemLogicHost;
import net.oktawia.crazyae2addons.menus.item.WirelessEmitterTerminalMenu;
import net.oktawia.crazyae2addons.menus.item.WirelessNotificationTerminalMenu;
import net.oktawia.crazyae2addons.menus.item.WirelessRedstoneTerminalMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;

@Mod(CrazyAddons.MODID)
public class CrazyAddons {
    public static final String MODID = "crazyae2addons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrazyAddons() {
        LogUtils.getLogger().info("Loading Crazy AE2 Addons");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CrazyConfig.COMMON_SPEC);
        modEventBus.addListener(this::commonSetup);

        CrazyItemRegistrar.ITEMS.register(modEventBus);
        CrazyBlockRegistrar.BLOCKS.register(modEventBus);
        CrazyBlockRegistrar.BLOCK_ITEMS.register(modEventBus);
        CrazyBlockEntityRegistrar.BLOCK_ENTITIES.register(modEventBus);
        CrazyMenuRegistrar.MENU_TYPES.register(modEventBus);
        CrazyRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        CrazyRecipes.RECIPE_TYPES.register(modEventBus);
        CrazyLDLibPlugin.init();

        modEventBus.addListener(this::registerCreativeTab);


        modEventBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(ForgeRegistries.ITEMS.getRegistryKey())) {
                GridLinkables.register(CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
                IUniversalWirelessTerminalItem notifTerm = CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get();
                Objects.requireNonNull(notifTerm);
                WUTHandler.addTerminal("wireless_notification_terminal",
                    notifTerm::tryOpen,
                    WirelessNotificationTerminalItemLogicHost::new,
                    WirelessNotificationTerminalMenu.TYPE,
                    notifTerm,
                    "wireless_notification_terminal",
                    "item.crazyae2addons.wireless_notification_terminal"
                );

                GridLinkables.register(CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
                IUniversalWirelessTerminalItem emitterTerm = CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get();
                Objects.requireNonNull(emitterTerm);
                WUTHandler.addTerminal("wireless_emitter_terminal",
                        emitterTerm::tryOpen,
                        WirelessEmitterTerminalItemLogicHost::new,
                        WirelessEmitterTerminalMenu.TYPE,
                        emitterTerm,
                        "wireless_emitter_terminal",
                        "item.crazyae2addons.wireless_emitter_terminal"
                );

                GridLinkables.register(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
                IUniversalWirelessTerminalItem redstoneTerm = CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get();
                Objects.requireNonNull(redstoneTerm);
                WUTHandler.addTerminal("wireless_redstone_terminal",
                        redstoneTerm::tryOpen,
                        WirelessRedstoneTerminalItemLogicHost::new,
                        WirelessRedstoneTerminalMenu.TYPE,
                        redstoneTerm,
                        "wireless_redstone_terminal",
                        "item.crazyae2addons.wireless_redstone_terminal"
                );
            }
        });
    }

    public static @NotNull ResourceLocation makeId(String path) {
        return new ResourceLocation(MODID, path);
    }

    private void registerCreativeTab(final RegisterEvent evt) {
        if (evt.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            evt.register(
                    Registries.CREATIVE_MODE_TAB,
                    CrazyCreativeTabRegistrar.ID,
                    () -> CrazyCreativeTabRegistrar.TAB
            );
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        if (IsModLoaded.GTCEU) {
            WormholeP2PTunnelPart.registerExtension(GTWormholeCapabilityExtension::new);
        }
        event.enqueueWork(() -> {
            new UpgradeCards(event);
            CrazyBlockEntityRegistrar.setupBlockEntityTypes();
            GridLinkables.register(CrazyItemRegistrar.PORTABLE_SPATIAL_CLONER.get(), PortableSpatialCloner.LINKABLE_HANDLER);
            NetworkHandler.registerMessages();
        });
    }

    @Mod.EventBusSubscriber(
            modid = CrazyAddons.MODID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> CrazyConfigScreen.create(parent))
            );
            Screens.register();
            event.enqueueWork(() -> {
                PortableSpatialStoragePreviewRenderer renderer =
                        IsModLoaded.GTCEU
                                ? new PortableSpatialStoragePreviewRendererGTCEu()
                                : new PortableSpatialStoragePreviewRenderer();

                MinecraftForge.EVENT_BUS.register(renderer);
            });
        }

        @SubscribeEvent
        public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders evt) {
            try {
                CrazyItemRegistrar.registerPartModels();
            } catch (Exception e) {
                LOGGER.debug("Register Geometry Loaders failed {}", e.getLocalizedMessage());
            }
        }
    }
}