package net.oktawia.crazyae2addons.misc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.blocks.PenroseFrameBlock;
import net.oktawia.crazyae2addons.blocks.SpawnerExtractorWallBlock;
import net.oktawia.crazyae2addons.entities.SpawnerExtractorControllerBE;

import java.util.*;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT)
public class SpawnerExtractorPreviewRenderer {

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_SOLID_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        for (SpawnerExtractorControllerBE controller : SpawnerExtractorControllerBE.CLIENT_INSTANCES) {
            if (!controller.preview) continue;

            BlockPos origin = controller.getBlockPos();
            if (origin.distSqr(mc.player.blockPosition()) > 64 * 64) continue;

            Direction facing = controller.getBlockState()
                    .getValue(BlockStateProperties.HORIZONTAL_FACING)
                    .getOpposite();

            if (controller.ghostCache == null) {
                rebuildCache(controller, facing);
            }

            PreviewRenderer.render(controller.ghostCache, event);
        }
    }

    private static void rebuildCache(SpawnerExtractorControllerBE controller, Direction facing) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();

        var validator = controller.validator;
        List<List<String>> layers = validator.getLayers();
        Map<String, List<Block>> symbols = validator.getSymbols();
        int originX = validator.getOriginX();
        int originY = validator.getOriginY();
        int originZ = validator.getOriginZ();

        BlockPos origin = controller.getBlockPos();
        List<CachedBlockInfo> cache = new ArrayList<>();

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
                    if (state.getBlock() instanceof SpawnerExtractorWallBlock){
                        state = state.setValue(SpawnerExtractorWallBlock.FORMED, true);
                    }
                    int relX = x - originX;
                    int relY = y - originY;
                    int relZ = z - originZ;
                    BlockPos offset = rotateOffset(relX, relZ, facing);
                    BlockPos pos = origin.offset(offset.getX(), relY, offset.getZ());

                    BakedModel model = blockRenderer.getBlockModel(state);
                    cache.add(new CachedBlockInfo(pos, state, model));
                }
            }
        }

        controller.ghostCache = cache;
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
}
