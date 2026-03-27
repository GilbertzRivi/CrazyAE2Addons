package net.oktawia.crazyae2addons.items;

import appeng.items.AEBaseItem;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.network.ClipboardPacket;
import net.oktawia.crazyae2addons.network.NetworkHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StructurePatternCopyItem extends AEBaseItem {
    private BlockPos pos1 = null;
    private BlockPos pos2 = null;

    public StructurePatternCopyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player != null) {
            if (!context.isSecondaryUseActive()) {
                pos1 = pos;
                player.displayClientMessage(Component.literal("Corner 1 set!"), true);
            } else {
                pos2 = pos;
                player.displayClientMessage(Component.literal("Corner 2 set!"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && pos1 != null && pos2 != null) {
            BlockPos min = new BlockPos(
                    Math.min(pos1.getX(), pos2.getX()),
                    Math.min(pos1.getY(), pos2.getY()),
                    Math.min(pos1.getZ(), pos2.getZ()));
            BlockPos max = new BlockPos(
                    Math.max(pos1.getX(), pos2.getX()),
                    Math.max(pos1.getY(), pos2.getY()),
                    Math.max(pos1.getZ(), pos2.getZ()));

            Map<String, String> symbolMap = new LinkedHashMap<>();
            List<List<String>> layers = new ArrayList<>();
            char nextSymbol = 'A';

            for (int y = min.getY(); y <= max.getY(); y++) {
                List<String> layer = new ArrayList<>();
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    StringBuilder row = new StringBuilder();
                    for (int x = min.getX(); x <= max.getX(); x++) {
                        BlockPos current = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(current);
                        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                        String key = id.toString();

                        String symbol;
                        if (key.equals("minecraft:air")) {
                            symbol = ".";
                        } else {
                            if (!symbolMap.containsKey(key)) {
                                if (nextSymbol > 'Z') {
                                    player.displayClientMessage(Component.literal("Too many unique blocks in structure! Max 26."), true);
                                    return InteractionResultHolder.fail(player.getItemInHand(hand));
                                }
                                if (nextSymbol == 'C') nextSymbol++;
                                symbolMap.put(key, String.valueOf(nextSymbol++));
                            }
                            symbol = symbolMap.get(key);
                        }
                        row.append(symbol).append(" ");
                    }
                    layer.add(row.toString().trim());
                }
                layers.add(layer);
            }

            JsonObject root = new JsonObject();
            JsonObject symbolsJson = new JsonObject();
            for (Map.Entry<String, String> entry : symbolMap.entrySet()) {
                JsonArray arr = new JsonArray();
                arr.add(entry.getKey());
                symbolsJson.add(entry.getValue(), arr);
            }

            root.add("symbols", symbolsJson);

            JsonArray layersJson = new JsonArray();
            for (List<String> layer : layers) {
                JsonArray layerArray = new JsonArray();
                for (String row : layer) {
                    layerArray.add(row);
                }
                layersJson.add(layerArray);
            }
            root.add("layers", layersJson);

            String jsonStr = new GsonBuilder().setPrettyPrinting().create().toJson(root);

            if (!level.isClientSide()) {
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ClipboardPacket(jsonStr));
                player.displayClientMessage(Component.literal("Structure pattern copied to clipboard."), true);
            }

            pos1 = null;
            pos2 = null;
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}