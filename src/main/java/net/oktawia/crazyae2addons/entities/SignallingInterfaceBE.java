package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.*;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.util.AECableType;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.helpers.IPriorityHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.logic.Signalling.SignallingInterfaceLogic;
import net.oktawia.crazyae2addons.logic.Signalling.SignallingInterfaceLogicHost;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SignallingInterfaceBE extends AENetworkBlockEntity
        implements IPriorityHost, IUpgradeableObject, IConfigurableObject, SignallingInterfaceLogicHost {

    private final SignallingInterfaceLogic logic = createLogic();
    public boolean redstoneOut = false;

    public SignallingInterfaceBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.SIGNALLING_INTERFACE_BE.get(), pos, blockState);
        this.getMainNode()
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.SIGNALLING_INTERFACE_BLOCK.get().asItem())
                );
    }

    protected SignallingInterfaceLogic createLogic() {
        return new SignallingInterfaceLogic(getMainNode(), this, getItemFromBlockEntity().asItem(), 9);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (getMainNode().hasGridBooted()) {
            this.logic.notifyNeighbors();
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        this.logic.addDrops(drops);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.logic.clearContent();
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.logic.writeToNBT(data);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        this.logic.readFromNBT(data);
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return this.logic.getCableConnectionType(dir);
    }

    @Override
    public SignallingInterfaceLogic getInterfaceLogic() {
        return this.logic;
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.SIGNALLING_INTERFACE_BLOCK.get().asItem().getDefaultInstance();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        LazyOptional<T> result = this.logic.getCapability(capability, facing);
        if (result.isPresent()) {
            return result;
        }
        return super.getCapability(capability, facing);
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(UPGRADES)) {
            return logic.getUpgrades();
        }
        return super.getSubInventory(id);
    }

    @Override
    public IUpgradeInventory getUpgrades(){
        return logic.getUpgrades();
    }
}
