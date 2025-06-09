package net.oktawia.crazyae2addons;

import appeng.api.features.GridLinkables;
import appeng.api.stacks.AEKeyTypes;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.mojang.logging.LogUtils;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
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
import net.oktawia.crazyae2addons.defs.*;
import net.oktawia.crazyae2addons.defs.regs.*;
import net.oktawia.crazyae2addons.logic.WirelessRedstoneTerminalItemLogicHost;
import net.oktawia.crazyae2addons.menus.WirelessRedstoneTerminalMenu;
import net.oktawia.crazyae2addons.mobstorage.EntityTypeRenderer;
import net.oktawia.crazyae2addons.mobstorage.MobKeyType;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Mod(CrazyAddons.MODID)
public class CrazyAddons {
    public static final String MODID = "crazyae2addons";

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
                IUniversalWirelessTerminalItem term = CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get();
                Objects.requireNonNull(term);
                LogUtils.getLogger().info(term.toString());
                WUTHandler.addTerminal("wireless_redstone_terminal",
                        term::tryOpen,
                        WirelessRedstoneTerminalItemLogicHost::new,
                        WirelessRedstoneTerminalMenu.TYPE,
                        term,
                        "wireless_redstone_terminal",
                        "item.crazyae2addons.wireless_redstone_terminal"
                );}
            }
        );

        modEventBus.addListener(this::registerCreativeTab);

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
            NetworkHandler.registerClientPackets();
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
        }
        @SubscribeEvent
        public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders evt) {
            try {
                CrazyItemRegistrar.registerPartModels();
            } catch (Exception ignored) {}
        }
    }
}