package net.oktawia.crazyae2addons;

import appeng.api.features.GridLinkables;
import appeng.api.stacks.AEKeyTypes;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.mojang.logging.LogUtils;
import de.mari_023.ae2wtlib.api.registration.AddTerminalEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.oktawia.crazyae2addons.datavariables.FlowNodeRegistry;
import net.oktawia.crazyae2addons.defs.Screens;
import net.oktawia.crazyae2addons.defs.UpgradeCards;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyCreativeTabRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyFluidRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.logic.WirelessRedstoneTerminalItemLogicHost;
import net.oktawia.crazyae2addons.menus.WirelessRedstoneTerminalMenu;
import net.oktawia.crazyae2addons.mobstorage.EntityTypeRenderer;
import net.oktawia.crazyae2addons.mobstorage.MobKeyType;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.renderer.AutoBuilderBERenderer;
import net.oktawia.crazyae2addons.renderer.PreviewTooltipRenderer;
import net.oktawia.crazyae2addons.renderer.preview.EntropyCradlePreviewRenderer;
import net.oktawia.crazyae2addons.renderer.preview.EnergyStoragePreviewRenderer;
import net.oktawia.crazyae2addons.renderer.preview.MobFarmPreviewRenderer;
import net.oktawia.crazyae2addons.renderer.preview.PenrosePreviewRenderer;
import net.oktawia.crazyae2addons.renderer.preview.SpawnerExtractorPreviewRenderer;
import org.jetbrains.annotations.NotNull;

@Mod(CrazyAddons.MODID)
public class CrazyAddons {
    public static final String MODID = "crazyae2addons";

    public CrazyAddons(IEventBus modEventBus, ModContainer modContainer) {
        LogUtils.getLogger().info("Loading Crazy AE2 Addons");

        modContainer.registerConfig(ModConfig.Type.COMMON, CrazyConfig.COMMON_SPEC);

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
            if (event.getRegistryKey().equals(Registries.ITEM)) {
                GridLinkables.register(
                        CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(),
                        WirelessTerminalItem.LINKABLE_HANDLER
                );
            }
        });

        AddTerminalEvent.register(ev -> ev.builder(
                                CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get(),
                                WirelessRedstoneTerminalMenu.TYPE,
                                WirelessRedstoneTerminalItemLogicHost::new
                        )
                        .id(ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "wireless_redstone_terminal"))
                        .translationKey("item." + CrazyAddons.MODID + ".wireless_redstone_terminal")
                        .addTerminal()
        );

        modEventBus.addListener(this::registerCreativeTab);

        NeoForge.EVENT_BUS.register(this);
    }

    public static @NotNull ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
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
        event.enqueueWork(() -> {
            new UpgradeCards(event);
            MobKeyType.registerContainerItemStrategies();
            CrazyBlockEntityRegistrar.setupBlockEntityTypes();
            NetworkHandler.registerClientPackets();
            NetworkHandler.registerServerPackets();
            FlowNodeRegistry.init();
        });
    }

    @EventBusSubscriber(
            modid = CrazyAddons.MODID,
            value = Dist.CLIENT
    )
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            EntityTypeRenderer.initialize();
            Screens.register();

            NeoForge.EVENT_BUS.addListener(PenrosePreviewRenderer::onRender);
            NeoForge.EVENT_BUS.addListener(EntropyCradlePreviewRenderer::onRender);
            NeoForge.EVENT_BUS.addListener(EnergyStoragePreviewRenderer::onRender);
            NeoForge.EVENT_BUS.addListener(SpawnerExtractorPreviewRenderer::onRender);
            NeoForge.EVENT_BUS.addListener(MobFarmPreviewRenderer::onRender);

            ItemBlockRenderTypes.setRenderLayer(
                    CrazyFluidRegistrar.RESEARCH_FLUID_BLOCK.get(),
                    RenderType.translucent()
            );
        }

        @SubscribeEvent
        public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders evt) {
            try {
                CrazyItemRegistrar.registerPartModels();
            } catch (Exception ignored) {
            }
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    CrazyBlockEntityRegistrar.AUTO_BUILDER_BE.get(),
                    AutoBuilderBERenderer::new
            );
        }

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerBelowAll(
                    makeId("crazytooltip"),
                    PreviewTooltipRenderer.TOOLTIP
            );
        }
    }
}
