package net.oktawia.crazyae2addons.mixins;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.logic.interfaces.RenderTypeTextureAccess;
import net.oktawia.crazyae2addons.mixins.accessors.CompositeStateAccessor;
import net.oktawia.crazyae2addons.mixins.accessors.TextureStateShardAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public abstract class MixinCompositeRenderType implements RenderTypeTextureAccess {

    @Final
    @Shadow
    private RenderType.CompositeState state;

    @Override
    public ResourceLocation crazyae2addons$getTextureLocation() {
        RenderStateShard.EmptyTextureStateShard shard =
                ((CompositeStateAccessor) (Object) state).crazyae2addons$getTextureState();

        if (shard instanceof RenderStateShard.TextureStateShard) {
            return ((TextureStateShardAccessor) (Object) shard)
                    .crazyae2addons$getTexture()
                    .orElse(null);
        }

        return null;
    }
}