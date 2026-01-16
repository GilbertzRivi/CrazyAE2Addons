package net.oktawia.crazyae2addonslite;

import appeng.api.features.GridLinkables;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.mojang.logging.LogUtils;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
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
import net.oktawia.crazyae2addonslite.defs.Screens;
import net.oktawia.crazyae2addonslite.defs.UpgradeCards;
import net.oktawia.crazyae2addonslite.defs.regs.*;
import net.oktawia.crazyae2addonslite.logic.WirelessNotificationTerminalItemLogicHost;
import net.oktawia.crazyae2addonslite.logic.WirelessRedstoneTerminalItemLogicHost;
import net.oktawia.crazyae2addonslite.menus.WirelessNotificationTerminalMenu;
import net.oktawia.crazyae2addonslite.menus.WirelessRedstoneTerminalMenu;
import net.oktawia.crazyae2addonslite.network.NetworkHandler;
import net.oktawia.crazyae2addonslite.parts.WormholeIPCompat;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Mod(CrazyAddons.MODID)
public class CrazyAddons {
    public static final String MODID = "crazyae2addonslite";

    public CrazyAddons() {
        LogUtils.getLogger().info("Loading Crazy AE2 Addons (lite)");

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
        CrazyMenuRegistrar.MENU_TYPES.register(modEventBus);

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
                        "item.crazyae2addonslite.wireless_redstone_terminal"
                );
                WUTHandler.addTerminal("wireless_notification_terminal",
                        term2::tryOpen,
                        WirelessNotificationTerminalItemLogicHost::new,
                        WirelessNotificationTerminalMenu.TYPE,
                        term2,
                        "wireless_notification_terminal",
                        "item.crazyae2addonslite.wireless_notification_terminal"
                );}
            }
        );

        modEventBus.addListener(this::registerCreativeTab);
        MinecraftForge.EVENT_BUS.register(this);

        if (IsModLoaded.isIPLoaded()) {
            MinecraftForge.EVENT_BUS.register(new WormholeIPCompat.Events());
        }
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
            CrazyBlockEntityRegistrar.setupBlockEntityTypes();
            NetworkHandler.registerMessages();
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
            Screens.register();
        }
        @SubscribeEvent
        public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders evt) {
            try {
                CrazyItemRegistrar.registerPartModels();
            } catch (Exception ignored) {}
        }
    }
}
