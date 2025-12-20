package net.oktawia.crazyae2addons.renderer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.items.PortableAutobuilder;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.renderer.BuilderPreviewRenderer;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT)
public class HeldStructureGadgetPreviewRenderer {

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof PortableSpatialStorage) && !(held.getItem() instanceof PortableAutobuilder)) return;

        var tag = held.getTag();
        if (tag == null || !tag.getBoolean("code")) return;

        // Własny raytrace do 50 bloków – zgodnie z tym jak będziemy wklejać na serwerze
        BlockHitResult bhr = rayTrace(mc, 50.0D, event.getPartialTick());
        if (bhr == null || bhr.getType() != HitResult.Type.BLOCK) return;

        BlockPos originWorld = bhr.getBlockPos().relative(bhr.getDirection());

        if (!tag.contains("preview_palette", Tag.TAG_LIST)) return;
        if (!tag.contains("preview_positions", Tag.TAG_INT_ARRAY)) return;
        if (!tag.contains("preview_indices", Tag.TAG_INT_ARRAY)) return;

        ListTag palList = tag.getList("preview_palette", Tag.TAG_STRING);
        int[] posArr = tag.getIntArray("preview_positions");
        int[] idxArr = tag.getIntArray("preview_indices");
        if (posArr.length % 3 != 0) return;

        // Stały kierunek struktury – NIE zależy od aktualnego obrotu gracza
        Direction structureFacing = PortableSpatialStorage.readSrcFacingFromNbt(held);
        if (!structureFacing.getAxis().isHorizontal()) {
            structureFacing = Direction.NORTH;
        }

        List<PreviewInfo.BlockInfo> blocks = new ArrayList<>();
        Basis basis = Basis.forFacing(structureFacing);

        int n = posArr.length / 3;
        for (int i = 0; i < n && i < idxArr.length; i++) {
            int px = posArr[i * 3];
            int py = posArr[i * 3 + 1];
            int pz = posArr[i * 3 + 2];

            BlockPos world = localToWorld(new BlockPos(px, py, pz), originWorld, basis);

            int palIndex = idxArr[i];
            if (palIndex < 0 || palIndex >= palList.size()) continue;
            String key = palList.getString(palIndex);

            BlockState state = AutoBuilderPreviewStateCache.parseBlockState(key);
            if (state == null) continue;

            // brak rotacji stanu względem gracza – orientacja tylko z kodu / menu
            blocks.add(new PreviewInfo.BlockInfo(world, state));
        }

        if (!blocks.isEmpty()) {
            BuilderPreviewRenderer.render(new PreviewInfo(new ArrayList<>(blocks)), event);
        }
    }

    private static BlockHitResult rayTrace(Minecraft mc, double maxDistance, float partialTick) {
        if (mc.level == null || mc.player == null) return null;

        Vec3 eye = mc.player.getEyePosition(partialTick);
        Vec3 look = mc.player.getViewVector(partialTick);
        Vec3 end = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

        ClipContext ctx = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player);
        HitResult res = mc.level.clip(ctx);
        if (res instanceof BlockHitResult bhr && res.getType() == HitResult.Type.BLOCK) {
            return bhr;
        }
        return null;
    }

    private static class Basis {
        final int fx, fz;
        final int rx, rz;
        private Basis(int fx, int fz, int rx, int rz) { this.fx = fx; this.fz = fz; this.rx = rx; this.rz = rz; }
        static Basis forFacing(Direction f) {
            return switch (f) {
                case SOUTH -> new Basis( 0,  1, -1,  0);
                case EAST  -> new Basis( 1,  0,  0,  1);
                case WEST  -> new Basis(-1,  0,  0, -1);
                default    -> new Basis( 0, -1,  1,  0); // NORTH
            };
        }
    }

    private static BlockPos localToWorld(BlockPos local, BlockPos origin, Basis b) {
        int dx = local.getX() * b.rx + local.getZ() * b.fx;
        int dz = local.getX() * b.rz + local.getZ() * b.fz;
        int dy = local.getY();
        return origin.offset(dx, dy, dz);
    }
}
