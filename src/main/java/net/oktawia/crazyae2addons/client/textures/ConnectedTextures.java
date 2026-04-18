package net.oktawia.crazyae2addons.client.textures;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class ConnectedTextures {
    private ConnectedTextures() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ConnectedTextures::onModifyBakingResult);
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        for (Map.Entry<Supplier<? extends Block>, ConnectedTextureEntry> entry : ConnectedTextureRegistry.all().entrySet()) {
            Block block = entry.getKey().get();
            ConnectedTextureEntry textureEntry = entry.getValue();

            Set<ModelResourceLocation> modelLocations = new HashSet<>();
            for (var state : block.getStateDefinition().getPossibleStates()) {
                modelLocations.add(BlockModelShaper.stateToModelLocation(state));
            }

            for (ModelResourceLocation location : modelLocations) {
                models.computeIfPresent(location, (id, originalModel) ->
                        new ConnectedTextureModel(originalModel, textureEntry)
                );
            }
        }
    }
}