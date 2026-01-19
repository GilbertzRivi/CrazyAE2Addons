package net.oktawia.crazyae2addonslite;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.oktawia.crazyae2addonslite.defs.UpgradeCards;
import net.oktawia.crazyae2addonslite.defs.regs.*;
import net.oktawia.crazyae2addonslite.network.NetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(CrazyAddonslite.MODID)
public class CrazyAddonslite {
    public static final String MODID = "crazyae2addonslite";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CrazyAddonslite(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Loading Crazy AE2 Addons (lite)");

        modContainer.registerConfig(ModConfig.Type.COMMON, CrazyConfig.COMMON_SPEC);
        modEventBus.addListener(this::commonSetup);

        CrazyItemRegistrar.ITEMS.register(modEventBus);
        CrazyBlockRegistrar.BLOCKS.register(modEventBus);
        CrazyBlockRegistrar.BLOCK_ITEMS.register(modEventBus);
        CrazyBlockEntityRegistrar.BLOCK_ENTITIES.register(modEventBus);
        CrazyMenuRegistrar.MENU_TYPES.register(modEventBus);
        CrazyDataComponents.COMPONENTS.register(modEventBus);

        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::registerCreativeTab);
        modEventBus.addListener(NetworkHandler::registerMessages);
    }

    public static @NotNull ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onRegister(RegisterEvent event) {
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

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            new UpgradeCards(event);
            CrazyBlockEntityRegistrar.setupBlockEntityTypes();
        });
    }
}
