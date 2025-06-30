package net.oktawia.crazyae2addons.misc;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.blocks.MobFarmWallBlock;
import net.oktawia.crazyae2addons.blocks.PenroseFrameBlock;
import net.oktawia.crazyae2addons.entities.PenroseControllerBE;
import net.oktawia.crazyae2addons.interfaces.PenroseValidator;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT)
public class PenrosePreviewRenderer {

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_SOLID_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        Frustum frustum = event.getFrustum();

        for (PenroseControllerBE controller : PenroseControllerBE.CLIENT_INSTANCES) {
            if (!controller.preview) continue;

            BlockPos origin = controller.getBlockPos();
            if (origin.distSqr(mc.player.blockPosition()) > 64 * 64) continue;

            PenroseValidator validator = switch (controller.previewTier) {
                case 3 -> controller.validatorT3;
                case 2 -> controller.validatorT2;
                case 1 -> controller.validatorT1;
                default -> controller.validatorT0;
            };

            Direction facing = controller.getBlockState()
                    .getValue(BlockStateProperties.HORIZONTAL_FACING)
                    .getOpposite();

            if (controller.ghostCache == null || controller.cachedTier != controller.previewTier) {
                rebuildCache(controller, validator, facing);
            }

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            for (CachedBlockInfo info : controller.ghostCache) {

                BlockPos pos = info.pos();
                if (!mc.level.isLoaded(pos)) continue;
                if (pos.distSqr(mc.player.blockPosition()) > 64 * 64) continue;

                if (!frustum.isVisible(new AABB(pos))) continue;

                BlockState current = mc.level.getBlockState(pos);
                if (current.getBlock() == info.state().getBlock()) continue;

                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                var state = info.state();
                BakedModel model = blockRenderer.getBlockModel(state);
                RenderType layer = model.getRenderTypes(state, mc.level.random, ModelData.EMPTY).asList().get(0);

                blockRenderer.getModelRenderer().renderModel(
                        poseStack.last(),
                        buffer.getBuffer(layer),
                        state,
                        model,
                        1f, 1f, 1f,
                        0xF0F0F0,
                        OverlayTexture.NO_OVERLAY
                );

                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }


    private static BlockPos rotateOffset(int x, int z, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(x, 0, z);
            case SOUTH -> new BlockPos(-x, 0, -z);
            case WEST -> new BlockPos(z, 0, -x);
            case EAST -> new BlockPos(-z, 0, x);
            default -> BlockPos.ZERO;
        };
    }

    private static void rebuildCache(PenroseControllerBE controller, PenroseValidator validator, Direction facing) {
        controller.ghostCache = new java.util.ArrayList<>();
        controller.cachedTier = controller.previewTier;

        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();

        List<List<String>> layers = validator.getLayers();
        Map<String, List<Block>> symbols = validator.getSymbols();
        int originX = validator.getOriginX();
        int originY = validator.getOriginY();
        int originZ = validator.getOriginZ();

        BlockPos origin = controller.getBlockPos();

        int height = layers.size();
        int sizeZ = layers.get(0).size();
        int sizeX = layers.get(0).get(0).split(" ").length;

        for (int y = 0; y < height; y++) {
            List<String> layer = layers.get(y);
            for (int z = 0; z < sizeZ; z++) {
                String[] row = layer.get(z).split(" ");
                for (int x = 0; x < sizeX; x++) {
                    String symbol = row[x];
                    if (symbol.equals(".") || !symbols.containsKey(symbol)) continue;

                    List<Block> blocks = symbols.get(symbol);
                    if (blocks.isEmpty()) continue;

                    BlockState state = blocks.get(0).defaultBlockState();
                    if (state.getBlock() instanceof PenroseFrameBlock){
                        state = state.setValue(PenroseFrameBlock.FORMED, true);
                    }
                    int relX = x - originX;
                    int relY = y - originY;
                    int relZ = z - originZ;
                    BlockPos offset = rotateOffset(relX, relZ, facing);
                    BlockPos pos = origin.offset(offset.getX(), relY, offset.getZ());

                    BakedModel model = blockRenderer.getBlockModel(state);
                    controller.ghostCache.add(new CachedBlockInfo(pos, state, model));
                }
            }
        }
    }

    public record CachedBlockInfo(BlockPos pos, BlockState state, BakedModel model) {}
}
