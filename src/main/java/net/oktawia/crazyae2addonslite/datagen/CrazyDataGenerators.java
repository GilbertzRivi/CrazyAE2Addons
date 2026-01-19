package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = CrazyAddonslite.MODID)
public class CrazyDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new CrazyRecipeProvider(packOutput, lookupProvider));
        generator.addProvider(event.includeServer(), CrazyLootTableProvider.create(packOutput, lookupProvider));

        generator.addProvider(event.includeClient(), new CrazyBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new CrazyItemModelProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new CrazyLangProvider(packOutput, "en_us"));

        CrazyBlockTagGenerator blockTagGenerator = generator.addProvider(
                event.includeServer(),
                new CrazyBlockTagGenerator(packOutput, lookupProvider, existingFileHelper)
        );

        generator.addProvider(
                event.includeServer(),
                new CrazyItemTagGenerator(packOutput, lookupProvider, blockTagGenerator.contentsGetter(), existingFileHelper)
        );
    }
}
