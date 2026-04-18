package net.oktawia.crazyae2addons.client.textures;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.blocks.penrose.PenroseFrameBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class ConnectedTextures {
    private ConnectedTextures() {}

    public static void init(IEventBus modEventBus) {
        ConnectedTextureRegistry.register(
                CrazyBlockRegistrar.PENROSE_FRAME,
                new ConnectedTextureEntry(
                        state -> state.getValue(PenroseFrameBlock.FORMED)
                                ? CrazyAddons.makeId("block/penrose_frame_formed")
                                : CrazyAddons.makeId("block/penrose_frame_not_formed"),
                        ConnectedTextureRules.SAME_BLOCK
                )
        );

        ConnectedTextureRegistry.register(
                CrazyBlockRegistrar.PENROSE_COIL,
                ConnectedTextureEntry.single(
                        CrazyAddons.makeId("block/penrose_coil_formed"),
                        ConnectedTextureRules.SAME_BLOCK
                )
        );

        modEventBus.addListener(ConnectedTextures::onModifyBakingResult);
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();

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