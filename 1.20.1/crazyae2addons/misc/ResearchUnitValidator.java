package net.oktawia.crazyae2addons.misc;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.blocks.ResearchUnitFrameBlock;
import net.oktawia.crazyae2addons.entities.ResearchUnitBE;
import net.oktawia.crazyae2addons.entities.ResearchUnitFrameBE;
import net.oktawia.crazyae2addons.interfaces.PenroseValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResearchUnitValidator implements PenroseValidator {

    private static final String STRUCTURE_JSON = """
            {
              "symbols": {
                "A": [
                  "crazyae2addons:research_unit_frame"
                ],
                "B": [
                  "ae2:quartz_vibrant_glass"
                ],
                "C": [
                  "crazyae2addons:research_unit"
                ],
                "E": [
                  "ae2:sky_stone_tank"
                ],
                "Q": [
                  "ae2:1k_crafting_storage",
                  "ae2:4k_crafting_storage",
                  "ae2:16k_crafting_storage",
                  "ae2:64k_crafting_storage",
                  "ae2:256k_crafting_storage",
                  "minecraft:air"
                ]
              },
              "layers": [
                [
                  "A A A A A",
                  "A B B B A",
                  "A B B B A",
                  "A B B B A",
                  "A A C A A"
                ],
                [
                  "A B B B A",
                  "B Q Q Q B",
                  "B Q Q Q B",
                  "B Q Q Q B",
                  "A B B B A"
                ],
                [
                  "A B B B A",
                  "B Q Q Q B",
                  "B Q Q Q B",
                  "B Q Q Q B",
                  "A B B B A"
                ],
                [
                  "A B B B A",
                  "B Q Q Q B",
                  "B Q Q Q B",
                  "B Q Q Q B",
                  "A B B B A"
                ],
                [
                  "A A A A A",
                  "A B B B A",
                  "A B E B A",
                  "A B B B A",
                  "A A A A A"
                ]
              ]
            }
            """;

    private final Map<String, List<Block>> symbols = new HashMap<>();
    private final List<List<String>> layers = new ArrayList<>();
    private int originInPatternX = -1;
    private int originInPatternY = -1;
    private int originInPatternZ = -1;

    public ResearchUnitValidator() {
        JsonObject json = JsonParser.parseString(STRUCTURE_JSON).getAsJsonObject();

        // Symbols
        JsonObject symbolsJson = json.getAsJsonObject("symbols");
        for (Map.Entry<String, JsonElement> entry : symbolsJson.entrySet()) {
            List<Block> blocks = new ArrayList<>();
            for (JsonElement el : entry.getValue().getAsJsonArray()) {
                ResourceLocation id = ResourceLocation.tryParse(el.getAsString());
                if (id == null) continue;

                Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                if (block != null) {
                    blocks.add(block);
                }
            }
            symbols.put(entry.getKey(), blocks);
        }

        // Layers + origin (C)
        JsonArray layersJson = json.getAsJsonArray("layers");
        for (int y = 0; y < layersJson.size(); y++) {
            JsonArray layerJson = layersJson.get(y).getAsJsonArray();
            List<String> layer = new ArrayList<>();
            for (int z = 0; z < layerJson.size(); z++) {
                String row = layerJson.get(z).getAsString();
                String[] parts = row.split(" ");
                for (int x = 0; x < parts.length; x++) {
                    if (parts[x].equals("C")) {
                        originInPatternX = x;
                        originInPatternY = y;
                        originInPatternZ = z;
                    }
                }
                layer.add(row);
            }
            layers.add(layer);
        }

        if (originInPatternX == -1 || originInPatternY == -1 || originInPatternZ == -1) {
            throw new IllegalStateException("Pattern does not contain origin symbol 'C'");
        }

        // === Dodatkowe bloki do symbolu Q z configa: MAPA rl -> int (int na razie ignorowany) ===
        extendSymbolFromConfigMap("Q", CrazyConfig.COMMON.ResearchUnitExtraQBlocks.get());
    }

    public Map<String, List<Block>> getSymbols() { return this.symbols; }
    public List<List<String>> getLayers() { return this.layers; }
    public int getOriginX() { return this.originInPatternX; }
    public int getOriginY() { return this.originInPatternY; }
    public int getOriginZ() { return this.originInPatternZ; }

    private void extendSymbolFromConfigMap(String symbol, UnmodifiableConfig extraCfg) {
        if (extraCfg == null) return;

        var map = extraCfg.valueMap();
        if (map == null || map.isEmpty()) return;

        List<Block> allowed = symbols.computeIfAbsent(symbol, k -> new ArrayList<>());

        for (var e : map.entrySet()) {
            String key = e.getKey();
            if (key == null || key.isBlank()) continue;

            ResourceLocation rl = ResourceLocation.tryParse(key);
            if (rl == null) continue;

            BuiltInRegistries.BLOCK.getOptional(rl).ifPresent(b -> {
                if (!allowed.contains(b)) allowed.add(b);
            });
        }
    }

    public boolean matchesStructure(Level level, BlockPos origin, BlockState state, ResearchUnitBE controller) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();

        int height = layers.size();
        int sizeZ = layers.get(0).size();
        int sizeX = layers.get(0).get(0).split(" ").length;

        for (int y = 0; y < height; y++) {
            List<String> layer = layers.get(y);
            for (int z = 0; z < sizeZ; z++) {
                String[] row = layer.get(z).split(" ");
                for (int x = 0; x < sizeX; x++) {
                    String symbol = row[x];
                    if (symbol.equals(".")) continue;

                    int relX = x - originInPatternX;
                    int relZ = z - originInPatternZ;
                    int relY = y - originInPatternY;

                    BlockPos rotatedOffset = rotateOffset(relX, relZ, facing);
                    BlockPos checkPos = origin.offset(rotatedOffset.getX(), relY, rotatedOffset.getZ());

                    BlockState checkState = level.getBlockState(checkPos);
                    Block block = checkState.getBlock();

                    List<Block> allowed = symbols.get(symbol);
                    if (allowed == null || !allowed.contains(block)) {
                        markWalls(level, origin, state, ResearchUnitFrameBlock.FORMED, false, controller);
                        return false;
                    }
                }
            }
        }

        markWalls(level, origin, state, ResearchUnitFrameBlock.FORMED, true, controller);
        return true;
    }

    private BlockPos rotateOffset(int x, int z, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(x, 0, z);
            case SOUTH -> new BlockPos(-x, 0, -z);
            case WEST  -> new BlockPos(z, 0, -x);
            case EAST  -> new BlockPos(-z, 0, x);
            default    -> BlockPos.ZERO;
        };
    }

    public void markWalls(
            Level level,
            BlockPos origin,
            BlockState state,
            BooleanProperty formedProperty,
            boolean setState,
            ResearchUnitBE controller
    ) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        int height = layers.size();
        int sizeZ = layers.get(0).size();
        int sizeX = layers.get(0).get(0).split(" ").length;

        for (int y = 0; y < height; y++) {
            List<String> layer = layers.get(y);
            for (int z = 0; z < sizeZ; z++) {
                String[] row = layer.get(z).split(" ");
                for (int x = 0; x < sizeX; x++) {
                    if (!row[x].equals("A") && !row[x].equals("P")) continue;

                    int relX = x - originInPatternX;
                    int relZ = z - originInPatternZ;
                    int relY = y - originInPatternY;

                    BlockPos offsetXZ = rotateOffset(relX, relZ, facing);
                    BlockPos checkPos = origin.offset(offsetXZ.getX(), relY, offsetXZ.getZ());

                    BlockState bs = level.getBlockState(checkPos);
                    if (bs.hasProperty(formedProperty) && bs.getValue(formedProperty) != setState) {
                        level.setBlock(checkPos, bs.setValue(formedProperty, setState), 3);
                    }

                    var be = level.getBlockEntity(checkPos);
                    if (be instanceof ResearchUnitFrameBE frameBE) {
                        frameBE.setController(setState ? controller : null);
                    }
                }
            }
        }
    }

    public int countBlockInStructure(Level level, BlockPos origin, BlockState state, String blockId) {
        ResourceLocation id = ResourceLocation.tryParse(blockId);
        if (id == null) return 0;

        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block == null) return 0;

        return countBlockInStructure(level, origin, state, block);
    }

    public int countBlockInStructure(Level level, BlockPos origin, BlockState state, Block block) {
        if (block == null) return 0;

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();

        int height = layers.size();
        int sizeZ = layers.get(0).size();
        int sizeX = layers.get(0).get(0).split(" ").length;

        int count = 0;

        for (int y = 0; y < height; y++) {
            List<String> layer = layers.get(y);
            for (int z = 0; z < sizeZ; z++) {
                String[] row = layer.get(z).split(" ");
                for (int x = 0; x < sizeX; x++) {
                    String symbol = row[x];
                    if (symbol.equals(".")) continue;

                    List<Block> allowed = symbols.get(symbol);
                    if (allowed == null || !allowed.contains(block)) continue;

                    int relX = x - originInPatternX;
                    int relZ = z - originInPatternZ;
                    int relY = y - originInPatternY;

                    BlockPos rotatedOffset = rotateOffset(relX, relZ, facing);
                    BlockPos checkPos = origin.offset(rotatedOffset.getX(), relY, rotatedOffset.getZ());

                    BlockState checkState = level.getBlockState(checkPos);
                    if (checkState.getBlock() == block) {
                        count++;
                    }
                }
            }
        }

        return count;
    }
}
