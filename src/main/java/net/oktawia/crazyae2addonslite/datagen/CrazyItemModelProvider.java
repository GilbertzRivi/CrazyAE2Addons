package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;

public class CrazyItemModelProvider extends ItemModelProvider {

    public CrazyItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CrazyAddonslite.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (var item : CrazyItemRegistrar.getItems()) {
            if (!CrazyItemRegistrar.getParts().contains(item)) {
                simpleItem(item);
            }
        }
    }

    private ItemModelBuilder simpleItem(Item item) {
        var key = BuiltInRegistries.ITEM.getKey(item);
        return withExistingParent(
                key.getPath(),
                ResourceLocation.withDefaultNamespace("item/generated")
        ).texture(
                "layer0",
                CrazyAddonslite.makeId("item/" + key.getPath())
        );
    }
}
