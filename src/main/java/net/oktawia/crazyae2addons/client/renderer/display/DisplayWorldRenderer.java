package net.oktawia.crazyae2addons.client.renderer.display;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.parts.DisplayPart;

import java.util.HashSet;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class DisplayWorldRenderer {

    private static final float RENDER_DIST_SQ = 128f * 128f;

    private DisplayWorldRenderer() {}

    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        PoseStack ps = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Frustum frustum = event.getFrustum();

        boolean removedAny = DisplayPart.CLIENT_INSTANCES.removeIf(p -> {
            var be = p.getBlockEntity();
            return be == null || be.isRemoved() || be.getLevel() != mc.level;
        });

        if (removedAny) {
            DisplayGrid.invalidateClientCache();
        }

        Set<DisplayPart> visited = new HashSet<>();

        for (DisplayPart part : DisplayPart.CLIENT_INSTANCES) {
            if (visited.contains(part)) {
                continue;
            }

            DisplayGrid.RenderGroup group = DisplayGrid.getRenderGroup(part);
            Set<DisplayPart> grid = group.parts();

            if (grid.isEmpty()) {
                visited.add(part);
                continue;
            }

            DisplayPart renderOrigin = group.renderOrigin();
            boolean alreadyRendered = visited.contains(renderOrigin);

            visited.add(part);
            visited.addAll(grid);

            if (alreadyRendered) {
                continue;
            }

            AABB aabb = group.aabb();
            Vec3 center = aabb.getCenter();
            if (center.distanceToSqr(cam) > RENDER_DIST_SQ) {
                continue;
            }
            if (!frustum.isVisible(aabb)) {
                continue;
            }

            renderMatrix(renderOrigin, grid, ps, buf, cam);
        }
    }

    private static void renderMatrix(DisplayPart renderOrigin,
                                     Set<DisplayPart> grid,
                                     PoseStack ps,
                                     MultiBufferSource.BufferSource buf,
                                     Vec3 cam) {
        Font font = Minecraft.getInstance().font;
        DisplayRendererCommon.PreparedDisplay prepared = DisplayRendererCommon.prepare(font, renderOrigin, grid);

        if (prepared.commands().isEmpty()) {
            return;
        }

        BlockPos originPos = renderOrigin.getBlockEntity().getBlockPos();

        ps.pushPose();
        ps.translate(originPos.getX() - cam.x, originPos.getY() - cam.y, originPos.getZ() - cam.z);
        applyFacingTransform(ps, renderOrigin);
        ps.translate(0, 0, 0.52f);

        ps.scale(1f / 64f, -1f / 64f, 1f / 64f);

        DisplayRendererCommon.renderPrepared(prepared, ps, buf, font, 0xF000F0);

        ps.popPose();
        buf.endBatch();
    }

    private record Transformation(float tx, float ty, float tz, float yRot, float xRot) {}

    private static void applyFacingTransform(PoseStack ps, DisplayPart part) {
        Transformation t = getFacingTransformation(part.getSide());
        ps.translate(t.tx, t.ty, t.tz);
        ps.mulPose(Axis.YP.rotationDegrees(t.yRot));
        ps.mulPose(Axis.XP.rotationDegrees(t.xRot));
        if (t.xRot != 0f) {
            applySpinTransformation(ps, part, t.xRot);
        }
    }

    private static Transformation getFacingTransformation(Direction facing) {
        return switch (facing) {
            case SOUTH -> new Transformation(0f, 1f, 0.5f, 0f, 0f);
            case WEST  -> new Transformation(0.5f, 1f, 0f, -90f, 0f);
            case EAST  -> new Transformation(0.5f, 1f, 1f, 90f, 0f);
            case NORTH -> new Transformation(1f, 1f, 0.5f, 180f, 0f);
            case UP    -> new Transformation(0f, 0.5f, 0f, 0f, -90f);
            case DOWN  -> new Transformation(1f, 0.5f, 0f, 0f, 90f);
        };
    }

    private static void applySpinTransformation(PoseStack ps, DisplayPart part, float xRot) {
        float spin = 0f;

        if (xRot == 90f) {
            switch (part.spin) {
                case 0 -> {
                    spin = 0f;
                    ps.translate(-1f, 1f, 0f);
                }
                case 1 -> {
                    spin = 90f;
                    ps.translate(-1f, 0f, 0f);
                }
                case 2 -> {
                    spin = 180f;
                    ps.translate(0f, 0f, 0f);
                }
                case 3 -> {
                    spin = -90f;
                    ps.translate(0f, 1f, 0f);
                }
            }
        } else {
            switch (part.spin) {
                case 0 -> {
                    spin = 0f;
                    ps.translate(0f, 0f, 0f);
                }
                case 1 -> {
                    spin = -90f;
                    ps.translate(1f, 0f, 0f);
                }
                case 2 -> {
                    spin = 180f;
                    ps.translate(1f, -1f, 0f);
                }
                case 3 -> {
                    spin = 90f;
                    ps.translate(0f, -1f, 0f);
                }
            }
        }

        ps.mulPose(Axis.ZP.rotationDegrees(spin));
    }
}