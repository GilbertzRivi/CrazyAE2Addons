package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;

public class CrazyItemModelProvider extends ItemModelProvider {
    public CrazyItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CrazyAddons.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (var item : CrazyItemRegistrar.getItems()){
            if (!CrazyItemRegistrar.getParts().contains(item)){
                simpleItem(item);
            }
        }
    }

    private ItemModelBuilder simpleItem(Item item){
        return withExistingParent(ForgeRegistries.ITEMS.getKey(item).getPath(),
            new ResourceLocation("item/generated")).texture("layer0",
            CrazyAddons.makeId("item/" + ForgeRegistries.ITEMS.getKey(item).getPath()));
    }
}
