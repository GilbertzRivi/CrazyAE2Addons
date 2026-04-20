package net.oktawia.crazyae2addons.defs.regs;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.GregTech.GTAmpereMeterBE;
import net.oktawia.crazyae2addons.entities.*;
import net.oktawia.crazyae2addons.logic.autobuilder.BuilderPatternHost;
import net.oktawia.crazyae2addons.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addons.menus.block.*;
import net.oktawia.crazyae2addons.menus.item.BuilderPatternMenu;
import net.oktawia.crazyae2addons.menus.item.BuilderPatternSubMenu;

public class CrazyMenuRegistrar {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CrazyAddons.MODID);

    private static <C extends AEBaseMenu, I> RegistryObject<MenuType<C>> reg(
            String id, MenuTypeBuilder.MenuFactory<C, I> factory, Class<I> host) {

        return MENU_TYPES.register(id,
                () -> MenuTypeBuilder.create(factory, host).build(id));
    }

    public static final RegistryObject<MenuType<CrazyPatternProviderMenu>> CRAZY_PATTERN_PROVIDER_MENU =
            reg("crazy_pattern_provider", CrazyPatternProviderMenu::new, PatternProviderLogicHost.class);

    public static final RegistryObject<MenuType<AmpereMeterMenu>> AMPERE_METER_MENU =
            IsModLoaded.GTCEU
                    ? reg("ampere_meter", AmpereMeterMenu::new, GTAmpereMeterBE.class)
                    : reg("ampere_meter", AmpereMeterMenu::new, AmpereMeterBE.class);

    public static final RegistryObject<MenuType<AutoBuilderMenu>> AUTO_BUILDER_MENU =
            reg("auto_builder_menu", AutoBuilderMenu::new, AutoBuilderBE.class);

    public static final RegistryObject<MenuType<BuilderPatternMenu>> BUILDER_PATTERN_MENU =
            reg("builder_pattern_menu", BuilderPatternMenu::new, BuilderPatternHost.class);

    public static final RegistryObject<MenuType<BuilderPatternSubMenu>> BUILDER_PATTERN_SUBMENU =
            reg("builder_pattern_submenu", BuilderPatternSubMenu::new, BuilderPatternHost.class);

    public static final RegistryObject<MenuType<BrokenPatternProviderMenu>> BROKEN_PATTERN_PROVIDER_MENU =
            reg("broken_pattern_provider_menu", BrokenPatternProviderMenu::new, BrokenPatternProviderBE.class);

    public static final RegistryObject<MenuType<EjectorMenu>> EJECTOR_MENU =
            reg("ejector_menu", EjectorMenu::new, EjectorBE.class);

    public static final RegistryObject<MenuType<CraftingSchedulerMenu>> CRAFTING_SCHEDULER_MENU =
            reg("crafting_scheduler_menu", CraftingSchedulerMenu::new, CraftingSchedulerBE.class);

    private CrazyMenuRegistrar() {}
}