package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addonslite.CrazyAddons;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CrazyDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new CrazyRecipeProvider(packOutput));
        generator.addProvider(event.includeServer(), CrazyLootTableProvider.create(packOutput));

        generator.addProvider(event.includeClient(), new CrazyBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new CrazyItemModelProvider(packOutput, existingFileHelper));

        generator.addProvider(event.includeClient(), new CrazyLangProvider(packOutput, "en_us"));

        CrazyBlockTagGenerator blockTagGenerator = generator.addProvider(event.includeServer(),
                new CrazyBlockTagGenerator(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(), new CrazyItemTagGeneratorr(packOutput, lookupProvider, blockTagGenerator.contentsGetter(), existingFileHelper));
    }
}
