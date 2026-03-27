package net.oktawia.crazyae2addons.recipes;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class CradlePattern {
    private final Map<String, List<Block>> symbolMap;
    private final List<String[][]> layers;
    private static final int SIZE = 5;
    private static final int OFFSET = SIZE / 2; // 2

    public CradlePattern(Map<String, List<Block>> symbolMap, List<String[][]> layers) {
        this.symbolMap = symbolMap;
        this.layers = layers;
    }

    public Map<String, List<Block>> symbolMap(){
        return symbolMap;
    }

    public List<String[][]> layers(){
        return layers;
    }

    public boolean matches(Level level, BlockPos center, Direction facing) {
        if (layers.size() != SIZE) return false;

        for (int y = 0; y < SIZE; y++) {
            String[][] layer = layers.get(y);
            if (layer.length != SIZE) return false;

            for (int z = 0; z < SIZE; z++) {
                String[] row = layer[z];
                if (row.length != SIZE) return false;

                for (int x = 0; x < SIZE; x++) {
                    String symbol = row[x];
                    if (symbol.equals(".")) continue;

                    BlockPos relative = rotateOffset(x - OFFSET, y - OFFSET, z - OFFSET, facing);
                    BlockPos checkPos = center.offset(relative);

                    BlockState state = level.getBlockState(checkPos);
                    Block block = state.getBlock();

                    List<Block> allowed = symbolMap.get(symbol);
                    if (allowed == null || !allowed.contains(block)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static BlockPos rotateOffset(int dx, int dy, int dz, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(dx, dy, dz);
            case SOUTH -> new BlockPos(-dx, dy, -dz);
            case WEST  -> new BlockPos(dz, dy, -dx);
            case EAST  -> new BlockPos(-dz, dy, dx);
            default    -> new BlockPos(dx, dy, dz);
        };
    }

    public static CradlePattern fromJson(JsonObject json) {
        Map<String, List<Block>> symbolMap = new HashMap<>();
        JsonObject symbolsJson = json.getAsJsonObject("symbols");
        for (Map.Entry<String, JsonElement> e : symbolsJson.entrySet()) {
            List<Block> blocks = new ArrayList<>();
            for (JsonElement el : e.getValue().getAsJsonArray()) {
                ResourceLocation id = new ResourceLocation(el.getAsString());
                Block b = ForgeRegistries.BLOCKS.getValue(id);
                if (b != null) blocks.add(b);
            }
            symbolMap.put(e.getKey(), blocks);
        }

        List<String[][]> layers = new ArrayList<>();
        JsonArray layersArray = json.getAsJsonArray("layers");
        for (JsonElement layerEl : layersArray) {
            JsonArray layerRows = layerEl.getAsJsonArray();
            String[][] grid = new String[SIZE][SIZE];
            for (int z = 0; z < SIZE; z++) {
                String[] row = layerRows.get(z).getAsString().split(" ");
                grid[z] = row;
            }
            layers.add(grid);
        }

        return new CradlePattern(symbolMap, layers);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonObject sym = new JsonObject();
        symbolMap.forEach((k, v) -> {
            JsonArray arr = new JsonArray();
            for (Block b : v) arr.add(ForgeRegistries.BLOCKS.getKey(b).toString());
            sym.add(k, arr);
        });
        root.add("symbols", sym);

        JsonArray layersArr = new JsonArray();
        for (String[][] layer : layers) {
            JsonArray rows = new JsonArray();
            for (int z = 0; z < SIZE; z++) {
                rows.add(String.join(" ", layer[z]));
            }
            layersArr.add(rows);
        }
        root.add("layers", layersArr);
        return root;
    }
}
