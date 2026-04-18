package net.oktawia.crazyae2addons.client.textures;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;

public record ConnectedTextureEntry(
        Function<BlockState, ResourceLocation> textureSelector,
        ConnectedTextureRule rule
) {
    public ResourceLocation texture(BlockState state) {
        return textureSelector.apply(state);
    }

    public static ConnectedTextureEntry single(ResourceLocation texture, ConnectedTextureRule rule) {
        return new ConnectedTextureEntry(state -> texture, rule);
    }
}