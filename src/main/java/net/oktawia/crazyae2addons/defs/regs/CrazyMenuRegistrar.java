package net.oktawia.crazyae2addons.defs.regs;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.entities.CraftingSchedulerBE;
import net.oktawia.crazyae2addons.entities.EjectorBE;
import net.oktawia.crazyae2addons.logic.builder.BuilderPatternHost;
import net.oktawia.crazyae2addons.menus.*;
import net.oktawia.crazyae2addons.menus.AmpereMeterMenu;
import net.oktawia.crazyae2addons.menus.AutoBuilderMenu;
import net.oktawia.crazyae2addons.menus.CraftingSchedulerMenu;
import net.oktawia.crazyae2addons.menus.DisplayMenu;
import net.oktawia.crazyae2addons.menus.DisplayTokenSubMenu;
import net.oktawia.crazyae2addons.parts.ChunkyFluidP2PTunnelPart;
import net.oktawia.crazyae2addons.parts.DisplayPart;
import net.oktawia.crazyae2addons.parts.EmitterTerminalPart;

public class CrazyMenuRegistrar {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CrazyAddons.MODID);

    private static <C extends AEBaseMenu, I> DeferredHolder<MenuType<?>, MenuType<C>> reg(
            String id, MenuTypeBuilder.MenuFactory<C, I> factory, Class<I> host) {
        return MENU_TYPES.register(id, () -> MenuTypeBuilder.create(factory, host).build(id));
    }

    public static final DeferredHolder<MenuType<?>, MenuType<DisplayMenu>> DISPLAY_MENU =
            reg("display", DisplayMenu::new, DisplayPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<DisplayTokenSubMenu>> DISPLAY_TOKEN_SUBMENU =
            reg("display_token_sub", DisplayTokenSubMenu::new, DisplayPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<DisplayImagesSubMenu>> DISPLAY_IMAGES_SUBMENU =
            reg("display_images_sub", DisplayImagesSubMenu::new, DisplayPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<AmpereMeterMenu>> AMPERE_METER_MENU =
            reg("ampere_meter", AmpereMeterMenu::new, AmpereMeterBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<EmitterTerminalMenu>> EMITTER_TERMINAL_MENU =
            reg("emitter_terminal", EmitterTerminalMenu::new, EmitterTerminalPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<EjectorMenu>> EJECTOR_MENU =
            reg("ejector", EjectorMenu::new, EjectorBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<ChunkyFluidP2PTunnelMenu>> CHUNKY_FLUID_P2P_TUNNEL_MENU =
            reg("chunky_p2p", ChunkyFluidP2PTunnelMenu::new, ChunkyFluidP2PTunnelPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingSchedulerMenu>> CRAFTING_SCHEDULER_MENU =
            reg("crafting_scheduler", CraftingSchedulerMenu::new, CraftingSchedulerBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<AutoBuilderMenu>> AUTO_BUILDER_MENU =
            reg("auto_builder", AutoBuilderMenu::new, AutoBuilderBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<BuilderPatternMenu>> BUILDER_PATTERN_MENU =
            reg("builder_pattern", BuilderPatternMenu::new, BuilderPatternHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<BuilderPatternSubMenu>> BUILDER_PATTERN_SUBMENU =
            reg("visual_assist", BuilderPatternSubMenu::new, BuilderPatternHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CrazyPatternProviderMenu>> CRAZY_PATTERN_PROVIDER_MENU =
            reg("crazy_pattern_provider", CrazyPatternProviderMenu::new, PatternProviderLogicHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<BrokenPatternProviderMenu>> BROKEN_PATTERN_PROVIDER_MENU =
            reg("broken_pattern_provider", BrokenPatternProviderMenu::new, PatternProviderLogicHost.class);

    private CrazyMenuRegistrar() {}
}
