package net.oktawia.crazyae2addons;

import appeng.api.features.GridLinkables;
import appeng.api.stacks.AEKeyTypes;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.mojang.logging.LogUtils;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.oktawia.crazyae2addons.defs.Screens;
import net.oktawia.crazyae2addons.defs.UpgradeCards;
import net.oktawia.crazyae2addons.defs.regs.*;
import net.oktawia.crazyae2addons.items.PortableAutobuilder;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.WirelessNotificationTerminalItemLogicHost;
import net.oktawia.crazyae2addons.logic.WirelessRedstoneTerminalItemLogicHost;
import net.oktawia.crazyae2addons.menus.WirelessNotificationTerminalMenu;
import net.oktawia.crazyae2addons.menus.WirelessRedstoneTerminalMenu;
import net.oktawia.crazyae2addons.mobstorage.EntityTypeRenderer;
import net.oktawia.crazyae2addons.mobstorage.MobKeyType;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.renderer.AutoBuilderBERenderer;
import net.oktawia.crazyae2addons.renderer.PreviewTooltipRenderer;
import net.oktawia.crazyae2addons.renderer.ResearchPedestalTopRenderer;
import net.oktawia.crazyae2addons.renderer.preview.*;
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
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                CrazyConfig.COMMON_SPEC
        );
        modEventBus.addListener(this::commonSetup);

        CrazyItemRegistrar.ITEMS.register(modEventBus);

        CrazyBlockRegistrar.BLOCKS.register(modEventBus);
        CrazyBlockRegistrar.BLOCK_ITEMS.register(modEventBus);
        CrazyBlockEntityRegistrar.BLOCK_ENTITIES.register(modEventBus);
        CrazyRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        CrazyRecipes.RECIPE_TYPES.register(modEventBus);
        CrazyFluidRegistrar.register(modEventBus);

        CrazyMenuRegistrar.MENU_TYPES.register(modEventBus);

        modEventBus.addListener((RegisterEvent event) -> {
            if (!event.getRegistryKey().equals(Registries.BLOCK)) {
                return;
            }
            AEKeyTypes.register(MobKeyType.TYPE);
        });

        modEventBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(ForgeRegistries.ITEMS.getRegistryKey())) {
                GridLinkables.register(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
                GridLinkables.register(CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
                IUniversalWirelessTerminalItem term = CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get();
                IUniversalWirelessTerminalItem term2 = CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get();
                Objects.requireNonNull(term);
                Objects.requireNonNull(term2);
                WUTHandler.addTerminal("wireless_redstone_terminal",
                        term::tryOpen,
                        WirelessRedstoneTerminalItemLogicHost::new,
                        WirelessRedstoneTerminalMenu.TYPE,
                        term,
                        "wireless_redstone_terminal",
                        "item.crazyae2addons.wireless_redstone_terminal"
                );
                WUTHandler.addTerminal("wireless_notification_terminal",
                        term2::tryOpen,
                        WirelessNotificationTerminalItemLogicHost::new,
                        WirelessNotificationTerminalMenu.TYPE,
                        term2,
                        "wireless_notification_terminal",
                        "item.crazyae2addons.wireless_notification_terminal"
                );}
            }
        );

        modEventBus.addListener(this::registerCreativeTab);

        CrazyRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        CrazyRecipes.RECIPE_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static @NotNull ResourceLocation makeId(String path) {
        return new ResourceLocation(MODID, path);
    }

    private void registerCreativeTab(final RegisterEvent evt) {
        if (evt.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            evt.register(Registries.CREATIVE_MODE_TAB,
                CrazyCreativeTabRegistrar.ID,
                () -> CrazyCreativeTabRegistrar.TAB);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            new UpgradeCards(event);
            MobKeyType.registerContainerItemStrategies();
            CrazyBlockEntityRegistrar.setupBlockEntityTypes();
            NetworkHandler.registerMessages();
            GridLinkables.register(CrazyItemRegistrar.PORTABLE_BUILDER.get(), PortableAutobuilder.LINKABLE_HANDLER);
            if (ModList.get().isLoaded("computercraft")) {
                net.oktawia.crazyae2addons.compat.CC.CCCompat.init();
            }
        });
    }

    @Mod.EventBusSubscriber(
            modid = CrazyAddons.MODID,
            bus   = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT
    )
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityTypeRenderer.initialize();
            Screens.register();
            MinecraftForge.EVENT_BUS.addListener(PenrosePreviewRenderer::onRender);
            MinecraftForge.EVENT_BUS.addListener(EntropyCradlePreviewRenderer::onRender);
            MinecraftForge.EVENT_BUS.addListener(SpawnerExtractorPreviewRenderer::onRender);
            MinecraftForge.EVENT_BUS.addListener(MobFarmPreviewRenderer::onRender);
            MinecraftForge.EVENT_BUS.addListener(ResearchUnitPreviewRenderer::onRender);
            ItemBlockRenderTypes.setRenderLayer(CrazyFluidRegistrar.RESEARCH_FLUID_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(CrazyBlockRegistrar.RESEARCH_PEDESTAL_TOP.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(CrazyBlockRegistrar.RESEARCH_CABLE_BLOCK.get(), RenderType.cutout());
        }
        @SubscribeEvent
        public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders evt) {
            try {
                CrazyItemRegistrar.registerPartModels();
            } catch (Exception ignored) {}
        }
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    CrazyBlockEntityRegistrar.AUTO_BUILDER_BE.get(),
                    AutoBuilderBERenderer::new
            );
            event.registerBlockEntityRenderer(
                    CrazyBlockEntityRegistrar.RESEARCH_PEDESTAL_TOP_BE.get(),
                    ResearchPedestalTopRenderer::new
            );
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerBelowAll("crazypreviewtooltip", PreviewTooltipRenderer.TOOLTIP);
        }
    }
}
