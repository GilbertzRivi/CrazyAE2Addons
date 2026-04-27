package net.oktawia.crazyae2addons.client.renderer.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class CachedPreviewBuffer implements VertexConsumer {

    private final List<CachedVertex> vertices = new ArrayList<>();

    private double x;
    private double y;
    private double z;

    private int r = 255;
    private int g = 255;
    private int b = 255;
    private int a = 255;

    private float u;
    private float v;

    private int overlay = OverlayTexture.NO_OVERLAY;
    private int light = LightTexture.FULL_BRIGHT;

    private float nx;
    private float ny = 1.0f;
    private float nz;

    private boolean colorSet;

    private boolean defaultColorSet;
    private int defaultR = 255;
    private int defaultG = 255;
    private int defaultB = 255;
    private int defaultA = 255;

    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    public void clear() {
        vertices.clear();
    }

    public int vertexCount() {
        return vertices.size();
    }

    public void emitBlock(PoseStack.Pose pose, VertexConsumer out) {
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        for (CachedVertex vertex : vertices) {
            out.vertex(poseMatrix, vertex.x, vertex.y, vertex.z)
                    .color(vertex.r, vertex.g, vertex.b, vertex.a)
                    .uv(vertex.u, vertex.v)
                    .uv2(unpackU(vertex.light), unpackV(vertex.light))
                    .normal(normalMatrix, vertex.nx, vertex.ny, vertex.nz)
                    .endVertex();
        }
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.colorSet = true;
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        this.overlay = pack(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        this.light = pack(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.nx = x;
        this.ny = y;
        this.nz = z;
        return this;
    }

    @Override
    public void endVertex() {
        int finalR = colorSet ? r : defaultColorSet ? defaultR : 255;
        int finalG = colorSet ? g : defaultColorSet ? defaultG : 255;
        int finalB = colorSet ? b : defaultColorSet ? defaultB : 255;
        int finalA = colorSet ? a : defaultColorSet ? defaultA : 255;

        vertices.add(new CachedVertex(
                (float) x,
                (float) y,
                (float) z,
                finalR,
                finalG,
                finalB,
                finalA,
                u,
                v,
                overlay,
                light,
                nx,
                ny,
                nz
        ));

        colorSet = false;

        r = 255;
        g = 255;
        b = 255;
        a = 255;

        u = 0.0f;
        v = 0.0f;

        overlay = OverlayTexture.NO_OVERLAY;
        light = LightTexture.FULL_BRIGHT;

        nx = 0.0f;
        ny = 1.0f;
        nz = 0.0f;
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        this.defaultColorSet = true;
        this.defaultR = r;
        this.defaultG = g;
        this.defaultB = b;
        this.defaultA = a;
    }

    @Override
    public void unsetDefaultColor() {
        this.defaultColorSet = false;
    }

    private static int pack(int u, int v) {
        return (u & 0xFFFF) | ((v & 0xFFFF) << 16);
    }

    private static int unpackU(int packed) {
        return packed & 0xFFFF;
    }

    private static int unpackV(int packed) {
        return (packed >>> 16) & 0xFFFF;
    }

    private record CachedVertex(
            float x,
            float y,
            float z,
            int r,
            int g,
            int b,
            int a,
            float u,
            float v,
            int overlay,
            int light,
            float nx,
            float ny,
            float nz
    ) {
    }
}