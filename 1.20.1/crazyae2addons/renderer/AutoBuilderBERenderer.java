package net.oktawia.crazyae2addons.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class AutoBuilderBERenderer implements BlockEntityRenderer<AutoBuilderBE> {
    public AutoBuilderBERenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AutoBuilderBE be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (be.getLevel() == null || !be.getLevel().isClientSide) return;

        BlockPos ghostPos = be.getGhostRenderPos();
        if (ghostPos == null) return;

        Vec3 offset = Vec3.atLowerCornerWithOffset(ghostPos, 1, 1, 1)
                .subtract(Vec3.atCenterOf(be.getBlockPos()));

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        renderGhostOutlineBox(poseStack, buffer);
        poseStack.popPose();
    }

    private void renderGhostOutlineBox(PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer builder = buffer.getBuffer(RenderType.lines());
        Matrix4f mat = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();

        float r = 0.0f, g = 0.0f, b = 0.0f, alpha = 1.0f;

        Vec3[] corners = {
                new Vec3(-0.5, -0.5, -0.5),
                new Vec3( 0.5, -0.5, -0.5),
                new Vec3( 0.5, -0.5,  0.5),
                new Vec3(-0.5, -0.5,  0.5),
                new Vec3(-0.5,  0.5, -0.5),
                new Vec3( 0.5,  0.5, -0.5),
                new Vec3( 0.5,  0.5,  0.5),
                new Vec3(-0.5,  0.5,  0.5)
        };

        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };

        for (int[] edge : edges) {
            Vec3 pa = corners[edge[0]];
            Vec3 pb = corners[edge[1]];

            builder.vertex(mat, (float)pa.x, (float)pa.y, (float)pa.z)
                    .color(r, g, b, alpha).uv2(0x00F000F0).normal(normalMat, 0, 1, 0).endVertex();

            builder.vertex(mat, (float)pb.x, (float)pb.y, (float)pb.z)
                    .color(r, g, b, alpha).uv2(0x00F000F0).normal(normalMat, 0, 1, 0).endVertex();
        }
    }
}

