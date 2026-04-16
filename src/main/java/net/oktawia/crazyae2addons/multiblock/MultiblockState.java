package net.oktawia.crazyae2addons.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class MultiblockState {
    private static final int POLL_INTERVAL_TICKS = 10;
    private static final int CALLBACK_INVALIDATE_INTERVAL_TICKS = 100;
    private final MultiblockDefinition definition;
    private final BlockEntity controller;
    private final Runnable onFormed;
    private final Runnable onDisformed;
    @Getter
    private boolean formed;
    private final Map<BlockPos, MultiblockCallback> registeredCallbacks = new LinkedHashMap<>();
    private final Map<Character, List<BlockPos>> symbolPositions = new LinkedHashMap<>();
    private final Map<BlockPos, MultiblockDefinition.PatternEntry> entryByWorldPos = new LinkedHashMap<>();

    private int pollTimer;
    private int callbackInvalidateTimer;

    private @Nullable Direction cachedFacing;
    private @Nullable BlockPos cachedControllerPos;

    public MultiblockState(MultiblockDefinition definition, BlockEntity controller, Runnable onFormed, Runnable onDisformed) {
        this.definition = definition;
        this.controller = controller;
        this.onFormed = onFormed;
        this.onDisformed = onDisformed;
    }

    public void tick(Level level, BlockPos controllerPos, BlockState controllerState) {
        Direction structureFacing = getStructureFacing(controllerState);

        boolean rebuilt = rebuildWorldCachesIfNeeded(controllerPos, structureFacing);
        if (rebuilt) {
            invalidateCallbackCache(level);
            this.callbackInvalidateTimer = 0;
        }

        this.callbackInvalidateTimer++;
        if (this.callbackInvalidateTimer >= CALLBACK_INVALIDATE_INTERVAL_TICKS) {
            invalidateCallbackCache(level);
            this.callbackInvalidateTimer = 0;
        }

        this.pollTimer++;
        if (this.pollTimer < POLL_INTERVAL_TICKS) {
            return;
        }
        this.pollTimer = 0;

        boolean polledOk = pollPolledEntries(level);
        registerMissingCallbacks(level);
        boolean callbacksOk = areAllCallbackEntriesRegistered();

        boolean everythingOk = polledOk && callbacksOk;

        if (!this.formed && everythingOk) {
            doForm();
        } else if (this.formed && !everythingOk) {
            doDisform();
        }
    }

    public void unregisterCallback(BlockPos pos) {
        MultiblockCallback callback = this.registeredCallbacks.remove(pos);
        if (callback != null) {
            detachCallback(callback);
        }

        if (this.formed) {
            doDisform();
        }
    }

    public void destroy() {
        for (MultiblockCallback callback : this.registeredCallbacks.values()) {
            detachCallback(callback);
        }

        this.registeredCallbacks.clear();
        this.symbolPositions.clear();
        this.entryByWorldPos.clear();

        this.formed = false;
        this.pollTimer = 0;
        this.callbackInvalidateTimer = 0;
        this.cachedFacing = null;
        this.cachedControllerPos = null;
    }

    public List<BlockPos> getBlocksBySymbol(char symbol) {
        List<BlockPos> positions = this.symbolPositions.get(symbol);
        if (positions == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(positions);
    }

    private boolean rebuildWorldCachesIfNeeded(BlockPos controllerPos, Direction structureFacing) {
        if (!needsCacheRebuild(controllerPos, structureFacing)) {
            return false;
        }

        this.symbolPositions.clear();
        this.entryByWorldPos.clear();

        for (MultiblockDefinition.PatternEntry rotatedEntry : this.definition.getEntries(structureFacing)) {
            BlockPos worldPos = controllerPos.offset(
                rotatedEntry.relX(),
                rotatedEntry.relY(),
                rotatedEntry.relZ()
            );

            MultiblockDefinition.PatternEntry previous = this.entryByWorldPos.put(worldPos, rotatedEntry);
            if (previous != null) {
                throw new IllegalStateException(
                    "Duplicate world position in multiblock cache for controller at "
                        + controllerPos
                        + ": "
                        + worldPos
                );
            }

            this.symbolPositions
                .computeIfAbsent(rotatedEntry.symbol(), ignored -> new ArrayList<>())
                .add(worldPos);
        }

        this.cachedControllerPos = controllerPos.immutable();
        this.cachedFacing = structureFacing;
        return true;
    }

    private boolean needsCacheRebuild(BlockPos controllerPos, Direction structureFacing) {
        if (this.symbolPositions.isEmpty() || this.entryByWorldPos.isEmpty()) {
            return true;
        }

        if (!controllerPos.equals(this.cachedControllerPos)) {
            return true;
        }

        return this.cachedFacing != structureFacing;
    }

    private boolean pollPolledEntries(Level level) {
        for (Map.Entry<BlockPos, MultiblockDefinition.PatternEntry> entry : this.entryByWorldPos.entrySet()) {
            MultiblockDefinition.PatternEntry patternEntry = entry.getValue();
            MultiblockDefinition.SymbolDef symbolDef = requireSymbol(patternEntry.symbol());

            if (symbolDef.tracking() != MultiblockDefinition.TrackingMode.POLLED) {
                continue;
            }

            BlockPos worldPos = entry.getKey();
            Block block = level.getBlockState(worldPos).getBlock();

            if (!matchesAllowedBlock(block, symbolDef)) {
                return false;
            }
        }

        return true;
    }

    private void registerMissingCallbacks(Level level) {
        for (Map.Entry<BlockPos, MultiblockDefinition.PatternEntry> entry : this.entryByWorldPos.entrySet()) {
            BlockPos worldPos = entry.getKey();
            if (this.registeredCallbacks.containsKey(worldPos)) {
                continue;
            }

            MultiblockDefinition.PatternEntry patternEntry = entry.getValue();
            MultiblockDefinition.SymbolDef symbolDef = requireSymbol(patternEntry.symbol());

            if (symbolDef.tracking() != MultiblockDefinition.TrackingMode.CALLBACK) {
                continue;
            }

            BlockState blockState = level.getBlockState(worldPos);
            if (!matchesAllowedBlock(blockState.getBlock(), symbolDef)) {
                continue;
            }

            BlockEntity be = level.getBlockEntity(worldPos);
            if (!(be instanceof MultiblockCallback callback)) {
                continue;
            }

            callback.setController(this.controller);
            callback.setState(this);
            this.registeredCallbacks.put(worldPos.immutable(), callback);
        }
    }

    private boolean areAllCallbackEntriesRegistered() {
        for (Map.Entry<BlockPos, MultiblockDefinition.PatternEntry> entry : this.entryByWorldPos.entrySet()) {
            MultiblockDefinition.PatternEntry patternEntry = entry.getValue();
            MultiblockDefinition.SymbolDef symbolDef = requireSymbol(patternEntry.symbol());

            if (symbolDef.tracking() != MultiblockDefinition.TrackingMode.CALLBACK) {
                continue;
            }

            if (!this.registeredCallbacks.containsKey(entry.getKey())) {
                return false;
            }
        }

        return true;
    }

    private void invalidateCallbackCache(Level level) {
        Iterator<Map.Entry<BlockPos, MultiblockCallback>> iterator = this.registeredCallbacks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, MultiblockCallback> entry = iterator.next();
            BlockPos pos = entry.getKey();
            MultiblockCallback callback = entry.getValue();

            if (isCallbackStillValid(level, pos, callback)) {
                continue;
            }

            detachCallback(callback);
            iterator.remove();
        }
    }

    private boolean isCallbackStillValid(Level level, BlockPos pos, MultiblockCallback callback) {
        MultiblockDefinition.PatternEntry patternEntry = this.entryByWorldPos.get(pos);
        if (patternEntry == null) {
            return false;
        }

        MultiblockDefinition.SymbolDef symbolDef = requireSymbol(patternEntry.symbol());
        if (symbolDef.tracking() != MultiblockDefinition.TrackingMode.CALLBACK) {
            return false;
        }

        BlockState blockState = level.getBlockState(pos);
        if (!matchesAllowedBlock(blockState.getBlock(), symbolDef)) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MultiblockCallback currentCallback)) {
            return false;
        }

        if (be.isRemoved()) {
            return false;
        }

        return currentCallback == callback;
    }

    private MultiblockDefinition.SymbolDef requireSymbol(char symbol) {
        MultiblockDefinition.SymbolDef symbolDef = this.definition.getSymbol(symbol);
        if (symbolDef == null) {
            throw new IllegalStateException("Missing symbol definition for '" + symbol + "'");
        }

        return symbolDef;
    }

    private static boolean matchesAllowedBlock(Block block, MultiblockDefinition.SymbolDef symbolDef) {
        return symbolDef.blocks().contains(block);
    }

    private static Direction getStructureFacing(BlockState controllerState) {
        if (!controllerState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            throw new IllegalStateException(
                "Controller state must have HORIZONTAL_FACING: " + controllerState
            );
        }

        return controllerState.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    private void doForm() {
        this.formed = true;
        this.onFormed.run();
    }

    private void doDisform() {
        this.formed = false;
        this.onDisformed.run();
    }

    private void detachCallback(MultiblockCallback callback) {
        callback.setController(null);
        callback.setState(null);
    }
}