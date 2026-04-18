package net.oktawia.crazyae2addons.multiblock;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MultiblockState {
    private static final int FAST_POLL_INTERVAL_TICKS = 10;
    private static final int ATTACHED_CALLBACK_VALIDATE_INTERVAL_TICKS = 200; // 10 sekund

    private final MultiblockDefinition definition;
    private final BlockEntity controller;
    private final Runnable onFormed;
    private final Runnable onDisformed;

    @Getter
    private boolean formed;

    private final Map<BlockPos, MultiblockCallback> registeredCallbacks = new LinkedHashMap<>();
    private final Map<BlockPos, MultiblockCallback> attachedPolledCallbacks = new LinkedHashMap<>();

    private final Map<Character, List<BlockPos>> symbolPositions = new LinkedHashMap<>();
    private final Map<BlockPos, MultiblockDefinition.PatternEntry> entryByWorldPos = new LinkedHashMap<>();

    private int fastPollTimer;
    private int attachedValidateTimer;

    private @Nullable Direction cachedFacing;
    private @Nullable BlockPos cachedControllerPos;

    public MultiblockState(
            MultiblockDefinition definition,
            BlockEntity controller,
            Runnable onFormed,
            Runnable onDisformed
    ) {
        this.definition = definition;
        this.controller = controller;
        this.onFormed = onFormed;
        this.onDisformed = onDisformed;
    }

    public void tick(Level level, BlockPos controllerPos, BlockState controllerState) {
        Direction structureFacing = getStructureFacing(controllerState);

        boolean rebuilt = rebuildWorldCachesIfNeeded(controllerPos, structureFacing);
        if (rebuilt) {
            invalidateRequiredCallbackCache(level);
            invalidatePolledCallbackCache(level);
        }

        boolean runAttachedValidation = rebuilt;
        this.attachedValidateTimer++;
        if (this.attachedValidateTimer >= ATTACHED_CALLBACK_VALIDATE_INTERVAL_TICKS) {
            this.attachedValidateTimer = 0;
            runAttachedValidation = true;
        }

        if (runAttachedValidation) {
            invalidateRequiredCallbackCache(level);
            invalidatePolledCallbackCache(level);
        }

        boolean runFastPoll = rebuilt;
        this.fastPollTimer++;
        if (this.fastPollTimer >= FAST_POLL_INTERVAL_TICKS) {
            this.fastPollTimer = 0;
            runFastPoll = true;
        }

        if (!runFastPoll) {
            return;
        }

        // POLLED entries sprawdzamy co 10 ticków
        syncPolledCallbacks(level);
        boolean polledOk = pollPolledEntries(level);

        // CALLBACK entries, których jeszcze nie mamy, próbujemy dopiąć co 10 ticków
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
        boolean removedRequired = detachAndRemove(this.registeredCallbacks, pos);
        detachAndRemove(this.attachedPolledCallbacks, pos);

        if (removedRequired && this.formed) {
            doDisform();
        }
    }

    public void destroy() {
        for (MultiblockCallback callback : this.registeredCallbacks.values()) {
            detachCallback(callback);
        }
        for (MultiblockCallback callback : this.attachedPolledCallbacks.values()) {
            detachCallback(callback);
        }

        this.registeredCallbacks.clear();
        this.attachedPolledCallbacks.clear();
        this.symbolPositions.clear();
        this.entryByWorldPos.clear();

        this.formed = false;
        this.fastPollTimer = 0;
        this.attachedValidateTimer = 0;
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
                                + controllerPos + ": " + worldPos
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

    private void syncPolledCallbacks(Level level) {
        for (Map.Entry<BlockPos, MultiblockDefinition.PatternEntry> entry : this.entryByWorldPos.entrySet()) {
            BlockPos worldPos = entry.getKey();
            MultiblockDefinition.PatternEntry patternEntry = entry.getValue();
            MultiblockDefinition.SymbolDef symbolDef = requireSymbol(patternEntry.symbol());

            if (symbolDef.tracking() != MultiblockDefinition.TrackingMode.POLLED) {
                continue;
            }

            BlockState blockState = level.getBlockState(worldPos);
            if (!matchesAllowedBlock(blockState.getBlock(), symbolDef)) {
                detachAndRemove(this.attachedPolledCallbacks, worldPos);
                continue;
            }

            BlockEntity be = level.getBlockEntity(worldPos);
            if (!(be instanceof MultiblockCallback callback) || be.isRemoved()) {
                detachAndRemove(this.attachedPolledCallbacks, worldPos);
                continue;
            }

            MultiblockCallback current = this.attachedPolledCallbacks.get(worldPos);
            if (current == callback) {
                callback.setState(this);
                if (this.formed) {
                    callback.setController(this.controller);
                }
                continue;
            }

            if (current != null) {
                detachCallback(current);
            }

            callback.setState(this);
            if (this.formed) {
                callback.setController(this.controller);
            }
            this.attachedPolledCallbacks.put(worldPos.immutable(), callback);
        }
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
            if (!(be instanceof MultiblockCallback callback) || be.isRemoved()) {
                continue;
            }

            callback.setState(this);
            if (this.formed) {
                callback.setController(this.controller);
            }
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

    private void invalidateRequiredCallbackCache(Level level) {
        Iterator<Map.Entry<BlockPos, MultiblockCallback>> iterator = this.registeredCallbacks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, MultiblockCallback> entry = iterator.next();
            BlockPos pos = entry.getKey();
            MultiblockCallback callback = entry.getValue();

            if (isAttachedCallbackStillValid(level, pos, callback, MultiblockDefinition.TrackingMode.CALLBACK)) {
                continue;
            }

            detachCallback(callback);
            iterator.remove();
        }
    }

    private void invalidatePolledCallbackCache(Level level) {
        Iterator<Map.Entry<BlockPos, MultiblockCallback>> iterator = this.attachedPolledCallbacks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, MultiblockCallback> entry = iterator.next();
            BlockPos pos = entry.getKey();
            MultiblockCallback callback = entry.getValue();

            if (isAttachedCallbackStillValid(level, pos, callback, MultiblockDefinition.TrackingMode.POLLED)) {
                continue;
            }

            detachCallback(callback);
            iterator.remove();
        }
    }

    private boolean isAttachedCallbackStillValid(
            Level level,
            BlockPos pos,
            MultiblockCallback callback,
            MultiblockDefinition.TrackingMode expectedTracking
    ) {
        MultiblockDefinition.PatternEntry patternEntry = this.entryByWorldPos.get(pos);
        if (patternEntry == null) {
            return false;
        }

        MultiblockDefinition.SymbolDef symbolDef = requireSymbol(patternEntry.symbol());
        if (symbolDef.tracking() != expectedTracking) {
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
        attachControllerToAllCallbacks();
        this.onFormed.run();
    }

    private void doDisform() {
        this.formed = false;
        detachControllerFromAllCallbacks();
        this.onDisformed.run();
    }

    private void attachControllerToAllCallbacks() {
        for (MultiblockCallback callback : this.registeredCallbacks.values()) {
            callback.setController(this.controller);
        }

        for (MultiblockCallback callback : this.attachedPolledCallbacks.values()) {
            callback.setController(this.controller);
        }
    }

    private void detachControllerFromAllCallbacks() {
        for (MultiblockCallback callback : this.registeredCallbacks.values()) {
            callback.setController(null);
        }

        for (MultiblockCallback callback : this.attachedPolledCallbacks.values()) {
            callback.setController(null);
        }
    }

    private static void detachCallback(MultiblockCallback callback) {
        callback.setController(null);
        callback.setState(null);
    }

    private static boolean detachAndRemove(Map<BlockPos, MultiblockCallback> map, BlockPos pos) {
        MultiblockCallback callback = map.remove(pos);
        if (callback == null) {
            return false;
        }

        detachCallback(callback);
        return true;
    }
}