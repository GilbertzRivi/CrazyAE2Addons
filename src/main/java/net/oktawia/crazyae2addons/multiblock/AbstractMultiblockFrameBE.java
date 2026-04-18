package net.oktawia.crazyae2addons.multiblock;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public abstract class AbstractMultiblockFrameBE<C extends BlockEntity> extends AENetworkedBlockEntity
        implements MultiblockCallback, ISyncPersistRPCBlockEntity {

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    protected @Nullable MultiblockState activeState;
    protected @Nullable C activeController;

    @DescSynced
    protected @Nullable BlockPos syncedControllerPos;

    protected AbstractMultiblockFrameBE(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState,
            ItemStack visualRepresentation,
            float idlePowerUsage
    ) {
        super(type, pos, blockState);

        this.getMainNode()
                .setIdlePowerUsage(idlePowerUsage)
                .setVisualRepresentation(visualRepresentation);
    }

    protected abstract Class<C> controllerClass();

    @Override
    public void setState(@Nullable MultiblockState state) {
        this.activeState = state;
        setChanged();
    }

    @Override
    public void setController(@Nullable BlockEntity controller) {
        C newController = controllerClass().isInstance(controller)
                ? controllerClass().cast(controller)
                : null;

        if (this.activeController == newController) {
            return;
        }

        this.activeController = newController;
        this.syncedControllerPos = newController != null ? newController.getBlockPos().immutable() : null;

        onControllerChanged(newController);
        setChanged();
    }

    protected void onControllerChanged(@Nullable C newController) {
    }

    protected @Nullable C getResolvedController() {
        if (this.activeController != null && !this.activeController.isRemoved()) {
            return this.activeController;
        }

        if (this.syncedControllerPos == null) {
            return null;
        }

        Level level = getLevel();
        if (level == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(this.syncedControllerPos);
        if (controllerClass().isInstance(be)) {
            return controllerClass().cast(be);
        }

        return null;
    }

    @Override
    public void unregister(MultiblockState state) {
        state.unregisterCallback(getBlockPos());
    }

    @Override
    public void setRemoved() {
        if (this.activeState != null) {
            unregister(this.activeState);
            this.activeState = null;
        }

        disconnectFromControllerGrid();
        this.activeController = null;
        this.syncedControllerPos = null;
        super.setRemoved();
    }

    public @Nullable C getActiveController() {
        return getResolvedController();
    }

    public @Nullable MultiblockState getActiveState() {
        return this.activeState;
    }

    public void connectToControllerGrid() {
        IGridNode selfNode = this.getMainNode().getNode();
        if (selfNode == null || this.activeController == null) {
            return;
        }

        if (!(this.activeController instanceof AENetworkedBlockEntity controllerAe)) {
            return;
        }

        IGridNode controllerNode = controllerAe.getMainNode().getNode();
        if (controllerNode == null) {
            return;
        }

        boolean alreadyLinked = selfNode.getConnections().stream()
                .anyMatch(connection -> connection.getOtherSide(selfNode) == controllerNode);

        if (!alreadyLinked) {
            GridHelper.createConnection(selfNode, controllerNode);
        }
    }

    public void disconnectFromControllerGrid() {
        IGridNode selfNode = this.getMainNode().getNode();
        if (selfNode == null) {
            return;
        }

        for (IGridConnection connection : new ArrayList<>(selfNode.getConnections())) {
            if (connection.isInWorld()) {
                continue;
            }

            IGridNode otherSide = connection.getOtherSide(selfNode);
            if (otherSide != null && controllerClass().isInstance(otherSide.getOwner())) {
                connection.destroy();
            }
        }
    }
}