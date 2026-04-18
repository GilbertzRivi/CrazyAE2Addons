package net.oktawia.crazyae2addons.defs.regs;

import appeng.api.parts.PartModels;
import appeng.items.AEBaseItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.items.CrazyPatternProviderPartItem;

import java.util.List;

public class CrazyItemRegistrar {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CrazyAddons.MODID);

    public static List<Item> getItems() {
        return ITEMS.getEntries()
                .stream()
                .map(RegistryObject::get)
                .toList();
    }

    public static List<Item> getParts() {
        return ITEMS.getEntries()
                .stream()
                .map(RegistryObject::get)
                .filter(i -> i instanceof PartItem)
                .toList();
    }

    public static void registerPartModels() {
        for (Item item : getParts()) {
            if (item instanceof PartItem<?> partItem) {
                Class<?> partClass = partItem.getPartClass();
                if (partClass != null) {
                    PartModels.registerModels(PartModelsHelper.createModels(partClass.asSubclass(appeng.api.parts.IPart.class)));
                }
            }
        }
    }

    public static final RegistryObject<CrazyPatternProviderPartItem> CRAZY_PATTERN_PROVIDER_PART =
            ITEMS.register("crazy_pattern_provider_part",
                    () -> new CrazyPatternProviderPartItem(new Item.Properties()));

    public static final RegistryObject<Item> CRAZY_UPGRADE =
            ITEMS.register("crazy_upgrade",
                    () -> new Item(new Item.Properties()));

    private CrazyItemRegistrar() {}
}
