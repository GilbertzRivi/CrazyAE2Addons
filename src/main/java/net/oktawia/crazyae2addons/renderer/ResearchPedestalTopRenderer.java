package net.oktawia.crazyae2addons.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.entities.ResearchPedestalTopBE;
import com.mojang.math.Axis;

public class ResearchPedestalTopRenderer implements BlockEntityRenderer<ResearchPedestalTopBE> {

    public ResearchPedestalTopRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ResearchPedestalTopBE be, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {

        ItemStack stack = be.getStoredStack();
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Bezpiecznie ogarnij time, jakby level był nullem
        long gameTime = 0L;
        if (be.getLevel() != null) {
            gameTime = be.getLevel().getGameTime();
        }

        float time = (gameTime + partialTick);

        float bobAmplitude = 0.1f;
        float bobSpeed = 0.1f; // im mniejsze, tym wolniej
        float bobOffset = (float) Math.sin(time * bobSpeed) * bobAmplitude;

        double baseY = 0.5f;

        poseStack.translate(0.5, baseY + bobOffset, 0.5);

        float baseRotationSpeed = 1.0f;

        float angle = time * baseRotationSpeed;
        float angleX = angle;
        float angleY = angle * 1.3f;
        float angleZ = angle * 0.7f;

        poseStack.mulPose(Axis.XP.rotationDegrees(angleX));
        poseStack.mulPose(Axis.YP.rotationDegrees(angleY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(angleZ));

        float scale = 0.5f;
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                be.getLevel(),
                0
        );

        poseStack.popPose();
    }
}
