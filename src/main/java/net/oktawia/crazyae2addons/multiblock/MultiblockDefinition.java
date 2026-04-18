package net.oktawia.crazyae2addons.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public final class MultiblockDefinition {
    private final Map<Character, SymbolDef> symbols;
    private final List<PatternEntry> entries;
    private final Map<Direction, List<PatternEntry>> rotatedEntries;
    private final Map<Direction, Map<Character, List<BlockPos>>> rotatedSymbolOffsets;
    private final Map<Direction, Map<BlockPos, PatternEntry>> rotatedEntryByOffset;

    private MultiblockDefinition(
            Map<Character, SymbolDef> symbols,
            List<PatternEntry> entries
    ) {
        this.symbols = Collections.unmodifiableMap(new LinkedHashMap<>(symbols));
        this.entries = List.copyOf(entries);

        this.rotatedEntries = buildRotatedEntries(this.entries);
        this.rotatedSymbolOffsets = buildRotatedSymbolOffsets(this.rotatedEntries);
        this.rotatedEntryByOffset = buildRotatedEntryByOffset(this.rotatedEntries);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<PatternEntry> entries() {
        return this.entries;
    }

    public @Nullable SymbolDef getSymbol(char symbol) {
        return this.symbols.get(symbol);
    }

    public List<PatternEntry> getEntries(Direction structureFacing) {
        return this.rotatedEntries.get(normalizeHorizontalFacing(structureFacing));
    }

    public List<BlockPos> getOffsetsBySymbol(Direction structureFacing, char symbol) {
        Map<Character, List<BlockPos>> bySymbol =
                this.rotatedSymbolOffsets.get(normalizeHorizontalFacing(structureFacing));

        List<BlockPos> offsets = bySymbol.get(symbol);
        return offsets != null ? offsets : Collections.emptyList();
    }

    public Map<BlockPos, PatternEntry> getEntryByOffset(Direction structureFacing) {
        return this.rotatedEntryByOffset.get(normalizeHorizontalFacing(structureFacing));
    }

    public record SymbolDef(List<Block> blocks, TrackingMode tracking) {
        public SymbolDef {
            Objects.requireNonNull(blocks, "blocks");
            Objects.requireNonNull(tracking, "tracking");

            if (blocks.isEmpty()) {
                throw new IllegalArgumentException("SymbolDef blocks cannot be empty");
            }

            blocks = List.copyOf(blocks);
        }
    }

    public record PatternEntry(int relX, int relY, int relZ, char symbol) {
    }

    public enum TrackingMode {
        POLLED,
        CALLBACK
    }

    public static final class Builder {
        private final Map<Character, SymbolDef> symbols = new LinkedHashMap<>();
        private final List<List<String>> layers = new ArrayList<>();

        private int expectedWidth = -1;
        private int expectedDepth = -1;

        private Builder() {
        }

        public Builder symbol(char symbol, TrackingMode tracking, String... blockIds) {
            validateSymbolChar(symbol);

            Objects.requireNonNull(tracking, "tracking");
            Objects.requireNonNull(blockIds, "blockIds");

            if (blockIds.length == 0) {
                throw new IllegalArgumentException("Symbol '" + symbol + "' must have at least one block id");
            }

            LinkedHashSet<Block> resolvedBlocks = new LinkedHashSet<>();
            for (String blockId : blockIds) {
                if (blockId == null || blockId.isBlank()) {
                    throw new IllegalArgumentException("Block id for symbol '" + symbol + "' cannot be null or blank");
                }

                ResourceLocation id;
                try {
                    id = ResourceLocation.parse(blockId);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Invalid block id '" + blockId + "' for symbol '" + symbol + "'",
                            e
                    );
                }

                if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                    throw new IllegalArgumentException(
                            "Unknown block id '" + blockId + "' for symbol '" + symbol + "'"
                    );
                }

                resolvedBlocks.add(BuiltInRegistries.BLOCK.get(id));
            }

            SymbolDef previous = this.symbols.put(symbol, new SymbolDef(List.copyOf(resolvedBlocks), tracking));
            if (previous != null) {
                throw new IllegalStateException("Duplicate symbol definition for '" + symbol + "'");
            }

            return this;
        }

        public Builder layer(String... rows) {
            Objects.requireNonNull(rows, "rows");

            if (rows.length == 0) {
                throw new IllegalArgumentException("Layer must contain at least one row");
            }

            List<String> normalizedRows = new ArrayList<>(rows.length);
            for (int z = 0; z < rows.length; z++) {
                String row = rows[z];
                if (row == null) {
                    throw new IllegalArgumentException("Layer row at z=" + z + " cannot be null");
                }

                String normalized = normalizeRow(row);

                if (normalized.isEmpty()) {
                    throw new IllegalArgumentException("Layer row at z=" + z + " cannot be empty after removing spaces");
                }

                if (this.expectedWidth == -1) {
                    this.expectedWidth = normalized.length();
                } else if (normalized.length() != this.expectedWidth) {
                    throw new IllegalArgumentException(
                            "All rows must have the same width after removing spaces. Expected "
                                    + this.expectedWidth
                                    + ", got "
                                    + normalized.length()
                                    + " at z="
                                    + z
                    );
                }

                normalizedRows.add(normalized);
            }

            if (this.expectedDepth == -1) {
                this.expectedDepth = normalizedRows.size();
            } else if (normalizedRows.size() != this.expectedDepth) {
                throw new IllegalArgumentException(
                        "All layers must have the same row count. Expected "
                                + this.expectedDepth
                                + ", got "
                                + normalizedRows.size()
                );
            }

            this.layers.add(List.copyOf(normalizedRows));
            return this;
        }

        public MultiblockDefinition build() {
            if (this.layers.isEmpty()) {
                throw new IllegalStateException("Cannot build multiblock definition without any layers");
            }

            int controllerX = Integer.MIN_VALUE;
            int controllerY = Integer.MIN_VALUE;
            int controllerZ = Integer.MIN_VALUE;
            int controllerCount = 0;

            for (int y = 0; y < this.layers.size(); y++) {
                List<String> layer = this.layers.get(y);
                for (int z = 0; z < layer.size(); z++) {
                    String row = layer.get(z);
                    for (int x = 0; x < row.length(); x++) {
                        char symbol = row.charAt(x);

                        if (isWildcard(symbol)) {
                            continue;
                        }

                        if (symbol == 'C') {
                            controllerCount++;
                            controllerX = x;
                            controllerY = y;
                            controllerZ = z;
                            continue;
                        }

                        if (!this.symbols.containsKey(symbol)) {
                            throw new IllegalStateException(
                                    "Pattern uses undefined symbol '" + symbol + "' at x=" + x + ", y=" + y + ", z=" + z
                            );
                        }
                    }
                }
            }

            if (controllerCount == 0) {
                throw new IllegalStateException("Pattern does not contain controller symbol 'C'");
            }

            if (controllerCount > 1) {
                throw new IllegalStateException("Pattern must contain exactly one controller symbol 'C'");
            }

            List<PatternEntry> entries = getPatternEntries(controllerX, controllerY, controllerZ);
            return new MultiblockDefinition(this.symbols, entries);
        }

        private @NotNull List<PatternEntry> getPatternEntries(int controllerX, int controllerY, int controllerZ) {
            List<PatternEntry> entries = new ArrayList<>();

            for (int y = 0; y < this.layers.size(); y++) {
                List<String> layer = this.layers.get(y);
                for (int z = 0; z < layer.size(); z++) {
                    String row = layer.get(z);
                    for (int x = 0; x < row.length(); x++) {
                        char symbol = row.charAt(x);

                        if (isWildcard(symbol) || symbol == 'C') {
                            continue;
                        }

                        entries.add(new PatternEntry(
                                x - controllerX,
                                y - controllerY,
                                z - controllerZ,
                                symbol
                        ));
                    }
                }
            }

            return entries;
        }

        private static void validateSymbolChar(char symbol) {
            if (symbol == '.' || Character.isWhitespace(symbol)) {
                throw new IllegalArgumentException(
                        "'.' and whitespace are reserved for pattern formatting/wildcards and cannot be used as real symbols"
                );
            }
        }

        private static boolean isWildcard(char symbol) {
            return symbol == '.';
        }

        private static String normalizeRow(String row) {
            StringBuilder out = new StringBuilder(row.length());

            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (!Character.isWhitespace(c)) {
                    out.append(c);
                }
            }

            return out.toString();
        }
    }

    private static Map<Direction, List<PatternEntry>> buildRotatedEntries(List<PatternEntry> rawEntries) {
        EnumMap<Direction, List<PatternEntry>> result = new EnumMap<>(Direction.class);

        for (Direction facing : List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)) {
            List<PatternEntry> rotated = new ArrayList<>(rawEntries.size());

            for (PatternEntry entry : rawEntries) {
                BlockPos rotatedOffset = rotateOffset(entry.relX(), entry.relZ(), facing);
                rotated.add(new PatternEntry(
                        rotatedOffset.getX(),
                        entry.relY(),
                        rotatedOffset.getZ(),
                        entry.symbol()
                ));
            }

            result.put(facing, List.copyOf(rotated));
        }

        return Collections.unmodifiableMap(result);
    }

    private static Map<Direction, Map<Character, List<BlockPos>>> buildRotatedSymbolOffsets(
            Map<Direction, List<PatternEntry>> rotatedEntries
    ) {
        EnumMap<Direction, Map<Character, List<BlockPos>>> result = new EnumMap<>(Direction.class);

        for (var facingEntry : rotatedEntries.entrySet()) {
            Map<Character, List<BlockPos>> bySymbolMutable = new LinkedHashMap<>();

            for (PatternEntry entry : facingEntry.getValue()) {
                bySymbolMutable
                        .computeIfAbsent(entry.symbol(), ignored -> new ArrayList<>())
                        .add(new BlockPos(entry.relX(), entry.relY(), entry.relZ()));
            }

            Map<Character, List<BlockPos>> bySymbol = new LinkedHashMap<>();
            for (var symbolEntry : bySymbolMutable.entrySet()) {
                bySymbol.put(symbolEntry.getKey(), List.copyOf(symbolEntry.getValue()));
            }

            result.put(facingEntry.getKey(), Collections.unmodifiableMap(bySymbol));
        }

        return Collections.unmodifiableMap(result);
    }

    private static Map<Direction, Map<BlockPos, PatternEntry>> buildRotatedEntryByOffset(
            Map<Direction, List<PatternEntry>> rotatedEntries
    ) {
        EnumMap<Direction, Map<BlockPos, PatternEntry>> result = new EnumMap<>(Direction.class);

        for (var facingEntry : rotatedEntries.entrySet()) {
            Map<BlockPos, PatternEntry> byOffset = new LinkedHashMap<>();

            for (PatternEntry entry : facingEntry.getValue()) {
                BlockPos pos = new BlockPos(entry.relX(), entry.relY(), entry.relZ());

                PatternEntry previous = byOffset.put(pos, entry);
                if (previous != null) {
                    throw new IllegalStateException(
                            "Duplicate rotated offset detected for facing "
                                    + facingEntry.getKey()
                                    + " at "
                                    + pos
                    );
                }
            }

            result.put(facingEntry.getKey(), Collections.unmodifiableMap(byOffset));
        }

        return Collections.unmodifiableMap(result);
    }

    private static Direction normalizeHorizontalFacing(Direction facing) {
        Objects.requireNonNull(facing, "facing");

        if (facing.getAxis().isVertical()) {
            throw new IllegalArgumentException("Expected horizontal facing, got: " + facing);
        }

        return facing;
    }

    private static BlockPos rotateOffset(int x, int z, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(x, 0, z);
            case SOUTH -> new BlockPos(-x, 0, -z);
            case WEST -> new BlockPos(z, 0, -x);
            case EAST -> new BlockPos(-z, 0, x);
            default -> throw new IllegalArgumentException("Unsupported facing: " + facing);
        };
    }
}