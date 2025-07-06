package net.oktawia.crazyae2addons.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.oktawia.crazyae2addons.blocks.PatternManagementUnitFrameBlock;
import net.oktawia.crazyae2addons.blocks.PatternManagementUnitWallBlock;
import net.oktawia.crazyae2addons.blocks.SpawnerExtractorWallBlock;
import net.oktawia.crazyae2addons.entities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatternManagementUnitValidator {

    private static final String STRUCTURE_JSON = """
            {
              "symbols": {
                "A": [
                  "crazyae2addons:pattern_management_unit_frame"
                ],
                "B": [
                  "crazyae2addons:pattern_management_unit_wall"
                ],
                "C": [
                  "crazyae2addons:pattern_management_unit_controller"
                ],
                "E": [
                  "crazyae2addons:pattern_management_unit"
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
                  "B E E E B",
                  "B E E E B",
                  "B E E E B",
                  "A B B B A"
                ],
                [
                  "A B B B A",
                  "B E E E B",
                  "B E E E B",
                  "B E E E B",
                  "A B B B A"
                ],
                [
                  "A B B B A",
                  "B E E E B",
                  "B E E E B",
                  "B E E E B",
                  "A B B B A"
                ],
                [
                  "A A A A A",
                  "A B B B A",
                  "A B B B A",
                  "A B B B A",
                  "A A A A A"
                ]
              ]
            }
    """;

    public Map<String, List<Block>> getSymbols() {
        return this.symbols;
    }

    public List<List<String>> getLayers() {
        return this.layers;
    }

    public int getOriginX() {
        return this.originInPatternX;
    }

    public int getOriginY() {
        return this.originInPatternY;
    }

    public int getOriginZ() {
        return this.originInPatternZ;
    }

    private final Map<String, List<Block>> symbols = new HashMap<>();
    private final List<List<String>> layers = new ArrayList<>();
    private int originInPatternX = -1;
    private int originInPatternY = -1;
    private int originInPatternZ = -1;

    public PatternManagementUnitValidator() {
        JsonObject json = JsonParser.parseString(STRUCTURE_JSON).getAsJsonObject();

        JsonObject symbolsJson = json.getAsJsonObject("symbols");
        for (Map.Entry<String, JsonElement> entry : symbolsJson.entrySet()) {
            List<Block> blocks = new ArrayList<>();
            for (JsonElement el : entry.getValue().getAsJsonArray()) {
                ResourceLocation id = new ResourceLocation(el.getAsString());
                Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                if (block != null) {
                    blocks.add(block);
                }
            }
            symbols.put(entry.getKey(), blocks);
        }

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
    }

    public boolean matchesStructure(Level level, BlockPos origin, BlockState state, PatternManagementUnitControllerBE controller) {
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

                    BlockPos offset = rotateOffset(relX, relZ, facing);
                    BlockPos checkPos = origin.offset(offset.getX(), relY, offset.getZ());

                    BlockState checkState = level.getBlockState(checkPos);
                    Block block = checkState.getBlock();
                    List<Block> allowed = symbols.get(symbol);

                    if (allowed == null || !allowed.contains(block)) {
                        LogUtils.getLogger().info("mismatch {} {} {}", block, symbol, checkPos);
                        markWalls(level, origin, state, SpawnerExtractorWallBlock.FORMED, false, controller);
                        return false;
                    }
                }
            }
        }

        markWalls(level, origin, state, SpawnerExtractorWallBlock.FORMED, true, controller);
        return true;
    }

    private BlockPos rotateOffset(int x, int z, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(x, 0, z);
            case SOUTH -> new BlockPos(-x, 0, -z);
            case WEST  -> new BlockPos(z, 0, -x);
            case EAST  -> new BlockPos(-z, 0, x);
            default -> BlockPos.ZERO;
        };
    }

    public void markWalls(
            Level level,
            BlockPos origin,
            BlockState state,
            BooleanProperty formedProperty,
            boolean setState,
            PatternManagementUnitControllerBE controller
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
                    if (!row[x].equals("A") && !row[x].equals("B")) continue;

                    int relX = x - originInPatternX;
                    int relZ = z - originInPatternZ;
                    int relY = y - originInPatternY;

                    BlockPos offset = rotateOffset(relX, relZ, facing);
                    BlockPos checkPos = origin.offset(offset.getX(), relY, offset.getZ());

                    BlockState bs = level.getBlockState(checkPos);
                    if (bs.hasProperty(formedProperty) && bs.getValue(formedProperty) != setState) {
                        level.setBlock(checkPos, bs.setValue(formedProperty, setState), 3);
                    }

                    BlockEntity be = level.getBlockEntity(checkPos);
                    if (be != null) {
                        if (be instanceof PatternManagementUnitWallBE wallBE) {
                            wallBE.controller = setState ? controller : null;
                        } else if (be instanceof PatternManagementUnitFrameBE frameBE) {
                            frameBE.controller = setState ? controller : null;
                        }
                    }
                }
            }
        }
    }
}