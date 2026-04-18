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
import net.oktawia.crazyae2addons.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addons.menus.block.*;

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

    private CrazyMenuRegistrar() {}
}