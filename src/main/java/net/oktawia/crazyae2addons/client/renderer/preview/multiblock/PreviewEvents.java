package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT)
public final class PreviewEvents {
    private PreviewEvents() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        for (MultiblockPreviewHost host : PreviewRegistry.snapshot()) {
            if (!host.isPreviewEnabled()) continue;
            if (!(host instanceof BlockEntity be)) continue;
            if (be.isRemoved()) continue;
            if (be.getLevel() != mc.level) continue;
            if (be.getBlockPos().distSqr(mc.player.blockPosition()) > 64 * 64) continue;

            MultiblockPreviewInfo previewInfo = host.getPreviewInfo();
            if (previewInfo == null || previewInfo.isStale(host)) {
                previewInfo = PreviewCacheBuilder.rebuild(host);
                host.setPreviewInfo(previewInfo);
            }

            PreviewRenderer.render(previewInfo, event);
        }
    }
}