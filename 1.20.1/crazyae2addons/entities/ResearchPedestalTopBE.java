package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.grid.AENetworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.List;
import java.util.Set;

public class ResearchPedestalTopBE extends AENetworkBlockEntity {

    private ItemStack storedStack = ItemStack.EMPTY;

    public ResearchPedestalTopBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.RESEARCH_PEDESTAL_TOP_BE.get(), pos, blockState);
        getMainNode()
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.RESEARCH_PEDESTAL_TOP.get().asItem())
                )
                .setExposedOnSides(Set.of());
    }

    public boolean isEmpty() {
        return this.storedStack.isEmpty();
    }

    public ItemStack getStoredStack() {
        return this.storedStack;
    }

    public void setStoredStack(ItemStack stack) {
        this.storedStack = stack.copy();
        this.setChanged();
        syncToClient();
    }

    public ItemStack takeStoredStack() {
        ItemStack result = this.storedStack;
        this.storedStack = ItemStack.EMPTY;
        this.setChanged();
        syncToClient();
        return result;
    }

    private void syncToClient() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.storedStack.isEmpty()) {
            tag.put("StoredItem", this.storedStack.save(new CompoundTag()));
        }
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        if (tag.contains("StoredItem")) {
            this.storedStack = ItemStack.of(tag.getCompound("StoredItem"));
        } else {
            this.storedStack = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        drops.add(getStoredStack());
    }
}
