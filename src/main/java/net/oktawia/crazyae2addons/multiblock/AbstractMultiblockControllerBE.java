package net.oktawia.crazyae2addons.multiblock;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMultiblockControllerBE
        extends AENetworkedBlockEntity
        implements IGridTickable, ISyncPersistRPCBlockEntity {

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Getter
    protected final MultiblockState multiblockState;

    protected AbstractMultiblockControllerBE(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState,
            ItemStack visualRepresentation,
            float idlePowerUsage
    ) {
        super(type, pos, blockState);

        this.getMainNode()
                .setIdlePowerUsage(idlePowerUsage)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(visualRepresentation);

        this.multiblockState = new MultiblockState(
                getMultiblockDefinition(),
                this,
                this::onFormedInternal,
                this::onDisformedInternal
        );
    }

    protected abstract MultiblockDefinition getMultiblockDefinition();

    protected abstract char frameSymbol();

    protected abstract void setOwnFormedState(boolean formed);

    protected abstract void setMemberFormedState(BlockPos pos, boolean formed);

    protected void afterFormed() {
    }

    protected void afterDisformed() {
    }

    protected void invalidateMemberCapabilities() {
        for (BlockPos pos : this.multiblockState.getBlocksBySymbol(frameSymbol())) {
            invalidateCapabilitiesAt(pos);
        }
    }

    protected final void invalidateOwnCapabilities() {
        invalidateCapabilitiesAt(getBlockPos());
    }

    protected final void invalidateCapabilitiesAt(BlockPos pos) {
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            level.invalidateCapabilities(pos);
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return TickRateModulation.IDLE;
        }

        this.multiblockState.tick(level, getBlockPos(), getBlockState());
        return TickRateModulation.IDLE;
    }

    @Override
    public void setRemoved() {
        this.multiblockState.destroy();
        super.setRemoved();
    }

    private void onFormedInternal() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        setOwnFormedState(true);
        setAllMemberFormedStates(true);
        afterFormed();
        invalidateMemberCapabilities();
        invalidateOwnCapabilities();
        setChanged();
    }

    private void onDisformedInternal() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        setOwnFormedState(false);
        setAllMemberFormedStates(false);
        afterDisformed();
        invalidateMemberCapabilities();
        invalidateOwnCapabilities();
        setChanged();
    }

    private void setAllMemberFormedStates(boolean formed) {
        for (BlockPos pos : this.multiblockState.getBlocksBySymbol(frameSymbol())) {
            setMemberFormedState(pos, formed);
        }
    }
}