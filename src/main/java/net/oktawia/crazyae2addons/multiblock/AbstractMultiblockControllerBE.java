package net.oktawia.crazyae2addons.multiblock;

import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.grid.AENetworkBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMultiblockControllerBE
        extends AENetworkBlockEntity
        implements IGridTickable, IAsyncAutoSyncBlockEntity, IRPCBlockEntity, IManaged {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(AbstractMultiblockControllerBE.class);

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

    @Override
    public IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        setChanged();
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
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                be.invalidateCaps();
            }
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
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