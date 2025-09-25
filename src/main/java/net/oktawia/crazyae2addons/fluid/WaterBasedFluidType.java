package net.oktawia.crazyae2addons.fluid;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;
import net.oktawia.crazyae2addons.CrazyAddons;

public class WaterBasedFluidType extends FluidType {
    private static final ResourceLocation UNDERWATER_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    private static final ResourceLocation WATER_STILL =
            ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "block/water_still");
    private static final ResourceLocation WATER_FLOW =
            ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "block/water_flowing");
    private static final ResourceLocation WATER_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "block/water_overlay");

    protected int tintColor = 0xFF47C7FF;

    public WaterBasedFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override public ResourceLocation getStillTexture() { return WATER_STILL; }
            @Override public ResourceLocation getFlowingTexture() { return WATER_FLOW; }
            @Override public ResourceLocation getOverlayTexture() { return WATER_OVERLAY; }
            @Override public ResourceLocation getRenderOverlayTexture(Minecraft mc) { return UNDERWATER_LOCATION; }
            @Override public int getTintColor() { return tintColor; }
            @Override public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) { return tintColor; }
        });
    }
}
