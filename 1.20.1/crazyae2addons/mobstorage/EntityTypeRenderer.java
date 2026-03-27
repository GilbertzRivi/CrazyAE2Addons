package net.oktawia.crazyae2addons.mobstorage;

import appeng.api.client.AEKeyRenderHandler;
import appeng.api.client.AEKeyRendering;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashMap;
import java.util.Map;

public class EntityTypeRenderer implements AEKeyRenderHandler<MobKey> {

    private static final Map<EntityType<?>, Entity> ENTITY_CACHE = new HashMap<>();

    public static void initialize() {
        AEKeyRendering.register(
                MobKeyType.TYPE,
                MobKey.class,
                new EntityTypeRenderer()
        );
    }

    @Override
    public void drawInGui(Minecraft mc, GuiGraphics gui, int x, int y, MobKey key) {
        if (mc.level == null) return;

        Entity ent = ENTITY_CACHE.computeIfAbsent(
                key.getEntityType(),
                type -> type.create(mc.level)
        );
        if (ent == null) return;

        ent.tickCount = (int) mc.level.getGameTime();
        ent.tick();

        long   t    = mc.level.getGameTime();
        float  pt   = mc.getFrameTime();
        float  angle= (t + pt);

        ent.setYRot(angle);
        ent.setYBodyRot(angle);
        ent.setYHeadRot(angle);
        ent.setXRot(0f);

        float w       = ent.getBbWidth();
        float h       = ent.getBbHeight();
        float margin  = 1f, slot = 16f;
        float avail   = slot - margin * 2f;
        float scaleX  = avail / w, scaleY = avail / h;
        float scale   = Math.min(scaleX, scaleY) * 0.8f;

        PoseStack pose = gui.pose();
        pose.pushPose();
        pose.translate(x + 8.0, y + 14.0, 100.0);
        pose.mulPose(Axis.XP.rotationDegrees(-22.5f));
        pose.scale(scale, -scale, scale);

        int light = LightTexture.pack(12, 12);
        var buffers = mc.renderBuffers().bufferSource();
        mc.getEntityRenderDispatcher().render(
                ent,
                0, 0, 0,
                angle,
                pt,
                pose,
                buffers,
                light
        );
        buffers.endBatch();
        pose.popPose();
    }

    @Override
    public void drawOnBlockFace(PoseStack poseStack, MultiBufferSource buffers, MobKey key, float scale, int combinedLight, Level level) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity ent = ENTITY_CACHE.computeIfAbsent(
                key.getEntityType(),
                t -> t.create(mc.level)
        );
        if (ent == null) return;
        long  t    = mc.level.getGameTime();
        float pt   = mc.getFrameTime();
        float yaw  = (t + pt) % 360f;
        ent.tickCount = (int) t;
        ent.tick();

        ent.setYRot(yaw);
        ent.setYBodyRot(yaw);
        ent.setYHeadRot(yaw);

        float w       = ent.getBbWidth();
        float h       = ent.getBbHeight();
        float scaleX  = 16f / w;
        float scaleY  = 16f / h;
        float myScale   = Math.min(scaleX, scaleY) * 0.2f;

        poseStack.pushPose();
        poseStack.translate(0, -0.15, 0.01);
        poseStack.scale(myScale * 0.1f, myScale * 0.1f, 0.0001f);

        mc.getEntityRenderDispatcher().render(
                ent,
                0, 0, 0,
                yaw,
                pt,
                poseStack,
                buffers,
                combinedLight
        );
        poseStack.popPose();
    }


    @Override
    public Component getDisplayName(MobKey stack) {
        return stack.getEntityType().getDescription();
    }
}
